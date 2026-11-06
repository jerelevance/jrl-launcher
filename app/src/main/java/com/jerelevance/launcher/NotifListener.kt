package com.jerelevance.launcher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Optional. Once the user grants notification access (Settings › Notification access › JRL),
 * this keeps a per-app count of clearable notifications for the top bar and the app cards.
 * It never reads notification content.
 */
class NotifListener : NotificationListenerService() {

    companion object {
        @Volatile
        var counts: Map<String, Int> = emptyMap()
            private set
    }

    override fun onListenerConnected() = recount()
    override fun onListenerDisconnected() { counts = emptyMap() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = recount()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = recount()

    private fun recount() {
        val m = HashMap<String, Int>()
        try {
            activeNotifications?.forEach { s ->
                if (s.isClearable && !s.isOngoing && (s.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0) {
                    m[s.packageName] = (m[s.packageName] ?: 0) + 1
                }
            }
        } catch (_: Exception) {
        }
        counts = m
    }
}
