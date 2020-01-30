package com.solutions.grutne.flovind.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Build
import android.preference.PreferenceManager
import android.widget.Toast
import com.solutions.grutne.flovind.ForecastFragment
import com.solutions.grutne.flovind.R
import com.solutions.grutne.flovind.data.DbContract
import com.solutions.grutne.flovind.data.TidesDbHelper
import org.threeten.bp.format.DateTimeParseException

import java.util.concurrent.TimeUnit

import timber.log.Timber

/**
 * Created by Adrian on 29/10/2017.
 *
 * Create, cancel, or update an AlarmManager to push a notification
 */

object NotificationUtils {
    private const val PREVIOUS_NOTIFICATION_TIME = "prev_not"
    private const val PREVIOUS_NOTIFICATION_OFFSET = "prev_offset"
    private const val TIDES_NOTIFICATION_OFFSET_REQUEST_CODE = 1200


    fun updateNotificationOnOffsetChange(context: Context) {
        // if current home loc is different than already made notification's home loc,
//        prepareNotification(context)
        Timber.d("UpdateNotification")
        // else update offset
    }

    fun cancelNotification(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java) // TODO Trengs exact samme intent som setAlarm brukte?
        val pendingIntent = PendingIntent.getBroadcast(context, TIDES_NOTIFICATION_OFFSET_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }

    //     TODO thread safe
    /**
     * Prepare a new Notification and alarm for each low tide after now until last tide in batch.
     *
     * @param context
     * @param userOffset, integer hours prior to tide
     * */
    fun prepareNotification(context: Context, userOffset: Int) {

        val sortOrder = DbContract.TidesEntry.COLUMN_TIDES_DATE + " ASC"
//        val selection = "${DbContract.TidesEntry.COLUMN_TIDES_DATE} =? OR ${DbContract.TidesEntry.COLUMN_TIDES_DATE} =?"
//        val selectionArgs = arrayOf(Utils.getPrettyDateFromMs(System.currentTimeMillis()), Utils.getPrettyDatePlusOne(System.currentTimeMillis()))

        try {
            TidesDbHelper(context).readableDatabase.query(
                    DbContract.TidesEntry.TABLE_TIDES,
                    ForecastFragment.TIDES_PROJECTION,
//                selection,
                    null,
//                selectionArgs,
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

                    Timber.d("flag: $flag\ndate: $date\ntime: $time\nlevel: $levelText\nAfterNowIncl ${lowTideEpoch > System.currentTimeMillis()}")
                    if (flag == "low" && lowTideEpoch > System.currentTimeMillis()) {//{Utils.timeIsAfterNowInclMidnight(rawDate)) {
                        // nextLow = time - currentT

                        val millisUntilLow = lowTideEpoch - System.currentTimeMillis()

                        // if next low is less than @param userOffset -> popup msg: "Next low for $locationName is already in <hh:mm>. You will not be notified until next..." TODO
                        if (TimeUnit.MILLISECONDS.toHours(millisUntilLow) < userOffset) {
                            Toast.makeText(context, "Next low is less than $userOffset!!", Toast.LENGTH_LONG).show()
                            continue
                        }

                        val notificationTime = lowTideEpoch - TimeUnit.HOURS.toMillis(userOffset.toLong())


                        val myIntent = Intent(context, AlarmReceiver::class.java)
                        myIntent.putExtra("nextLowTideTime", time)
                        myIntent.putExtra("nextLowTideLevel", level)

                        val pendingIntent = PendingIntent.getBroadcast(context, TIDES_NOTIFICATION_OFFSET_REQUEST_CODE, myIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                        val SDK_INT = Build.VERSION.SDK_INT
                        if (SDK_INT >= Build.VERSION_CODES.M)
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
                        else if (Build.VERSION_CODES.KITKAT <= SDK_INT && SDK_INT < Build.VERSION_CODES.M)
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
                        else
                            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)

                    }

                    // if last date -> setup new job to prepareNotification for the next week
                    if (it.isLast) {
                        Timber.d("Last record: Flag: $flag\ndate: $date\ntime: $time\nlevel: $levelText")
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
