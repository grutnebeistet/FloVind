package com.solutions.grutne.flovind.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng
import com.solutions.grutne.flovind.MainActivity.Companion.EXTRA_LATITUDE
import com.solutions.grutne.flovind.MainActivity.Companion.EXTRA_LONGITUDE
import com.solutions.grutne.flovind.MainActivity.Companion.HOME_LAT
import com.solutions.grutne.flovind.MainActivity.Companion.HOME_LON
import timber.log.Timber
import java.io.IOException
import java.lang.Double
import java.text.ParseException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Adrian on 24/11/2017.
 */

object Utils {

    // Returns current time in format hh:mm
    val time: String
        get() {
            val currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val minutes = Calendar.getInstance().get(Calendar.MINUTE)
            return currentHours.toString() + ":" + minutes
        }

    // returns millisec from string date
    @Throws(ParseException::class)
    fun getDateInMillisec(dateString: String): Long {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(dateString)
        return date.time
    }


    // returns a date string of the day after param
    @Throws(ParseException::class)
    fun getDatePlusOne(oldDate: String): String {
        //        Timber.d("Date in: " + oldDate + ", plus " + TimeUnit.DAYS.toMillis(1) + ", return: " +
        //                getDate(getDateInMillisec(oldDate) + TimeUnit.DAYS.toMillis(1)));
        return getDate(getDateInMillisec(oldDate) + TimeUnit.DAYS.toMillis(1))
    }

    // returns a date string of the day before param
    @Throws(ParseException::class)
    fun getDateMinusOne(oldDate: String): String {
        return getDate(getDateInMillisec(oldDate) - TimeUnit.DAYS.toMillis(1))
    }

    // Returns a date string in yyyy-MM-dd from millisecs
    fun getDate(millis: Long): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = Date(millis)
        return dateFormat.format(date)

    }

    // Returns a time string in hh:mm from millisecs
    fun getTime(millis: Long): String {
        val dateFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Date(millis)

        return dateFormat.format(date)

    }

    // Returns a date string in EEE,MMM dd from millisec
    fun getPrettyDate(millis: Long): String {
        val dateFormat =
                if (Locale.getDefault().displayLanguage == "norsk bokmål")
                    java.text.SimpleDateFormat("EEE dd MMM ", Locale.getDefault())
                else
                    java.text.SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val date = Date(millis)

        return dateFormat.format(date).substring(0,1).toUpperCase() + dateFormat.format(date).substring(1)
    }

    // Returns true if the whole hour of time given (next low tide time) is after current time
    fun timeIsAfterNowInclMidnight(time: String): Boolean {
        //2017-11-12T20:24:00+01:00
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        val year = Integer.valueOf(time.substring(0, 4))
        val month = Integer.valueOf(time.substring(5, 7))
        val day = Integer.valueOf(time.substring(8, 10))

        if (year < currentYear) return false
        if (month < currentMonth) return false
        if (day < currentDay) return false

        val lowTideHours = Integer.valueOf(time.substring(11, 13))
        val currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val notifyOnNext = lowTideHours >= currentHours || day > currentDay || month > currentMonth || year > currentYear
        Timber.d("notify on next: " + notifyOnNext)
        return notifyOnNext
    }

    // Returns true if the whole hour of time given (next low tide time) is after current time
    fun timeIsAfterNow(time: String): Boolean {
        val lowTideHours = Integer.valueOf(time.substring(0, 2))
        val currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val minutes = Calendar.getInstance().get(Calendar.MINUTE)

        return lowTideHours >= currentHours
    }

    // returns true if tomorrow is last day of forecast
    @Throws(ParseException::class)
    fun isTomorrowLast(dateString: String): Boolean {
        val now = System.currentTimeMillis()
        val last = now + TimeUnit.DAYS.toMillis(10)
        val testDate = getDateInMillisec(dateString)

        return testDate + TimeUnit.DAYS.toMillis(1) >= last
    }

    // Returns a String of remaining time in hours and/or minutes given a time in millisec
    fun getRemainingTime(rawTime: Long): String {
        val millisLeft = rawTime - System.currentTimeMillis()
        val hoursLeft = TimeUnit.MILLISECONDS.toHours(millisLeft)
        val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(millisLeft - TimeUnit.HOURS.toMillis(hoursLeft))
        return if (hoursLeft > 0) hoursLeft.toString() + "h " + minutesLeft + "m " else minutesLeft.toString() + " minutes"
        //(hoursLeft > 1 ? hoursLeft +  "hours" + )
    }

    // Returns a formatted time string from tides APIs (hh:mm)
    fun getFormattedTime(rawTime: String): String {
        return rawTime.substring(11, 16)
    }

    // Returns a formatted date string from tides APIs (yyy-mm-dd)
    fun getFormattedDate(rawDate: String): String {
        return rawDate.substring(0, 10)
    }

    @Throws(IOException::class, IndexOutOfBoundsException::class)
    fun getAccuratePlaceName(context: Context, latLng: LatLng): String {

        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        var place = ""
        try {
            val address = addresses[0].getAddressLine(0)
            val subAdminArea = addresses[0].subAdminArea

            val sb = StringBuilder()

            if (address.substring(0, 7) == "Unnamed" ||
                    address.substring(0, 2) == "Fv" ||
                    address.substring(0, 2) == "E1" ||
                    address.substring(0, 2) == "E6" ||
                    address.substring(0, 2) == "Rv")
                return getPlaceName(addresses)
            for (c in address.toCharArray()) {
                if (Character.isLetter(c) || Character.isSpaceChar(c))
                    sb.append(c)
                else
                    break
            }
            val possibleSpaceIndex = sb.length - 1
            if (sb.length > 0 && Character.isSpaceChar(sb[possibleSpaceIndex]))
                sb.deleteCharAt(possibleSpaceIndex)

            place = sb.append(", ").append(subAdminArea).toString()
        } catch (ioe: IndexOutOfBoundsException) {
            ioe.printStackTrace()
        }

        return place
    }

    @Throws(IOException::class, IndexOutOfBoundsException::class)
    fun getAccuratePlaceName(context: Context, homeLocation: Boolean): String {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)

        val latitude = if (homeLocation)
            preference.getString(HOME_LAT, "")
        else
            preference.getString(EXTRA_LATITUDE, "")

        val longitude = if (homeLocation)
            preference.getString(HOME_LON, "")
        else
            preference.getString(EXTRA_LONGITUDE, "")
        val latLng = LatLng(Double.valueOf(latitude)!!, Double.valueOf(longitude)!!)
        return getAccuratePlaceName(context, latLng)
    }

    private fun getPlaceName(addresses: List<Address>): String {
        if (addresses.isNotEmpty()) {
            val subAdmin = addresses[0].subAdminArea
            val adminArea = addresses[0].adminArea

            return if (subAdmin != null && adminArea != null)
                subAdmin + ", " + adminArea
            else if (subAdmin == null && adminArea != null)
                adminArea
            else if (subAdmin != null && adminArea == null) subAdmin else "Location unavailable"

        }
        return "Location unavailable"
    }

    @Throws(IOException::class)
    fun getPlaceName(context: Context, latLng: LatLng): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude,
                latLng.longitude, 1)

        if (addresses.size != 0) {
            val subAdmin = addresses[0].subAdminArea
            val adminArea = addresses[0].adminArea

            return if (subAdmin != null && adminArea != null)
                subAdmin + ", " + adminArea
            else if (subAdmin == null && adminArea != null)
                adminArea
            else if (subAdmin != null && adminArea == null) subAdmin else "Location unavailable"

        }
        return "Location unavailable"
    }

    @Throws(IOException::class)
    fun getPlaceName(context: Context, homeLocation: Boolean): String {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)

        val latitude = if (homeLocation)
            preference.getString(HOME_LAT, "")
        else
            preference.getString(EXTRA_LATITUDE, "")
        val longitude = if (homeLocation)
            preference.getString(HOME_LON, "")
        else
            preference.getString(EXTRA_LONGITUDE, "")
        val latLng = LatLng(Double.valueOf(latitude)!!, Double.valueOf(longitude)!!)
        return getPlaceName(context, latLng)
    }

    fun isGPSEnabled(mContext: Context): Boolean {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Checks internet connection status
     *
     * @param context
     * @return true if the user has a internet connection, false otherwise
     */
    fun workingConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkInfo = connectivityManager.activeNetworkInfo

        return networkInfo != null && networkInfo.isConnected
    }
}
