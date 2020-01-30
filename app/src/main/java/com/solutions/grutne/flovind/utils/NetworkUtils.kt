package com.solutions.grutne.flovind.utils


import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.solutions.grutne.flovind.BuildConfig
import com.solutions.grutne.flovind.R
//import jdk.nashorn.internal.objects.NativeDate.getTime
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
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

    const val NUMBER_OF_DAYS_TO_QUERY = 10

    fun buildTidesRequestUrl(context: Context, latLng: LatLng): String {
        val language = context.getString(R.string.language)//"en"
        val fromDate = FloVindDateUtils.getPersistentDateFromMillis(System.currentTimeMillis())
        val offset = TimeUnit.DAYS.toMillis(NUMBER_OF_DAYS_TO_QUERY.toLong())
        val tillDate = FloVindDateUtils.getPersistentDateFromMillis(System.currentTimeMillis() + offset)

        val requestUrl = "http://api.sehavniva.no/tideapi.php?lat=" +
                latLng.latitude +
                "&lon=" + //10.2795140 +
                latLng.longitude +
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
    fun buildRiseSetRequestUrl(latLng: LatLng): String {
        val fromDate = FloVindDateUtils.getPersistentDateFromMillis(System.currentTimeMillis())

        val base = " https://api.met.no/weatherapi/sunrise/2.0/"

        val queryUri = Uri.parse(base).buildUpon()
                .appendQueryParameter(PARAM_LAT, latLng.latitude.toString())
                .appendQueryParameter(PARAM_LONG, latLng.longitude.toString())
                .appendQueryParameter(PARAM_DATE, fromDate)
                .appendQueryParameter(PARAM_OFFSET, FloVindDateUtils.getZoneOffset())
                .appendQueryParameter(PARAM_DAYS, NUMBER_OF_DAYS_TO_QUERY.toString())
                .build()

        return queryUri.toString()
    }

    fun buildWindsRequestUrl(latLng: LatLng): String {
        val base = "https://api.met.no/weatherapi/locationforecast/1.9"

        val queryUri = Uri.parse(base).buildUpon()
                .appendQueryParameter(PARAM_LAT, latLng.latitude.toString())
                .appendQueryParameter(PARAM_LONG, latLng.longitude.toString())
                .build()

        return queryUri.toString()

    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun loadTidesXml(context: Context, url: String): Array<ContentValues?>? {
        var inputStream: InputStream? = null
        val parser = TidesXmlParser()
        var tidesValues: Array<ContentValues?>? = arrayOf()

        try {
            inputStream = downloadUrl(url)
            if (inputStream != null)
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
        var windsValues: Array<ContentValues?>? = arrayOf()

        try {
            inputStream = downloadUrl(url)
            if (inputStream != null)
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
        var riseSetValues: Array<ContentValues?>? = arrayOf()

        try {
            inputStream = downloadUrl(url)
            if (inputStream != null)
                riseSetValues = parser.parseRiseSets(inputStream)
        } finally {
            if (inputStream != null) inputStream.close()
        }
        return riseSetValues
    }

    // Given a string representation of a URL, sets up a connection and retrieve an InputStream
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream? {
        try {
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
        } catch (e: IOException) {
            Timber.d("failed to connect: $urlString")
            e.printStackTrace()
        }
        return null
    }
}
