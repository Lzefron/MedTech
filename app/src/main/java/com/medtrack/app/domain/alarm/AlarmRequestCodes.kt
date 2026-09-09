package com.medtrack.app.domain.alarm

/**
 * Single source of truth for every PendingIntent request code and notification
 * id used by the reminder system. Distinct non-overlapping ranges keep alarm,
 * snooze, take and notification-tap PendingIntents from colliding (a collision
 * with FLAG_UPDATE_CURRENT silently overwrites one intent with another).
 */
object AlarmRequestCodes {
    private const val ALARM_BASE = 1_000_000
    private const val SNOOZE_BASE = 2_000_000
    private const val TAKE_BASE = 3_000_000
    private const val TAP_BASE = 4_000_000
    private const val NOTIFICATION_BASE = 5_000_000

    /** Dose alarm for [medId] and dose slot [slotId] (1-4). */
    fun alarm(medId: Long, slotId: Int): Int = (ALARM_BASE + medId * 10 + slotId).toInt()

    /** 15-minute snooze re-arm for [medId]. */
    fun snooze(medId: Long): Int = (SNOOZE_BASE + medId).toInt()

    /** "Take Now" notification action for [medId]. */
    fun take(medId: Long): Int = (TAKE_BASE + medId).toInt()

    /** Notification content-tap intent for [medId]. */
    fun tap(medId: Long): Int = (TAP_BASE + medId).toInt()

    /** Posted notification id for [medId]. */
    fun notification(medId: Long): Int = (NOTIFICATION_BASE + medId).toInt()
}
