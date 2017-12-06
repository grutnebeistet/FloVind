package com.solutions.grutne.flovind.data

import android.net.Uri
import android.provider.BaseColumns


object DbContract {

    val CONTENT_AUTHORITY = "com.solutions.grutne.flovind.data.TidesDataProvider"

    val BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY)

    val PATH_TIDES = "tides"
    val PATH_WINDS = "winds"

    class TidesEntry : BaseColumns {
        companion object {
            var CONTENT_URI_TIDES = BASE_CONTENT_URI.buildUpon()
                    .appendPath(PATH_TIDES)
                    .build()

            val TABLE_TIDES = "tides_table"

            val TABLE_TIDES_HOME = "tides_table_home"
            val TABLE_WINDS_HOME = "winds_table_home"

            /**
             * Extend the favorites ContentProvider to store the movie poster, synopsis, user rating,
             * and release date, and display them even when offline.
             */
            val COLUMN_TIDES_ID = BaseColumns._ID
            val COLUMN_TIDES_DATE = "tide_date"
            val COLUMN_WATER_LEVEL = "water_level"
            val COLUMN_TIME_OF_LEVEL = "level_time"
            val COLUMN_LEVEL_FLAG = "level_flag"
            val COLUMN_TIDE_ERROR_MSG = "error"
        }
    }

    class WindsEntry : BaseColumns {
        companion object {
            var CONTENT_URI_WINDS = BASE_CONTENT_URI.buildUpon()
                    .appendPath(PATH_WINDS)
                    .build()

            val TABLE_WINDS = "winds_table"

            val COLUMN_WINDS_ID = BaseColumns._ID
            val COLUMN_WINDS_DATE = "wind_date"
            val COLUMN_TIME_OF_WIND = "wind_time"
            val COLUMN_WIND_DIRECTION = "wind_dir"
            val COLUMN_WIND_SPEED = "wind_speed"
            val COLUMN_WIND_DIR_DEG = "wind_dir_deg"
        }

    }
}
