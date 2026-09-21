package com.medtrack.app.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.medtrack.app.domain.ocr.DocumentImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DocumentPreview(uri: Uri) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) { runCatching { DocumentImages.decode(context, uri) }.getOrNull() }
    }
    var expanded by remember { mutableStateOf(false) }
    val image = bitmap
    if (image != null) {
        Image(image.asImageBitmap(), "Original report. Tap to enlarge.", Modifier.fillMaxWidth().height(200.dp).clickable { expanded = true })
        TextButton(onClick = { expanded = true }) { Text("Enlarge original photo") }
        if (expanded) Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.safeDrawingPadding().padding(12.dp)) {
                    TextButton(onClick = { expanded = false }) { Text("Back to results") }
                    var zoom by remember { mutableFloatStateOf(1f) }
                    Text("Zoom and scroll to compare each result")
                    Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 1f..4f)
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val width = maxWidth
                        Box(Modifier.horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
                            Image(image.asImageBitmap(), "Original report", Modifier.width(width * zoom).height(width * zoom * image.height / image.width))
                        }
                    }
                }
            }
        }
    } else Text("Photo preview unavailable. Reselect the image if needed.", style = MaterialTheme.typography.bodySmall)
}
