package com.solutions.grutne.flovind.utils

import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.net.Uri
import android.preference.PreferenceManager
import com.solutions.grutne.flovind.MainActivity
import com.solutions.grutne.flovind.R


import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

import timber.log.Timber

/**
 * Created by Adrian on 23/10/2017.
 */

object NetworkUtils {
    private val TIDES_BASE_URL = "http://api.sehavniva.no/tideapi.php?"
    private val TIDES_PARAM_LAT = "lat"
    private val TIDES_PARAM_LONG = "lon"
    private val TIDES_PARAM_FROM = "from"
    private val TIDES_PARAM_UNTIL = "to"
    private val TIDES_PARAM_TIME_SUFFIX = "T00%3A00"
    private val TIDES_LANGUAGE_PREFIX = "datatype=tab&refcode=cd&place=&file=&lang"
    private val TIDES_LANGUAGE_SUFFIX = "&interval=60&dst=0&tzone=1&tide_request=locationdata"
    internal var mLocation: Location? = null

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

        val requestUrl = "http://api.sehavniva.no/tideapi.php?lat=" + //63.4581662 +

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


    fun buildSunRequestUrl(context: Context, homeLocation: Boolean): String {
        val fromDate = Utils.getDate(System.currentTimeMillis())
        val offset = TimeUnit.DAYS.toMillis(10)
        val tillDate = Utils.getDate(System.currentTimeMillis() + offset)
        val base = "https://beta.api.met.no/weatherapi/sunrise/1.1/"
        val date = "&from=$fromDate&to=$tillDate"

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
                .appendQueryParameter(TIDES_PARAM_LAT, latitude)
                .appendQueryParameter(TIDES_PARAM_LONG, longitude)
                .appendQueryParameter(TIDES_PARAM_FROM, fromDate)
                .appendQueryParameter(TIDES_PARAM_UNTIL, tillDate)
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
                .appendQueryParameter(TIDES_PARAM_LAT, latitude)
                .appendQueryParameter(TIDES_PARAM_LONG, longitude)
                .build()

        return queryUri.toString()

    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun loadNearbyXml(context: Context, url: String): Array<ContentValues?>? {
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
        conn.doInput = true
        // Starts the query
        conn.connect()

        Timber.d("Response code ${conn.responseCode}")

        return conn.inputStream
    }


}
