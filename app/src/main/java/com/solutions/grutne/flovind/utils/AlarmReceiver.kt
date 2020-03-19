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
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.solutions.grutne.flovind.MainActivity
import com.solutions.grutne.flovind.R
import com.solutions.grutne.flovind.utils.NotificationUtils.ACTION_ALARM_PUSH_NOTIFICATION


import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by Adrian on 29/10/2017.
 */

class AlarmReceiver : BroadcastReceiver() {

    @Throws(NullPointerException::class)
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("onReceive  ${intent?.action}")

        when (intent?.action) {
            ACTION_ALARM_PUSH_NOTIFICATION -> {
                pushNotification(intent, context)
                setupNexNotification(context)
            }
        }
    }

    // Due to the FireBaseJobDispatcher, there should be data in the DB
    private fun setupNexNotification(context: Context) {
        val enableNotification = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.pref_enable_notifications_key), false)
        val prefOffset = PreferenceManager.getDefaultSharedPreferences(context).getString(
                context.getString(R.string.notify_hours_key), context.getString(R.string.notify_hours_default))

        if (enableNotification) {
            val userOffset = TimeUnit.HOURS.toMillis(prefOffset!!.toLong())
            NotificationUtils.prepareNotification(context.applicationContext, userOffset = userOffset)
            Timber.d("setupNexNotification")
        }
    }

    private fun pushNotification(intent: Intent?, context: Context) {

        val nextLowTideTime = intent?.getStringExtra(NotificationUtils.EXTRA_NEXT_LOW_TIDE_TIME)
        val nextLowTideLevel = intent?.getStringExtra(NotificationUtils.EXTRA_NEXT_LOW_TIDE_LEVEL)
//        val nextHighTideTime = intent.getStringExtra("nextHighTideTime")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_DEFAULT)

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
                .setColor(ContextCompat.getColor(context, R.color.ocean_blue))
                .setSmallIcon(R.drawable.ic_notification)
//                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground))
                .setContentTitle("Low Tide Alert! $nextLowTideTime o'clock.")
                .setContentText("Approaching minimum water level of $nextLowTideLevel cm.")
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
        private const val NOTIFICATION_CHANNEL_ID = "flovind_notification_channel"
        private const val NOTIFICATION_NAME = "flovind_notification"
    }
}
