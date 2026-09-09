package com.medtrack.app.domain.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_MEDICINE_ALARM -> {
                val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
                val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine"
                val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: ""
                val mealTiming = intent.getStringExtra(EXTRA_MED_MEAL) ?: "After Meal"

                NotificationHelper.showMedicineNotification(
                    context = context,
                    medicineId = medId,
                    medicineName = medName,
                    dosage = dosage,
                    mealTiming = mealTiming
                )

                // Reschedule for next day at same time
                rescheduleNextDay(context, intent)
            }

            ACTION_TAKE_MEDICINE -> {
                val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(medId.toInt())
            }

            ACTION_SNOOZE_MEDICINE -> {
                val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
                val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine"
                val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: ""
                val mealTiming = intent.getStringExtra(EXTRA_MED_MEAL) ?: "After Meal"

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(medId.toInt())

                // Schedule alarm 15 minutes later
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val snoozeIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                    this.action = ACTION_MEDICINE_ALARM
                    putExtra(EXTRA_MED_ID, medId)
                    putExtra(EXTRA_MED_NAME, medName)
                    putExtra(EXTRA_MED_DOSAGE, dosage)
                    putExtra(EXTRA_MED_MEAL, mealTiming)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (medId * 1000 + 99).toInt(),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAt = System.currentTimeMillis() + 15 * 60 * 1000
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // Restore all active alarms from Room database
                val app = context.applicationContext as? MedTrackApplication
                if (app != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val activeMeds = app.medicineRepository.getAllActiveMedicinesSync()
                        val scheduler = MedicineAlarmScheduler(context)
                        for (med in activeMeds) {
                            scheduler.scheduleReminders(med)
                        }
                    }
                }
            }
        }
    }

    private fun rescheduleNextDay(context: Context, intent: Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
        val slotId = intent.getIntExtra(EXTRA_SLOT_ID, 1)
        val requestCode = (medId * 100 + slotId).toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextDayTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextDayTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextDayTime, pendingIntent)
        }
    }
}
