package com.medtrack.app.domain.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

        val intent = alarmIntent(medicine, slotId, hour, minute)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmRequestCodes.alarm(medicine.id, slotId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        AlarmScheduling.schedule(context, calendar.timeInMillis, pendingIntent)
    }

    /** Intent shared by scheduling, rescheduling and cancellation. */
    fun alarmIntent(medicine: MedicineEntity, slotId: Int, hour: Int? = null, minute: Int? = null): Intent {
        return Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = MedicineAlarmReceiver.ACTION_MEDICINE_ALARM
            putExtra(MedicineAlarmReceiver.EXTRA_MED_ID, medicine.id)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_NAME, medicine.name)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_DOSAGE, medicine.dosage)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_MEAL, medicine.mealTiming)
            putExtra(MedicineAlarmReceiver.EXTRA_SLOT_ID, slotId)
            if (hour != null) putExtra(MedicineAlarmReceiver.EXTRA_HOUR, hour)
            if (minute != null) putExtra(MedicineAlarmReceiver.EXTRA_MINUTE, minute)
        }
    }

    fun cancelReminders(medicine: MedicineEntity) {
        for (slotId in 1..4) {
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = MedicineAlarmReceiver.ACTION_MEDICINE_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                AlarmRequestCodes.alarm(medicine.id, slotId),
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
