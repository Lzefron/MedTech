package com.medtrack.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medtrack.app.data.local.entity.ProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val profiles by viewModel.allProfiles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Health Profiles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Family Member")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Switch or add records for parents, children, or spouse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(profiles) { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (profile.isDefault) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                Text("DEFAULT", color = Color.White, modifier = Modifier.padding(2.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        "${profile.relationship} • Blood: ${profile.bloodGroup} • ${profile.gender}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!profile.isDefault) {
                                IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Emergency Phone: ${profile.emergencyPhone.ifBlank { "Not specified" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Allergies: ${profile.allergies}", style = MaterialTheme.typography.bodySmall)
                        Text("Chronic Conditions: ${profile.chronicConditions}", style = MaterialTheme.typography.bodySmall)

                        if (!profile.isDefault) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.setDefaultProfile(profile.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Set as Default Profile")
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showAddDialog) {
            AddProfileDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { newProfile ->
                    viewModel.createProfile(newProfile)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onAdd: (ProfileEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Parent") }
    var dob by remember { mutableStateOf("1965-01-01") }
    var gender by remember { mutableStateOf("Male") }
    var bloodGroup by remember { mutableStateOf("B+") }
    var phone by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("None") }
    var chronicConditions by remember { mutableStateOf("None") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Family Member Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = relationship, onValueChange = { relationship = it }, label = { Text("Relationship (e.g. Father, Mother, Child)") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bloodGroup, onValueChange = { bloodGroup = it }, label = { Text("Blood Group") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Emergency Contact Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("Known Allergies") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = chronicConditions, onValueChange = { chronicConditions = it }, label = { Text("Chronic Conditions") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            ProfileEntity(
                                name = name.trim(),
                                relationship = relationship.trim(),
                                dob = dob.trim(),
                                gender = gender.trim(),
                                bloodGroup = bloodGroup.trim(),
                                emergencyPhone = phone.trim(),
                                allergies = allergies.trim(),
                                chronicConditions = chronicConditions.trim(),
                                isDefault = false
                            )
                        )
                    }
                }
            ) {
                Text("Create Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
