package com.medtrack.app.ui.screens.medicines

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.medtrack.app.data.local.entity.MedicineEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScreen(viewModel: MedicineViewModel, profileId: Long) {
    val context = LocalContext.current
    LaunchedEffect(profileId) { viewModel.setProfileId(profileId) }
    val all by viewModel.medicines.collectAsState()
    val medicines = all.filter { it.profileId == profileId }
    val error by viewModel.error.collectAsState()
    val saving by viewModel.saving.collectAsState()
    var creating by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MedicineEntity?>(null) }
    var deleting by remember { mutableStateOf<MedicineEntity?>(null) }
    var notifications by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifications = it }
    val exact = Build.VERSION.SDK_INT < 31 || (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Your medicines") }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true; viewModel.error.value = null }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add medicine") }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Record the dose and times from your prescription. The app does not choose a treatment or dose for you.") }
            if (!notifications) item {
                OutlinedCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("Notifications are off. Dose alerts cannot be shown.")
                        TextButton(onClick = { if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Enable notifications") }
                    }
                }
            }
            if (!exact) item { Text("Android is using approximate reminders. For more precise timing, allow Alarms & reminders for MedTrack in system settings.", style = MaterialTheme.typography.bodySmall) }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (medicines.isEmpty()) item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Medication, null, Modifier.size(40.dp))
                        Text("One less thing to remember", style = MaterialTheme.typography.headlineSmall)
                        Text("Add a prescribed medicine and choose its daily reminder times. No times or doses are selected for you.")
                    }
                }
            }
            items(medicines, key = { it.id }) { medicine ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(medicine.name, style = MaterialTheme.typography.titleLarge)
                                Text(medicine.dosage)
                            }
                            Switch(medicine.isActive, { viewModel.toggleMedicine(context, medicine) })
                        }
                        Text(if (medicine.isActive) "Daily reminders on" else "Reminders paused", color = MaterialTheme.colorScheme.primary)
                        val times = listOfNotNull(medicine.morningTime.takeIf { medicine.takeMorning }, medicine.afternoonTime.takeIf { medicine.takeAfternoon }, medicine.eveningTime.takeIf { medicine.takeEvening }, medicine.nightTime.takeIf { medicine.takeNight })
                        Text(times.sorted().joinToString(" · "))
                        if (medicine.mealTiming.isNotBlank()) Text(medicine.mealTiming, style = MaterialTheme.typography.bodySmall)
                        if (medicine.notes.isNotBlank()) Text(medicine.notes, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { editing = medicine; viewModel.error.value = null }) { Text("Edit") }
                            TextButton(onClick = { deleting = medicine }) { Text("Remove") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (creating || editing != null) MedicineEditor(profileId, editing, saving, error,
        onDismiss = { if (!saving) { creating = false; editing = null } },
        onSave = { medicine ->
            viewModel.saveMedicine(context, medicine) {
                creating = false; editing = null
                if (!notifications && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        })
    deleting?.let { medicine ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Remove medicine?") }, text = { Text("This removes the record and stops its reminders. It does not change your prescription.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteMedicine(context, medicine); deleting = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}

@Composable
private fun MedicineEditor(profileId: Long, initial: MedicineEntity?, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (MedicineEntity) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var dose by rememberSaveable { mutableStateOf(initial?.dosage ?: "") }
    var meal by rememberSaveable { mutableStateOf(initial?.mealTiming ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var morning by rememberSaveable { mutableStateOf(initial?.takeMorning ?: false) }
    var afternoon by rememberSaveable { mutableStateOf(initial?.takeAfternoon ?: false) }
    var evening by rememberSaveable { mutableStateOf(initial?.takeEvening ?: false) }
    var night by rememberSaveable { mutableStateOf(initial?.takeNight ?: false) }
    var morningTime by rememberSaveable { mutableStateOf(initial?.morningTime ?: "") }
    var afternoonTime by rememberSaveable { mutableStateOf(initial?.afternoonTime ?: "") }
    var eveningTime by rememberSaveable { mutableStateOf(initial?.eveningTime ?: "") }
    var nightTime by rememberSaveable { mutableStateOf(initial?.nightTime ?: "") }
    AlertDialog(modifier = Modifier.imePadding(), onDismissRequest = onDismiss, title = { Text(if (initial == null) "Add prescribed medicine" else "Edit medicine") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(100) }, label = { Text("Medicine name *") })
                OutlinedTextField(dose, { dose = it.take(120) }, label = { Text("Prescribed dose *") }, supportingText = { Text("Copy the strength and amount from your prescription.") })
                Text("Choose daily reminder times (24-hour clock). Only daily schedules are supported.")
                DoseTime("Morning", morning, { morning = it }, morningTime, { morningTime = it })
                DoseTime("Afternoon", afternoon, { afternoon = it }, afternoonTime, { afternoonTime = it })
                DoseTime("Evening", evening, { evening = it }, eveningTime, { eveningTime = it })
                DoseTime("Night", night, { night = it }, nightTime, { nightTime = it })
                OutlinedTextField(meal, { meal = it.take(120) }, label = { Text("Food instructions (optional)") })
                OutlinedTextField(notes, { notes = it.take(1000) }, label = { Text("Instructions / end date (optional)") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(enabled = !saving && name.isNotBlank() && dose.isNotBlank(), onClick = {
            onSave((initial ?: MedicineEntity(profileId = profileId, name = "", dosage = "")).copy(name = name.trim(), dosage = dose.trim(), frequency = "Daily",
                takeMorning = morning, takeAfternoon = afternoon, takeEvening = evening, takeNight = night,
                morningTime = morningTime.trim(), afternoonTime = afternoonTime.trim(), eveningTime = eveningTime.trim(), nightTime = nightTime.trim(),
                mealTiming = meal.trim(), notes = notes.trim()))
        }) { Text(if (saving) "Saving…" else "Save medicine") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun DoseTime(label: String, selected: Boolean, onSelect: (Boolean) -> Unit, time: String, onTime: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(selected, onSelect); Text(label) }
    if (selected) OutlinedTextField(time, { onTime(it.take(5)) }, label = { Text("Time * · HH:mm") }, singleLine = true,
        isError = time.isNotBlank() && !Regex("""(?:[01]\d|2[0-3]):[0-5]\d""").matches(time))
}
