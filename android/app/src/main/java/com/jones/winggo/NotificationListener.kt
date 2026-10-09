package com.jones.winggo

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {

    private var lastNotificationTime = 0L

    override fun onNotificationPosted(
        sbn: StatusBarNotification?
    ) {
        super.onNotificationPosted(sbn)

        if (sbn == null) {
            return
        }

        // Ignore WingGo's own foreground notification
        if (sbn.packageName == packageName) {
            return
        }

        val currentTime =
            System.currentTimeMillis()

        // Prevent duplicate events
        if (
            currentTime -
                lastNotificationTime < 800
        ) {
            return
        }

        lastNotificationTime =
            currentTime

        // Send directly to native Floating WingGo
        WingGoForegroundService
            .onNotificationReceived()
    }
}