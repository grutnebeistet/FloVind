package com.solutions.grutne.flovind.utils

import android.content.ContentValues
import android.util.Xml
import com.solutions.grutne.flovind.data.DbContract

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.io.InputStream

class YrApiXmlParser {

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseWinds(`in`: InputStream): Array<ContentValues?>? {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(`in`, null)
            parser.nextTag()

            return readWindsXml(parser)

        } finally {
            `in`.close()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readWindsXml(parser: XmlPullParser): Array<ContentValues?>? {
        var evType = parser.eventType
        val windsValues = arrayOfNulls<ContentValues>(WINDS_MAX_SIZE)
        var name = ""
        var index = 0
        while (evType != XmlPullParser.END_DOCUMENT) {
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                name = parser.name
                if ("time" == name) {
                    val time = parser.getAttributeValue(1)

                    var windDir = ""
                    var winDirDeg = ""
                    var windSpeed = ""

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
            //Timber.d("YrParse get name: " + parser.getName());
            evType = parser.next()
        }
        return windsValues
    }

    companion object {
        private val ns: String? = null
        private val WINDS_MAX_SIZE = 89
    }
}
