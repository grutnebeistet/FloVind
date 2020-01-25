package com.solutions.grutne.flovind.utils

import android.content.ContentValues
import android.content.Context
import android.preference.PreferenceManager
import android.util.Xml
import com.solutions.grutne.flovind.MainActivity
import com.solutions.grutne.flovind.ForecastFragment.Companion.EXTRA_TIDE_QUERY_DATE
import com.solutions.grutne.flovind.data.DbContract
import com.solutions.grutne.flovind.models.TidesData

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

import timber.log.Timber


/**
 * Created by Adrian on 23/10/2017.
 */

class TidesXmlParser {
    private var mContext: Context? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseNearbyStation(context: Context, `in`: InputStream): Array<ContentValues?>? {
        mContext = context
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(`in`, null)
            parser.nextTag()
            return readNearbyStationEntry(parser)
        } finally {
            `in`.close()
        }
    }


    @Throws(XmlPullParserException::class, IOException::class)
    private fun readNearbyStationEntry(parser: XmlPullParser): Array<ContentValues?>? {
        val tidesValues: Array<ContentValues?>?
        val preferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        Timber.d("readNearbyStationEntry init: parser.eventType ${parser.eventType} -- parser.name ${parser.name}")
        if (parser.name == "error") {
            if (parser.next() == XmlPullParser.TEXT) {
                tidesValues = arrayOfNulls(1)
                Timber.e("Error: " + parser.text)
                val error = ContentValues()
                error.put(DbContract.TidesEntry.COLUMN_TIDE_ERROR_MSG, parser.text)
                error.put(DbContract.TidesEntry.COLUMN_TIDES_DATE,
                        preferences.getString(EXTRA_TIDE_QUERY_DATE,
                                Utils.getDate(System.currentTimeMillis())))
                tidesValues[0] = error
                return tidesValues
            }
        }
        parser.require(XmlPullParser.START_TAG, ns, "tide")

        var info: String? = null
        var stationName: String? = null
        var stationCode: String? = null
        var latitude: String? = null
        var longitude: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            Timber.d("parser.eventType ${parser.eventType} -- parser.name ${parser.name}")
            if (parser.name == "nodata") { // TODO verify structure with the updated API
                if (parser.getAttributeName(0) == "info") {
                    tidesValues = arrayOfNulls(1)
                    val error = ContentValues()
                    error.put(DbContract.TidesEntry.COLUMN_TIDE_ERROR_MSG, parser.getAttributeValue(0))
                    error.put(DbContract.TidesEntry.COLUMN_TIDES_DATE,
                            preferences.getString(EXTRA_TIDE_QUERY_DATE,
                                    Utils.getDate(System.currentTimeMillis())))
                    tidesValues[0] = error
                    return tidesValues
                }
            }
            if (parser.name == "service") {
//                if (parser.getAttributeName(0) == "info" || parser.getAttributeName(0) == "cominfo") {
//                    tidesValues = arrayOfNulls(1)
//                    val error = ContentValues()
//                    error.put(DbContract.TidesEntry.COLUMN_TIDE_ERROR_MSG, parser.getAttributeValue(0))
//                    error.put(DbContract.TidesEntry.COLUMN_TIDES_DATE,
//                            preferences.getString(EXTRA_TIDE_QUERY_DATE,
//                                    Utils.getDate(System.currentTimeMillis())))
//                    tidesValues[0] = error
//                    return tidesValues
//                }
                parser.nextTag()
            }
            if (parser.name == "location") {
                val attributeCount = parser.attributeCount
                for (i in 0 until attributeCount) {
                    val attrName = parser.getAttributeName(i)
                    val attrValue = parser.getAttributeValue(i)
                    Timber.d("Attr Value: " + attrValue)
                    when (attrName) {
                        "name" -> stationName = attrValue
                        "code" -> stationCode = attrValue
                        "latitude" -> latitude = attrValue
                        "longitude" -> longitude = attrValue
                    }
                }
            }
        }
        // Start parsing from data tag
        val dataType: String? = null
        var waterValue: String
        var atTime: String
        var flag: String

        val waterlevels = ArrayList<TidesData.Waterlevel>()
        var levelsIndex = 0
        Timber.d(parser.next().toString() + " = next")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            var name = parser.name
            if (name == "data") {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.eventType != XmlPullParser.START_TAG)
                        continue

                    name = parser.name
                    if (name == "waterlevel") {
                        waterValue = parser.getAttributeValue(0)
                        atTime = parser.getAttributeValue(1)
                        flag = parser.getAttributeValue(2)
                        waterlevels.add(levelsIndex, TidesData.Waterlevel(waterValue, atTime, flag))

                        levelsIndex++
                    }
                    parser.nextTag()
                }

                // prepare notification only if parsed area is actual location
                // No point in checking levels beyond the first 6, regarding notification
                val homeLat = preferences.getString(
                        MainActivity.HOME_LAT,"")
                val homeLong = preferences.getString(
                        MainActivity.HOME_LON,"")

                Timber.d("Parsing hydrographiics...")
                // and only if the data being parsed is from users actual (home) location
                if (latitude != null && longitude != null &&
                        latitude.substring(0, 7) == homeLat!!.substring(0, 7)
                        && longitude.substring(0, 7) == homeLong!!.substring(0, 7))
                    NotificationUtils.prepareNotification(mContext!!.applicationContext, waterlevels.subList(0, 6))

                tidesValues = arrayOfNulls(waterlevels.size)

                for (i in waterlevels.indices) {
                    val values = ContentValues()
                    values.put(DbContract.TidesEntry.COLUMN_TIDES_DATE,
                            Utils.getFormattedDate(waterlevels[i].dateTime))
                    values.put(DbContract.TidesEntry.COLUMN_WATER_LEVEL,
                            waterlevels[i].waterValue)
                    values.put(DbContract.TidesEntry.COLUMN_LEVEL_FLAG,
                            waterlevels[i].flag)
                    values.put(DbContract.TidesEntry.COLUMN_TIME_OF_LEVEL,
                            Utils.getFormattedTime(waterlevels[i].dateTime))
                    tidesValues[i] = values
                }
                return tidesValues

            } else {
                skip(parser)
            }
        }
        return null
        //return new TidesData(stationName, stationCode, latitude, longitude, dataType, waterlevels, null);
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    companion object {
        private val ns: String? = null
    }
}
