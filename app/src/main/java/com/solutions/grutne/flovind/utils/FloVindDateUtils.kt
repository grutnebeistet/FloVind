package com.solutions.grutne.flovind.utils

import com.solutions.grutne.flovind.utils.NetworkUtils.NUMBER_OF_DAYS_TO_QUERY
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import timber.log.Timber
import java.text.ParseException
import java.util.*
import java.util.concurrent.TimeUnit

object FloVindDateUtils {

    /**
     * Get millis since epoch from a raw API String date
     *
     * @property String rawDate, format [2020-02-04T13:04:00+01:00]
     * @return Long millis since epoch
     * */
    @Throws(DateTimeParseException::class)
    fun getRawDateTimeInMillis(rawDate: String): Long {
        Timber.d("getRawDateInMillis $rawDate")
        val odt = OffsetDateTime.parse(rawDate)
        odt.atZoneSameInstant(getZone())
        val instant = odt.toInstant()

        return instant.toEpochMilli()
    }

    /**
     * Get the ms since epoch value from a SharedPrefs/DB date
     *
     * @property String persistentDate, format [2020-02-04]
     * @return Long millis since epoch
     * */
    fun getPersistentDateInMillis(persistentDate: String): Long {
        Timber.d("getPersistentDateInMillis inStr $persistentDate")
        return LocalDate.parse(persistentDate).atStartOfDay(getZone()).toEpochSecond() * 1000
    }

    /**
     * Get date for SQL and SharedPrefs
     *
     * @property Long millis since epoch
     * @return String date in format [2020-02-04]
     * */
    fun getPersistentDateFromMillis(millis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val instant = Instant.ofEpochMilli(millis)
        val zdt = ZonedDateTime.ofInstant(instant, getZone())
        return formatter.format(zdt)
    }

    /**
     * Returns displayable date from an SQL/SharedPrefs date
     *
     * @property String persistentDate, format [2020-02-04]
     * @return String pretty date [Ons 30 Juni] or [Wed, June 30] depending on locale language
     * */
    fun getPrettyDateFromPersistentDate(persistentDate: String): String {
        val persistentDateMillis = getPersistentDateInMillis(persistentDate)

        val formatter =
                if (Locale.getDefault().displayLanguage == "norsk bokmål")
                    DateTimeFormatter.ofPattern("EEE dd MMM")
                else
                    DateTimeFormatter.ofPattern("EEE, MMM dd")

        val instant = Instant.ofEpochMilli(persistentDateMillis)
        val zdt = ZonedDateTime.ofInstant(instant, getZone())

        return formatter.format(zdt)
    }

    /**
     * Get a SQL/SharedPrefs date + one day
     *
     * @property String oldDate, format 2020-09-09
     * @return String new date, format 2020-09-10
     * */
    @Throws(ParseException::class)
    fun getPersistentDatePlusOne(oldDate: String): String {
        Timber.d("getPersistentDatePlusOne $oldDate")
        val oldDateMs = getPersistentDateInMillis(oldDate)
        return getPersistentDateFromMillis(oldDateMs + TimeUnit.DAYS.toMillis(1))
    }

    /**
     * Get a SQL/SharedPrefs date minus one day
     *
     * @property String oldDate, format 2020-09-10
     * @return String new date, format 2020-09-09
     * */
    fun getPersistentDateMinusOne(oldDate: String): String {
        Timber.d("getPersistentDatePlusOne $oldDate")
        val oldDateMs = getPersistentDateInMillis(oldDate)
        return getPersistentDateFromMillis(oldDateMs - TimeUnit.DAYS.toMillis(1))
    }

    /**
     * Check if next day is the last of batch
     *
     * @property String dateString, format 2020-09-10
     * @return Boolean
     * */
    @Throws(ParseException::class)
    fun isDateLastInBatch(dateString: String, now: Long): Boolean {
        val last = now + TimeUnit.DAYS.toMillis(NUMBER_OF_DAYS_TO_QUERY - 1.toLong())
        val testDate = getPersistentDateInMillis(dateString)

        return testDate >= last
    }

    fun millisToPersistentFormat(millis: Long): String {
        val instant = Instant.ofEpochMilli(millis)
        val zdt = ZonedDateTime.ofInstant(instant, getZone())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        return formatter.format(zdt)
    }

    // Returns a formatted time string from tides APIs (hh:mm)
    @Throws(IndexOutOfBoundsException::class)
    fun getFormattedTime(rawTime: String): String {
        return rawTime.substring(11, 16)
    }

    // Returns a formatted date string from tides APIs (yyy-mm-dd)
    @Throws(IndexOutOfBoundsException::class)
    fun getFormattedDate(rawDate: String): String {
        return rawDate.substring(0, 10)
    }

    private fun getZone(): ZoneId {
        return ZoneId.of("Europe/Oslo")
    }

    fun getZoneOffset(): String {
        val instant = Instant.ofEpochMilli(System.currentTimeMillis())

        return OffsetTime.ofInstant(instant, getZone()).offset.toString()
    }
}