package com.medtrack.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.domain.validation.HealthInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileLauncherScreen(viewModel: ProfileViewModel, onProfileSelected: (Long) -> Unit) {
    val profiles by viewModel.allProfiles.collectAsState()
    val error by viewModel.error.collectAsState()
    val saving by viewModel.saving.collectAsState()
    var creating by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProfileEntity?>(null) }
    var deleting by remember { mutableStateOf<ProfileEntity?>(null) }
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary) {
                        Icon(Icons.Default.HealthAndSafety, null, Modifier.padding(12.dp).size(28.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("MedTrack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)),
                        RoundedCornerShape(28.dp)
                    ).padding(24.dp)
                ) {
                    Text("A LITTLE CARE, EVERY DAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (profiles.isNullOrEmpty()) "Your family's health,\nin one calm place." else "Who are we\ncaring for today?",
                        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (profiles.isNullOrEmpty()) "Start with a name. Keep reports, medicines and the details that matter together."
                        else "Choose a profile to open their personal health records.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhonelinkLock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("On this device · No account needed", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
            if (profiles == null) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            } else if (profiles!!.isEmpty()) {
                item {
                    Button(
                        onClick = { creating = true; viewModel.error.value = null },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(10.dp)); Text("Create your first profile") }
                }
                item {
                    Text("One profile for each person", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Keep family members' records separate. You can add health details later, when you have them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(profiles!!, key = { it.id }) { profile ->
                    Card(onClick = { onProfileSelected(profile.id) }, shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(profile.name.take(1).uppercase(), Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text(profile.relationship.ifBlank { "Family member" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Open health records", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            var menu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Manage " + profile.name) }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Edit profile") }, onClick = { menu = false; editing = profile; viewModel.error.value = null })
                                    DropdownMenuItem(text = { Text("Delete profile") }, onClick = { menu = false; deleting = profile })
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { creating = true; viewModel.error.value = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Add another person")
                    }
                }
            }
            item { Text("Records stay on this phone. Uninstalling the app or clearing its storage removes them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (creating || editing != null) {
        ProfileEditor(
            initial = editing, saving = saving, error = error,
            onDismiss = { if (!saving) { creating = false; editing = null } },
            onSave = { profile ->
                val first = profiles?.isEmpty() == true
                viewModel.saveProfile(profile.copy(isDefault = profile.isDefault || first)) { id ->
                    creating = false; editing = null
                    if (first) onProfileSelected(id)
                }
            }
        )
    }
    deleting?.let { profile ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete " + profile.name + "?") },
            text = { Text("This permanently removes their reports, saved photos and medicines from this device.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteProfile(profile); deleting = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep profile") } })
    }
}

@Composable
private fun ProfileEditor(initial: ProfileEntity?, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (ProfileEntity) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var relationship by rememberSaveable { mutableStateOf(initial?.relationship ?: "") }
    var dob by rememberSaveable { mutableStateOf(initial?.dob ?: "") }
    var gender by rememberSaveable { mutableStateOf(initial?.gender ?: "Not specified") }
    var blood by rememberSaveable { mutableStateOf(initial?.bloodGroup ?: "Unknown") }
    var phone by rememberSaveable { mutableStateOf(initial?.emergencyPhone ?: "") }
    var allergies by rememberSaveable { mutableStateOf(initial?.allergies?.takeUnless { it == "Not recorded" } ?: "") }
    var conditions by rememberSaveable { mutableStateOf(initial?.chronicConditions?.takeUnless { it == "Not recorded" } ?: "") }
    var more by rememberSaveable { mutableStateOf(initial != null) }
    val dateError = HealthInput.dateError(dob, optional = true)
    val phoneError = HealthInput.phoneError(phone)
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Let's get to know you" else "Edit profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Only a name is required. Unknown health details stay unknown.")
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(relationship, { relationship = it.take(40) }, label = { Text("Relationship (optional)") }, placeholder = { Text("Self, mother, child…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { more = !more }) { Text(if (more) "Hide optional health details" else "Add optional health details") }
                if (more) {
                    OutlinedTextField(dob, { dob = it.take(10) }, label = { Text("Birth date · YYYY-MM-DD") }, isError = dateError != null,
                        supportingText = { Text(dateError ?: "Optional") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    ChoiceField("Blood group", blood, listOf("Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")) { blood = it }
                    ChoiceField("Sex (optional)", gender, listOf("Not specified", "Female", "Male", "Intersex")) { gender = it }
                    OutlinedTextField(phone, { phone = it.take(30) }, label = { Text("Emergency phone (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null, supportingText = { Text(phoneError ?: "Include a country code if needed") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(allergies, { allergies = it.take(500) }, label = { Text("Allergies (optional)") }, supportingText = { Text("Leave blank if unknown. Enter 'None known' only if confirmed.") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(conditions, { conditions = it.take(500) }, label = { Text("Conditions (optional)") }, modifier = Modifier.fillMaxWidth())
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && dateError == null && phoneError == null && !saving, onClick = {
                onSave((initial ?: ProfileEntity(name = "")).copy(name = name.trim(), relationship = relationship.trim(), dob = dob.trim(), gender = gender,
                    bloodGroup = blood, emergencyPhone = phone.trim(), allergies = allergies.trim().ifBlank { "Not recorded" }, chronicConditions = conditions.trim().ifBlank { "Not recorded" }))
            }) { Text(if (saving) "Saving…" else if (initial == null) "Create profile" else "Save changes") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ChoiceField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label + ": " + value); Icon(Icons.Default.ArrowDropDown, null) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

