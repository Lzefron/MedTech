package com.medtrack.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medtrack.app.domain.ocr.VisionResponseParser
import com.medtrack.app.domain.ocr.VisionSettings
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisionClientTest {
    @Test fun responseKeepsUnknownRowsAndRejectsInvalidDateWithoutInventingRanges() {
        val result = VisionResponseParser.parse(JSONObject("""{"rawText":"Example report", "source":"Gemini test", "date":"2024-02-30", "laboratory":"Example lab", "title":"Example panel", "rows":[{"name":"Unknown marker","value":"unclear","unit":"mg/L","reference":"Adults: 1 to 4; children: 2 to 5","kind":"number","warning":"Check handwriting"}]}"""))
        assertNull(result.detectedDate)
        assertEquals("Unknown marker", result.extractedRecords.single().canonicalName)
        assertNull(result.extractedRecords.single().numericValue)
        assertEquals("", result.extractedRecords.single().refRangeText)
        assertTrue(result.extractedRecords.single().warningNote!!.contains("Adults:"))
    }
    @Test fun gatewayTokenIsEncryptedAndInsecureEndpointsAreRejected() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = VisionSettings(context)
        val token = "test-token-".repeat(5)
        try {
            assertTrue(runCatching { settings.save("http://example.com/v1/extract", token) }.isFailure)
            settings.save("https://example.com/v1/extract", token)
            assertEquals(token, settings.load()!!.token)
            assertFalse(context.getSharedPreferences("vision-connection", Context.MODE_PRIVATE).all.values.any { it.toString().contains(token) })
        } finally { settings.clear() }
        assertNull(settings.load())
    }
}
