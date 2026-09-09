package com.medtrack.app.ui.screens.medicines

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.model.DrugDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScreen(
    viewModel: MedicineViewModel,
    profileId: Long
) {
    val context = LocalContext.current
    LaunchedEffect(profileId) {
        viewModel.setProfileId(profileId)
    }

    val medicines by viewModel.medicines.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Medicine Adherence Vault", fontWeight = FontWeight.Bold) }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Prescriptions & Alarms") },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Drug Reference Guide") },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Prescription")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab == 0) {
                MedicineListTab(
                    medicines = medicines,
                    onToggle = { med -> viewModel.toggleMedicine(context, med) },
                    onDelete = { med -> viewModel.deleteMedicine(context, med) }
                )
            } else {
                DrugReferenceTab()
            }
        }

        if (showAddDialog) {
            AddMedicineDialog(
                profileId = profileId,
                onDismiss = { showAddDialog = false },
                onAdd = { newMed ->
                    viewModel.addMedicine(context, newMed)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MedicineListTab(
    medicines: List<MedicineEntity>,
    onToggle: (MedicineEntity) -> Unit,
    onDelete: (MedicineEntity) -> Unit
) {
    if (medicines.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No Prescriptions Added",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap the + button to add medications and set exact dose alarm reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(medicines) { med ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (med.isActive) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Dose: ${med.dosage} (${med.frequency})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = med.isActive,
                            onCheckedChange = { onToggle(med) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Time slots & meal advice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (med.takeMorning) SlotChip(label = "Morn ${med.morningTime}")
                            if (med.takeAfternoon) SlotChip(label = "Aft ${med.afternoonTime}")
                            if (med.takeEvening) SlotChip(label = "Eve ${med.eveningTime}")
                            if (med.takeNight) SlotChip(label = "Night ${med.nightTime}")
                        }

                        IconButton(onClick = { onDelete(med) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "• Timing: ${med.mealTiming} ${if (med.notes.isNotBlank()) "— ${med.notes}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SlotChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DrugReferenceTab() {
    var query by remember { mutableStateOf("") }
    val searchResults = remember(query) {
        DrugDatabase.searchDrug(query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Generic or Brand (e.g., Napa, Monas, Metformin)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(searchResults) { drug ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(drug.genericName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Brands: ${drug.brandNames.joinToString(", ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Indication: ${drug.indication}", style = MaterialTheme.typography.bodySmall)
                        Text("Standard Dosage: ${drug.standardDose}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("Meal Advice: ${drug.mealAdvice}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Precaution: ${drug.precautions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddMedicineDialog(
    profileId: Long,
    onDismiss: () -> Unit,
    onAdd: (MedicineEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Daily") }
    var mealTiming by remember { mutableStateOf("After Meal") }
    var notes by remember { mutableStateOf("") }

    var takeMorning by remember { mutableStateOf(true) }
    var takeAfternoon by remember { mutableStateOf(false) }
    var takeEvening by remember { mutableStateOf(true) }
    var takeNight by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medication Reminder", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name (e.g., Montelukast)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g., 10 mg / 1 tablet)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Daily Dose Schedules:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilterChip(selected = takeMorning, onClick = { takeMorning = !takeMorning }, label = { Text("Morn (08:00)") })
                    FilterChip(selected = takeAfternoon, onClick = { takeAfternoon = !takeAfternoon }, label = { Text("Aft (13:00)") })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilterChip(selected = takeEvening, onClick = { takeEvening = !takeEvening }, label = { Text("Eve (18:00)") })
                    FilterChip(selected = takeNight, onClick = { takeNight = !takeNight }, label = { Text("Night (21:30)") })
                }

                OutlinedTextField(
                    value = mealTiming,
                    onValueChange = { mealTiming = it },
                    label = { Text("Meal Relation (e.g. After Meal, Before Bed)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Doctor Instructions / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            MedicineEntity(
                                profileId = profileId,
                                name = name.trim(),
                                dosage = dosage.trim(),
                                frequency = frequency,
                                takeMorning = takeMorning,
                                takeAfternoon = takeAfternoon,
                                takeEvening = takeEvening,
                                takeNight = takeNight,
                                mealTiming = mealTiming,
                                notes = notes,
                                isActive = true
                            )
                        )
                    }
                }
            ) {
                Text("Save & Set Alarms")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
