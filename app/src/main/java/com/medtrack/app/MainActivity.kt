package com.medtrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import com.medtrack.app.ui.navigation.MedTrackNavGraph
import com.medtrack.app.ui.theme.MedTrackTheme

class MainActivity : ComponentActivity() {
    private val sessionVersion = mutableIntStateOf(0)
    private var stopped = false
    private var externalReturn = false
    fun allowExternalReturn() { externalReturn = true }
    override fun onStart() {
        super.onStart()
        if (stopped && !externalReturn) sessionVersion.intValue++
        stopped = false
    }
    override fun onResume() { super.onResume(); externalReturn = false }
    override fun onStop() { if (!isChangingConfigurations) stopped = true; super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MedTrackApplication
        setContent {
            MedTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MedTrackNavGraph(app.profileRepository, app.labReportRepository, app.medicineRepository, sessionVersion.intValue)
                }
            }
        }
    }
}
