package com.solutions.grutne.flovind.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Build
import android.widget.Toast
import com.solutions.grutne.flovind.ForecastFragment
import com.solutions.grutne.flovind.R
import com.solutions.grutne.flovind.data.DbContract
import com.solutions.grutne.flovind.data.TidesDbHelper
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeParseException

import java.util.concurrent.TimeUnit

import timber.log.Timber

/**
 * Created by Adrian on 29/10/2017.
 *
 * Create, cancel, or update an AlarmManager to push a notification
 */

object NotificationUtils {
    private const val TIDES_NOTIFICATION_ALARM_REQUEST_CODE = 1200
    const val ACTION_ALARM_PUSH_NOTIFICATION = "action_push_notification"
    const val EXTRA_NEXT_LOW_TIDE_TIME = "next_low_tide_time"
    const val EXTRA_NEXT_LOW_TIDE_LEVEL = "next_low_tide_level"

    fun cancelNotification(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java) // TODO Trengs exact samme intent som setAlarm brukte?
        val pendingIntent = PendingIntent.getBroadcast(context, TIDES_NOTIFICATION_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }

    /**
     * Prepare a new Notification and alarm for each low tide after now until last tide in batch.
     *
     * @param context
     * @param userOffset, integer hours prior to tide
     * */
    fun prepareNotification(context: Context, userOffset: Long) {
        val sortOrder = DbContract.TidesEntry.COLUMN_TIDES_DATE + " ASC"

        try {
            TidesDbHelper(context).readableDatabase.query(
                    DbContract.TidesEntry.TABLE_TIDES_HOME,
                    ForecastFragment.TIDES_PROJECTION,
                    null,
                    null,
                    null,
                    null,
                    sortOrder).use {

                while (it.moveToNext()) {
                    val flag = it.getString(ForecastFragment.INDEX_FLAG)
                    val date = it.getString(ForecastFragment.INDEX_TIDE_DATE)
                    val time = it.getString(ForecastFragment.INDEX_LEVEL_TIME)
                    val level = it.getString(ForecastFragment.INDEX_TIDE_LEVEL)
                    val rawDate = it.getString(ForecastFragment.INDEX_TIDE_DATE_RAW)

                    val levelText = context.getString(R.string.level_format, level)

                    val lowTideEpoch = FloVindDateUtils.getRawDateTimeInMillis(rawDate)

                    if (flag == "low" && lowTideEpoch > System.currentTimeMillis() + userOffset) {
                        Timber.d("Notific on Flag: $flag\ndate: $date\ntime: $time\nlevel: $levelText")

                        val notificationEpoch = lowTideEpoch - userOffset
                        Timber.d("setting notification for time ${FloVindDateUtils.getPersistentDateFromMillis(notificationEpoch)} -- ${FloVindDateUtils.millisToDisplayTime(notificationEpoch)}")

                        val alarmIntent = Intent(context, AlarmReceiver::class.java)
                        alarmIntent.action = ACTION_ALARM_PUSH_NOTIFICATION
                        alarmIntent.putExtra(EXTRA_NEXT_LOW_TIDE_TIME, time)
                        alarmIntent.putExtra(EXTRA_NEXT_LOW_TIDE_LEVEL, level)

                        val pendingIntent = PendingIntent.getBroadcast(context, TIDES_NOTIFICATION_ALARM_REQUEST_CODE, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                        val SDK_INT = Build.VERSION.SDK_INT
                        if (SDK_INT >= Build.VERSION_CODES.M)
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationEpoch, pendingIntent)
                        else if (Build.VERSION_CODES.KITKAT <= SDK_INT && SDK_INT < Build.VERSION_CODES.M)
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationEpoch, pendingIntent)
                        else
                            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationEpoch, pendingIntent)

                        break
                    }
                }
            }
        } catch (sle: SQLiteException) {
            Timber.e("Failed to retrieve database: ${sle.localizedMessage}")
        } catch (dtpe: DateTimeParseException) {
            Timber.e("Failed to parse epoch time: ${dtpe.localizedMessage}")
        }
    }
}
