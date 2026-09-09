package com.medtrack.app.domain.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medtrack.app.data.local.entity.MedicineEntity
import java.util.Calendar

class MedicineAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminders(medicine: MedicineEntity) {
        if (!medicine.isActive) return

        if (medicine.takeMorning) scheduleDoseAlarm(medicine, medicine.morningTime, 1)
        if (medicine.takeAfternoon) scheduleDoseAlarm(medicine, medicine.afternoonTime, 2)
        if (medicine.takeEvening) scheduleDoseAlarm(medicine, medicine.eveningTime, 3)
        if (medicine.takeNight) scheduleDoseAlarm(medicine, medicine.nightTime, 4)
    }

    private fun scheduleDoseAlarm(medicine: MedicineEntity, timeStr: String, slotId: Int) {
        val parts = timeStr.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: 8
        val minute = parts[1].toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = MedicineAlarmReceiver.ACTION_MEDICINE_ALARM
            putExtra(MedicineAlarmReceiver.EXTRA_MED_ID, medicine.id)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_NAME, medicine.name)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_DOSAGE, medicine.dosage)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_MEAL, medicine.mealTiming)
            putExtra(MedicineAlarmReceiver.EXTRA_SLOT_ID, slotId)
        }

        val requestCode = (medicine.id * 100 + slotId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // In case SCHEDULE_EXACT_ALARM permission is revoked
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminders(medicine: MedicineEntity) {
        for (slotId in 1..4) {
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = MedicineAlarmReceiver.ACTION_MEDICINE_ALARM
            }
            val requestCode = (medicine.id * 100 + slotId).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
