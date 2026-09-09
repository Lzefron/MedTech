package com.medtrack.app.domain.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.medtrack.app.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "medtrack_reminders"
    private const val CHANNEL_NAME = "Medicine Reminders"
    private const val CHANNEL_DESC = "High priority notifications for scheduled medication doses"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMedicineNotification(
        context: Context,
        medicineId: Long,
        medicineName: String,
        dosage: String,
        mealTiming: String,
        slotId: Int = 1
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            AlarmRequestCodes.tap(medicineId),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Take
        val takeIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = MedicineAlarmReceiver.ACTION_TAKE_MEDICINE
            putExtra(MedicineAlarmReceiver.EXTRA_MED_ID, medicineId)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmRequestCodes.take(medicineId),
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze (carries slot id so the re-arm targets the right dose)
        val snoozeIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = MedicineAlarmReceiver.ACTION_SNOOZE_MEDICINE
            putExtra(MedicineAlarmReceiver.EXTRA_MED_ID, medicineId)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_NAME, medicineName)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_DOSAGE, dosage)
            putExtra(MedicineAlarmReceiver.EXTRA_MED_MEAL, mealTiming)
            putExtra(MedicineAlarmReceiver.EXTRA_SLOT_ID, slotId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmRequestCodes.snooze(medicineId),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time to take $medicineName")
            .setContentText("Dose: $dosage ($mealTiming)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Prescribed Dose: $dosage\nTiming: $mealTiming\nStay on schedule for optimal health."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Take Now", takePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze (15m)", snoozePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(AlarmRequestCodes.notification(medicineId), notification)
    }
}
