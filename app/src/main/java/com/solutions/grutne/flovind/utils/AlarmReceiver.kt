package com.solutions.grutne.flovind.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import com.solutions.grutne.flovind.MainActivity
import com.solutions.grutne.flovind.R


import timber.log.Timber

/**
 * Created by Adrian on 29/10/2017.
 */

class AlarmReceiver : BroadcastReceiver() {

    @Throws(NullPointerException::class)
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("onReceive")

        val nextLowTideTime = intent?.getStringExtra("nextLowTideTime")
        val nextLowTideLevel = intent?.getStringExtra("nextLowTideLevel")
//        val nextHighTideTime = intent.getStringExtra("nextHighTideTime")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT)

            // Configure the notification channel.
            notificationChannel.description = "Channel description"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setVibrate(longArrayOf(0, 100, 100, 100, 100, 100))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_light)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.common_google_signin_btn_icon_light))
                .setContentTitle("Tide alert! ")
                .setContentText("Next low tide $nextLowTideTime. Level $nextLowTideLevel")
                .setAutoCancel(true)

        val activityIntent = Intent(context, MainActivity::class.java)
        val taskStackBuilder = TaskStackBuilder.create(context)
        taskStackBuilder.addNextIntentWithParentStack(activityIntent)
        val resultPendingIntent = taskStackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationBuilder.setContentIntent(resultPendingIntent)

        val notificationsEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.resources.getString(R.string.pref_enable_notifications_key),
                context.resources.getBoolean(R.bool.show_notifications_by_default))
        if (notificationsEnabled)
            try {
                notificationManager.notify(TIDES_NOTIFICATION_ID, notificationBuilder.build())
            } catch (e: NullPointerException) {
                Timber.d("failed notifying: " + e.message)
                e.printStackTrace()
            }
    }

    companion object {
        private const val TIDES_NOTIFICATION_ID = 1349
        private const val NOTIFICATION_CHANNEL_ID = "my_notification_channel"
    }
}
