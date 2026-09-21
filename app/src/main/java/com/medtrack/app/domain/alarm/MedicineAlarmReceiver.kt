package com.medtrack.app.domain.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medtrack.app.MedTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicineAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MEDICINE_ALARM = "com.medtrack.app.ACTION_MEDICINE_ALARM"
        const val ACTION_TAKE_MEDICINE = "com.medtrack.app.ACTION_TAKE_MEDICINE"
        const val ACTION_SNOOZE_MEDICINE = "com.medtrack.app.ACTION_SNOOZE_MEDICINE"
        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_MED_DOSAGE = "extra_med_dosage"
        const val EXTRA_MED_MEAL = "extra_med_meal"
        const val EXTRA_SLOT_ID = "extra_slot_id"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
    }
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as? MedTrackApplication ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = MedicineAlarmScheduler(context)
                if (action == Intent.ACTION_BOOT_COMPLETED) {
                    app.medicineRepository.getAllActiveMedicinesSync().forEach { scheduler.scheduleReminders(it) }
                    return@launch
                }
                val id = intent.getLongExtra(EXTRA_MED_ID, 0L)
                val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (action == ACTION_TAKE_MEDICINE) { notifications.cancel(AlarmRequestCodes.notification(id)); return@launch }
                // Old/snoozed alarms must not resurrect a deleted or paused prescription.
                val medicine = app.medicineRepository.getMedicineByIdSync(id)?.takeIf { it.isActive } ?: return@launch
                val slot = intent.getIntExtra(EXTRA_SLOT_ID, 1)
                val enabled = when (slot) { 1 -> medicine.takeMorning; 2 -> medicine.takeAfternoon; 3 -> medicine.takeEvening; 4 -> medicine.takeNight; else -> false }
                if (!enabled) return@launch
                when (action) {
                    ACTION_MEDICINE_ALARM -> {
                        NotificationHelper.showMedicineNotification(context, medicine.id, medicine.name, medicine.dosage, medicine.mealTiming, slot)
                        scheduler.scheduleReminders(medicine)
                    }
                    ACTION_SNOOZE_MEDICINE -> {
                        notifications.cancel(AlarmRequestCodes.notification(id))
                        val alarm = PendingIntent.getBroadcast(context, AlarmRequestCodes.snooze(id),
                            scheduler.alarmIntent(medicine, slot), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        AlarmScheduling.schedule(context, System.currentTimeMillis() + 15 * 60 * 1000, alarm)
                    }
                }
            } catch (_: Exception) {
                android.util.Log.w("MedTrackAlarms", "Reminder delivery failed; check notification and alarm permissions.")
            } finally { pending.finish() }
        }
    }
}
