package com.medtrack.app.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    viewModel: EmergencyViewModel,
    profileId: Long
) {
    val context = LocalContext.current
    LaunchedEffect(profileId) {
        viewModel.setProfileId(profileId)
    }

    val summary by viewModel.emergencySummary.collectAsState()
    val qrBitmap by viewModel.qrBitmap.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emergency Clinical Access", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportAndSharePdf(context) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        val profile = summary?.profile
        if (profile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Emergency Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    profile.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Relationship: ${profile.relationship} • Gender: ${profile.gender}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }

                            // Large Blood Group Badge
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    profile.bloodGroup,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Emergency Dial Button
                        if (profile.emergencyPhone.isNotBlank()) {
                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.emergencyPhone}"))
                                    context.startActivity(dialIntent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call Emergency Contact (${profile.emergencyPhone})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Critical Medical Alerts Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Allergies & Critical Conditions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Known Allergies:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(profile.allergies, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Chronic Conditions:", fontWeight = FontWeight.Bold)
                        Text(profile.chronicConditions, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Active Medication List
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Active Prescriptions (${summary?.activeMedicines?.size ?: 0})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (summary?.activeMedicines.isNullOrEmpty()) {
                            Text("No active medications recorded.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            summary!!.activeMedicines.forEach { med ->
                                Text("• ${med.name} — ${med.dosage} (${med.frequency}, ${med.mealTiming})", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Offline First-Responder QR Code
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Emergency Responder QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Scan with any camera to view instant medical profile offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap!!,
                                    contentDescription = "Emergency QR",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            CircularProgressIndicator()
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { viewModel.exportAndSharePdf(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Clinical PDF Summary (for Doctor)")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
