package com.medtrack.app.domain.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.medtrack.app.data.model.MedicalDictionary
import com.medtrack.app.domain.validation.HealthInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

data class VisionConnection(val endpoint: String, val token: String)

/** Provider API keys belong on the gateway. Only its revocable per-device token lives here. */
class VisionSettings(context: Context) {
    private val prefs = context.getSharedPreferences("vision-connection", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("medtrack-vision", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("medtrack-vision", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun load(): VisionConnection? = runCatching {
        val encoded = prefs.getString("secret", null) ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
        VisionConnection(prefs.getString("endpoint", "")!!, cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
    }.getOrNull()
    fun save(endpoint: String, token: String) {
        val url = URL(endpoint)
        require(url.protocol == "https" && url.host.isNotBlank() && url.userInfo == null && url.query == null && url.ref == null && url.path == "/v1/extract") { "Use an HTTPS gateway URL ending in /v1/extract." }
        require(token.length in 32..512 && token.none { it.isWhitespace() }) { "Enter the gateway access token (32–512 characters), not a Gemini API key." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        check(prefs.edit().putString("endpoint", endpoint).putString("secret", Base64.encodeToString(cipher.iv + cipher.doFinal(token.toByteArray()), Base64.NO_WRAP)).commit()) { "Could not save the connection." }
    }
    fun clear() { prefs.edit().clear().apply() }
}

object VisionResponseParser {
    fun parse(json: JSONObject): OcrExtractionResult {
        fun field(obj: JSONObject, name: String, max: Int): String {
            val value = obj.get(name)
            require(value is String && value.length <= max) { "Invalid vision response." }
            return value
        }
        val raw = field(json, "rawText", 60000)
        val source = field(json, "source", 120)
        val date = field(json, "date", 10).takeIf { HealthInput.dateError(it) == null }
        val lab = field(json, "laboratory", 120)
        val title = field(json, "title", 120)
        val array = json.getJSONArray("rows")
        require(array.length() <= 150)
        val rows = (0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            val name = field(row, "name", 100).trim().also { require(it.isNotBlank()) }
            val value = field(row, "value", 160)
            val unit = field(row, "unit", 40)
            val reference = field(row, "reference", 80)
            val kind = field(row, "kind", 10).also { require(it in listOf("number", "written")) }
            val warning = field(row, "warning", 500)
            val canonical = MedicalDictionary.tests.firstOrNull { it.name.equals(name, true) || it.aliases.any { alias -> alias.equals(name, true) } }?.name ?: name
            val numeric = kind == "number"
            val number = if (numeric) HealthInput.number(value) else null
            val range = HealthInput.range(reference)
            ParsedLabRow(name, canonical, value, number, unit, range?.low, range?.high, if (range == null) "" else reference,
                number != null && range?.outside(number) == true, 0.4f,
                "$source — compare every field with the photo. $warning" + if (range == null) " Printed reference needs review: $reference" else "", numeric)
        }
        return OcrExtractionResult(raw, date, lab, rows, if (date == null) "AI did not identify an unambiguous valid test date. Enter it from the report." else "AI filled this date. Verify it is the test/collection date, not the birth date.", source, title)
    }
}

class VisionGateway(private val connection: VisionConnection) {
    suspend fun analyze(context: Context, uri: Uri): OcrExtractionResult = withContext(Dispatchers.IO) {
        val bitmap = DocumentImages.decode(context, uri, 2400)
        val output = ByteArrayOutputStream()
        try { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output) } finally { bitmap.recycle() }
        require(output.size() <= 4 * 1024 * 1024) { "Photo is too large for AI reading. Use a smaller photo." }
        val request = JSONObject().put("consent", true).put("mimeType", "image/jpeg").put("image", Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)).toString().toByteArray()
        val http = URL(connection.endpoint).openConnection() as HttpsURLConnection
        try {
            http.requestMethod = "POST"; http.instanceFollowRedirects = false
            http.connectTimeout = 15000; http.readTimeout = 95000; http.doOutput = true
            http.setRequestProperty("Authorization", "Bearer ${connection.token}")
            http.setRequestProperty("Content-Type", "application/json")
            http.setFixedLengthStreamingMode(request.size)
            http.outputStream.use { it.write(request) }
            when (http.responseCode) {
                200 -> Unit
                401, 403 -> error("AI connection access was rejected. Check the gateway access token.")
                429, 503 -> error("AI is busy or unavailable. Your local results are unchanged; try later or edit manually.")
                else -> error("AI reading failed. Your local results are unchanged; try another photo or edit manually.")
            }
            val bytes = http.inputStream.use { input ->
                val buffer = ByteArrayOutputStream(); val chunk = ByteArray(8192)
                while (true) { val count = input.read(chunk); if (count < 0) break; require(buffer.size() + count <= 250000) { "AI response is too large." }; buffer.write(chunk, 0, count) }
                buffer.toByteArray()
            }
            VisionResponseParser.parse(JSONObject(bytes.toString(Charsets.UTF_8)))
        } finally { http.disconnect() }
    }
}
