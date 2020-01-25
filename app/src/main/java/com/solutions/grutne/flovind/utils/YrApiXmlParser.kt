package com.solutions.grutne.flovind.utils

import android.content.ContentValues
import android.util.Xml
import com.solutions.grutne.flovind.data.DbContract
import com.solutions.grutne.flovind.utils.NetworkUtils.NUMBER_OF_DAYS

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber

import java.io.IOException
import java.io.InputStream

class YrApiXmlParser {

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseRiseSets(inputStream: InputStream): Array<ContentValues?>? {
        inputStream.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()

//            val k:Array<ContentValues?>? = readRiseSetXml(parser)
//
//            for (x in k!!.iterator()){
//                k.get()
//            }

            return readRiseSetXml(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readRiseSetXml(parser: XmlPullParser): Array<ContentValues?>? {
        var evType = parser.eventType
        val contentValues = arrayOfNulls<ContentValues>(NUMBER_OF_DAYS.toInt() * 4)
        var name: String
        var index = 0
        while (evType != XmlPullParser.END_DOCUMENT) {
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                name = parser.name
                if ("location" == name) {
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.eventType != XmlPullParser.START_TAG)
                            continue
                        name = parser.name
                        if ("time" == name) {
                            val date = parser.getAttributeValue(parser.namespace, "date")
                            while (parser.next() != XmlPullParser.END_TAG) {
                                if (parser.eventType != XmlPullParser.START_TAG)
                                    continue
                                val riseSet = ContentValues()
                                name = parser.name

                                if (name == "sunrise" || name == "sunset" || name == "moonrise" || name == "moonset") {
                                    val time = parser.getAttributeValue(parser.namespace, "time")
                                    riseSet.put(DbContract.RiseSetEntry.COLUMN_TIME_OF_RISE_SET, time)
                                    riseSet.put(DbContract.RiseSetEntry.COLUMN_RISE_SET_DATE, date)
                                    riseSet.put(DbContract.RiseSetEntry.COLUMN_RISE_SET_TYPE, name)

                                    contentValues[index] = riseSet
                                    index++

                                }

                                parser.nextTag()
                            }
                        }
                    }
                }
            }
            evType = parser.next()
        }
        return contentValues
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseWinds(inputStream: InputStream): Array<ContentValues?>? {
        inputStream.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()

            return readWindsXml(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readWindsXml(parser: XmlPullParser): Array<ContentValues?>? {
        var evType = parser.eventType
        val windsValues = arrayOfNulls<ContentValues>(WINDS_MAX_SIZE)
        var name: String
        var index = 0
        while (evType != XmlPullParser.END_DOCUMENT) {
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                name = parser.name
                if ("time" == name) {
//                    val time = parser.getAttributeValue(1)
                    val time = parser.getAttributeValue(parser.namespace, "from")
                    var windDir: String
                    var winDirDeg: String
                    var windSpeed: String

                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.eventType != XmlPullParser.START_TAG)
                            continue
                        //<windDirection id="dd" name="SW" deg="214.3"/>
                        name = parser.name

                        if ("location" == name) {
                            //  Timber.d("location == name");
                            val wind = ContentValues()
                            while (parser.next() != XmlPullParser.END_TAG && index < WINDS_MAX_SIZE) {
                                if (parser.eventType != XmlPullParser.START_TAG)
                                    continue
                                name = parser.name
                                //      Timber.d("name: " + name);
                                if ("windDirection" == name) {
                                    windDir = parser.getAttributeValue(2)
                                    winDirDeg = parser.getAttributeValue(1)

                                    wind.put(DbContract.WindsEntry.COLUMN_WIND_DIRECTION, windDir)
                                    wind.put(DbContract.WindsEntry.COLUMN_WIND_DIR_DEG, winDirDeg)
                                }
                                if ("windSpeed" == name) {
                                    windSpeed = parser.getAttributeValue(1)
                                    wind.put(DbContract.WindsEntry.COLUMN_WIND_SPEED, windSpeed)
                                    //   Timber.d("Time: " + Utils.getFormattedTime(time) + "\nDate: " + Utils.getFormattedDate(time));
                                    wind.put(DbContract.WindsEntry.COLUMN_WINDS_DATE,
                                            Utils.getFormattedDate(time))
                                    wind.put(DbContract.WindsEntry.COLUMN_TIME_OF_WIND,
                                            Utils.getFormattedTime(time))

                                    windsValues[index] = wind
                                    index++
                                }
                                parser.nextTag() // TODO make it skip through the rest (humidity etc)
                            }
                        }
                    }
                }
            }
            evType = parser.next()
        }
        return windsValues
    }

    companion object {
        private val ns: String? = null
        private const val WINDS_MAX_SIZE = 89
    }
}
