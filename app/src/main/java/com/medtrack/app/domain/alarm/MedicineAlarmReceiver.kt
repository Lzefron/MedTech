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
import java.util.Calendar

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

        when (action) {
            ACTION_MEDICINE_ALARM -> {
                val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
                val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine"
                val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: ""
                val mealTiming = intent.getStringExtra(EXTRA_MED_MEAL) ?: "After Meal"
                val slotId = intent.getIntExtra(EXTRA_SLOT_ID, 1)

                NotificationHelper.showMedicineNotification(
                    context = context,
                    medicineId = medId,
                    medicineName = medName,
                    dosage = dosage,
                    mealTiming = mealTiming,
                    slotId = slotId
                )

                // Reschedule for next day at the same wall-clock time
                rescheduleNextDay(context, intent)
            }

            ACTION_TAKE_MEDICINE -> {
                val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(AlarmRequestCodes.notification(medId))
            }

            ACTION_SNOOZE_MEDICINE -> {
                val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
                val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine"
                val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: ""
                val mealTiming = intent.getStringExtra(EXTRA_MED_MEAL) ?: "After Meal"
                val slotId = intent.getIntExtra(EXTRA_SLOT_ID, 1)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(AlarmRequestCodes.notification(medId))

                // Schedule alarm 15 minutes later. The slot id rides along so the
                // eventual daily reschedule re-arms the right dose alarm instead
                // of overwriting the slot-1 (morning) alarm.
                val snoozeIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                    this.action = ACTION_MEDICINE_ALARM
                    putExtra(EXTRA_MED_ID, medId)
                    putExtra(EXTRA_MED_NAME, medName)
                    putExtra(EXTRA_MED_DOSAGE, dosage)
                    putExtra(EXTRA_MED_MEAL, mealTiming)
                    putExtra(EXTRA_SLOT_ID, slotId)
                    copyTimeExtras(intent)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    AlarmRequestCodes.snooze(medId),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                AlarmScheduling.schedule(
                    context,
                    System.currentTimeMillis() + 15 * 60 * 1000,
                    pendingIntent
                )
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // Restore all active alarms from Room database. goAsync() keeps
                // the process alive while the DB read runs instead of dropping
                // the work when onReceive returns.
                val app = context.applicationContext as? MedTrackApplication
                if (app != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val activeMeds = app.medicineRepository.getAllActiveMedicinesSync()
                            val scheduler = MedicineAlarmScheduler(context)
                            for (med in activeMeds) {
                                scheduler.scheduleReminders(med)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    private fun Intent.copyTimeExtras(source: Intent) {
        if (source.hasExtra(EXTRA_HOUR)) putExtra(EXTRA_HOUR, source.getIntExtra(EXTRA_HOUR, -1))
        if (source.hasExtra(EXTRA_MINUTE)) putExtra(EXTRA_MINUTE, source.getIntExtra(EXTRA_MINUTE, -1))
    }

    private fun rescheduleNextDay(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
        val slotId = intent.getIntExtra(EXTRA_SLOT_ID, 1)
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmRequestCodes.alarm(medId, slotId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Recompute the next wall-clock occurrence of the dose time instead of
        // adding 24h to the delivery time (which drifts and mishandles DST).
        // Fall back to +24h only if the original time extras are missing.
        val triggerAt: Long = if (hour >= 0 && minute >= 0) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            next.timeInMillis
        } else {
            System.currentTimeMillis() + 24 * 60 * 60 * 1000
        }

        AlarmScheduling.schedule(context, triggerAt, pendingIntent)
    }
}
