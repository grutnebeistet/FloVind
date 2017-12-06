package com.solutions.grutne.flovind.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import com.solutions.grutne.flovind.R
import com.solutions.grutne.flovind.models.TidesData

import java.util.Calendar
import java.util.concurrent.TimeUnit

import timber.log.Timber

/**
 * Created by Adrian on 29/10/2017.
 */

object NotificationUtils {
    private val PREVIOUS_NOTIFICATION_TIME = "prev_not"
    private val PREVIOUS_NOTIFICATION_OFFSET = "prev_offset"

    fun prepareNotification(context: Context, waterlevels: List<TidesData.Waterlevel>) {
        // get next low tide to notify about
        var nextLow: TidesData.Waterlevel? = null
        var nextHighAfterLow: TidesData.Waterlevel? = null
        for (i in waterlevels.indices) {
            val l = waterlevels[i]
            if (l.flag.equals("low") && Utils.timeIsAfterNowInclMidnight(l.dateTime)) {// Utils.timeIsAfterNow(Utils.getFormattedTime(l.dateTime))) {
                //  nextLow = (nextLow == null || (l.dateTime.compareTo(nextLow.dateTime) < 0) ? l : nextLow);
                if (nextLow == null || l.dateTime.compareTo(nextLow.dateTime) < 0) {
                    nextLow = l
                    if (waterlevels.size > i + 1) nextHighAfterLow = waterlevels[i + 1]
                }
            }
        }
        if (nextLow != null) {

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // In case lowtide is after midnight, date must be considered
            val lowTideDateString = Utils.getFormattedDate(nextLow.dateTime)
            val lowTideTimeString = Utils.getFormattedTime(nextLow.dateTime)

            val calendarLowTide = Calendar.getInstance()

            calendarLowTide.set(Calendar.YEAR, Integer.valueOf(lowTideDateString.substring(0, 4))!!)
            calendarLowTide.set(Calendar.MONTH, Integer.valueOf(lowTideDateString.substring(5, 7))!! - 1) // months are counted from 0
            calendarLowTide.set(Calendar.DATE, Integer.valueOf(lowTideDateString.substring(8))!!)
            calendarLowTide.set(Calendar.HOUR_OF_DAY, Integer.valueOf(lowTideTimeString.substring(0, 2))!!)
            calendarLowTide.set(Calendar.MINUTE, Integer.valueOf(lowTideTimeString.substring(3, 5))!!)
            Timber.d("Date from calendar thing: " + calendarLowTide.time + "\nvia utils: " +
                    Utils.getDate(calendarLowTide.timeInMillis) + " " + Utils.getTime(calendarLowTide.timeInMillis))

            val currentTime = System.currentTimeMillis()
            val lowTideTime = calendarLowTide.timeInMillis
            val hoursOffsetPrefs = PreferenceManager.getDefaultSharedPreferences(context).getString(
                    context.getString(R.string.notify_hours_key), context.getString(R.string.notify_hours_default))
            Timber.d("NOTIFY OFFSET PREF: " + hoursOffsetPrefs!!)
            val offset = TimeUnit.HOURS.toMillis(Integer.parseInt(hoursOffsetPrefs).toLong())
            val offsetMargin = TimeUnit.MINUTES.toMillis(1)
            var notificationTime = lowTideTime - offset + offsetMargin

            // set notification time to one minute from now if it's less than 3 hours till low tide
            //if ((notificationTime + offsetMargin) < notificationTime); //;(calendarLowTide.getTimeInMillis() - offset)))
            if (currentTime + offset > lowTideTime)
                notificationTime = currentTime// + offsetMargin;

            val myIntent = Intent(context, AlarmReceiver::class.java)   //(AlarmReceiver.INTENT_FILTER);
            myIntent.putExtra("nextLowTideTime", lowTideTime)
            myIntent.putExtra("nextLowTideLevel", nextLow.waterValue)

            if (nextHighAfterLow != null) {
                myIntent.putExtra("nextHighTideTime", nextHighAfterLow.dateTime)
                myIntent.putExtra("nextHighTideLevel", nextHighAfterLow.waterValue)
            }

            val pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            // PendingIntent.FLAG_CANCEL_CURRENT);  // FLAG to avoid creating a second service if there's already one running

            // Prepare notification only if it hasn't already been shown for this low tide
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val previousNotificationTime = preferences.getLong(PREVIOUS_NOTIFICATION_TIME, 0)
            val previousNotificationOffset = preferences.getString(PREVIOUS_NOTIFICATION_OFFSET, "0")

            Timber.d("Previous time: $previousNotificationTime, newtime: $notificationTime")
            if (!(Utils.getTime(previousNotificationTime) == (Utils.getTime(lowTideTime)) && hoursOffsetPrefs == previousNotificationOffset)) {
                val SDK_INT = Build.VERSION.SDK_INT
                if (SDK_INT >= Build.VERSION_CODES.M)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
                else if (Build.VERSION_CODES.KITKAT <= SDK_INT && SDK_INT < Build.VERSION_CODES.M)
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
                else
                    alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)

                Timber.d("New alarm set for " + Utils.getTime(notificationTime))
                preferences.edit().putLong(PREVIOUS_NOTIFICATION_TIME, lowTideTime).putString(PREVIOUS_NOTIFICATION_OFFSET, hoursOffsetPrefs).apply()
            }
        }
    }
}
