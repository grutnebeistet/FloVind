package com.solutions.grutne.flovind.utils


import android.content.ContentValues
import android.content.Context
import android.icu.text.TimeZoneFormat
import android.location.Location
import android.net.Uri
import android.preference.PreferenceManager
import com.solutions.grutne.flovind.BuildConfig
import com.solutions.grutne.flovind.MainActivity
import com.solutions.grutne.flovind.R
//import jdk.nashorn.internal.objects.NativeDate.getTime
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Created by Adrian on 23/10/2017.
 */

object NetworkUtils {
    private val TIDES_BASE_URL = "http://api.sehavniva.no/tideapi.php?"
    private const val PARAM_LAT = "lat"
    private const val PARAM_LONG = "lon"
    private const val PARAM_FROM = "from"
    private const val PARAM_DATE = "date"
    private const val PARAM_DAYS = "days"
    private const val PARAM_OFFSET = "offset"
    private const val PARAM_UNTIL = "to"
    private const val TIDES_PARAM_TIME_SUFFIX = "T00%3A00"
    private const val TIDES_LANGUAGE_PREFIX = "datatype=tab&refcode=cd&place=&file=&lang"
    private const val TIDES_LANGUAGE_SUFFIX = "&interval=60&dst=0&tzone=1&tide_request=locationdata"
    internal var mLocation: Location? = null

    const val NUMBER_OF_DAYS = "14"

    fun buildTidesRequestUrl(context: Context, homeLocation: Boolean): String {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val latitude = if (homeLocation)
            preference.getString(MainActivity.HOME_LAT, "")
        else
            preference.getString(MainActivity.EXTRA_LATITUDE, "")
        Timber.d("LAT i buildTidesRequestUrl: " + latitude!!)
        val longitude = if (homeLocation)
            preference.getString(MainActivity.HOME_LON, "")
        else
            preference.getString(MainActivity.EXTRA_LONGITUDE, "")

        val language = context.getString(R.string.language)//"en" // TODO language setting
        val fromDate = Utils.getDate(System.currentTimeMillis())
        val offset = TimeUnit.DAYS.toMillis(10)
        val tillDate = Utils.getDate(System.currentTimeMillis() + offset)

        val requestUrl = "http://api.sehavniva.no/tideapi.php?lat=" +
                latitude +
                "&lon=" + //10.2795140 +
                longitude +
                "&fromtime=" +
                fromDate + "T00%3A00" +
                "&totime=" +
                tillDate +
                "T00%3A00" +
                "&datatype=tab&refcode=cd&place=&file=&lang=" + language + "&interval=60&dst=0&tzone=1&tide_request=locationdata"
        Timber.d("TidesRequest $requestUrl")
        return requestUrl
    }

    //The offset parameter is the difference from UTC, as defined in ISO 8601
    fun buildRiseSetRequestUrl(context: Context, homeLocation: Boolean): String {
        val fromDate = Utils.getDate(System.currentTimeMillis())

//        +03:00
        val offsetFromUtc = TimeUnit.MILLISECONDS.toHours((TimeZone.getDefault().getOffset(Date().time)).toLong()).toInt()
        val offset = "${TimeZoneFormat.getInstance(Locale.getDefault()).formatOffsetISO8601Extended(offsetFromUtc, false, true, true)}:00"

        val base = " https://api.met.no/weatherapi/sunrise/2.0/"

//        https://api.met.no/weatherapi/sunrise/2.0/?lat=74&lon=56&date=2018-06-24&offset=+03:00&days=3


        val preference = PreferenceManager.getDefaultSharedPreferences(context)

        val latitude = if (homeLocation)
            preference.getString(MainActivity.HOME_LAT, "")
        else
            preference.getString(MainActivity.EXTRA_LATITUDE, "")
        val longitude = if (homeLocation)
            preference.getString(MainActivity.HOME_LON, "")
        else
            preference.getString(MainActivity.EXTRA_LONGITUDE, "")

        val queryUri = Uri.parse(base).buildUpon()
                .appendQueryParameter(PARAM_LAT, latitude)
                .appendQueryParameter(PARAM_LONG, longitude)
                .appendQueryParameter(PARAM_DATE, fromDate)
                .appendQueryParameter(PARAM_OFFSET, offset)
                .appendQueryParameter(PARAM_DAYS, NUMBER_OF_DAYS)
                .build()

        return queryUri.toString()

    }

    fun buildWindsRequestUrl(context: Context, homeLocation: Boolean): String {
        val base = "https://api.met.no/weatherapi/locationforecast/1.9"
        //?lat=60.10;lon=9.58";

        val preference = PreferenceManager.getDefaultSharedPreferences(context)

        val latitude = if (homeLocation)
            preference.getString(MainActivity.HOME_LAT, "")
        else
            preference.getString(MainActivity.EXTRA_LATITUDE, "")
        Timber.d("LAT i buildTidesRequestUrl: " + latitude!!)
        val longitude = if (homeLocation)
            preference.getString(MainActivity.HOME_LON, "")
        else
            preference.getString(MainActivity.EXTRA_LONGITUDE, "")

        val queryUri = Uri.parse(base).buildUpon()
                .appendQueryParameter(PARAM_LAT, latitude)
                .appendQueryParameter(PARAM_LONG, longitude)
                .build()

        return queryUri.toString()

    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun loadTidesXml(context: Context, url: String): Array<ContentValues?>? {
        var inputStream: InputStream? = null
        val parser = TidesXmlParser()
        val tidesValues: Array<ContentValues?>?

        try {
            inputStream = downloadUrl(url)
            //tidesData = parser.parseNearbyStation(inputStream);
            tidesValues = parser.parseNearbyStation(context, inputStream)
        } finally {
            if (inputStream != null) inputStream.close()
        }
        return tidesValues
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun loadWindsXml(url: String): Array<ContentValues?>? {
        var inputStream: InputStream? = null
        val parser = YrApiXmlParser()
        val windsValues: Array<ContentValues?>?

        try {
            inputStream = downloadUrl(url)
            windsValues = parser.parseWinds(inputStream)
        } finally {
            if (inputStream != null) inputStream.close()
        }
        return windsValues
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun loadRiseSetXml(url: String): Array<ContentValues?>? {
        var inputStream: InputStream? = null
        val parser = YrApiXmlParser()
        val riseSetValues: Array<ContentValues?>?

        try {
            inputStream = downloadUrl(url)
            //tidesData = parser.parseNearbyStation(inputStream);
            riseSetValues = parser.parseRiseSets(inputStream)
        } finally {
            if (inputStream != null) inputStream.close()
        }
        return riseSetValues
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        Timber.d("Url: " + urlString)
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection

        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", BuildConfig.APPLICATION_ID)
        conn.doInput = true
        // Starts the query
        conn.connect()

        Timber.d("Response code ${conn.responseCode}")

        return conn.inputStream
    }


}
