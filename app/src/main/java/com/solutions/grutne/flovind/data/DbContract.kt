package com.solutions.grutne.flovind.data

import android.net.Uri
import android.provider.BaseColumns


object DbContract {

    const val CONTENT_AUTHORITY = "com.solutions.grutne.flovind.data.TidesDataProvider"

    val BASE_CONTENT_URI = Uri.parse("content://$CONTENT_AUTHORITY")

    const val PATH_TIDES = "path_tides"
    const val PATH_TIDES_HOME = "path_tides_home"
    const val PATH_WINDS = "path_winds"
    const val PATH_RISE_SET ="path_rise_set"


    class TidesEntry : BaseColumns {
        companion object {
            var CONTENT_URI_TIDES = BASE_CONTENT_URI.buildUpon()
                    .appendPath(PATH_TIDES)
                    .build()
            var CONTENT_URI_TIDES_HOME = BASE_CONTENT_URI.buildUpon()
                    .appendPath(PATH_TIDES_HOME)
                    .build()

            const val TABLE_TIDES = "tides_table"
            const val TABLE_TIDES_HOME = "tides_table_home"

            const val COLUMN_TIDES_ID = BaseColumns._ID
            const val COLUMN_TIDES_DATE = "tide_date"
            const val COLUMN_WATER_LEVEL = "water_level"
            const val COLUMN_TIME_OF_LEVEL = "level_time"
            const val COLUMN_LEVEL_FLAG = "level_flag"
            const val COLUMN_TIDE_ERROR_MSG = "error"
            const val COLUMN_TIDES_DATE_RAW = "tide_date_raw"
        }
    }

    class WindsEntry : BaseColumns {
        companion object {
            var CONTENT_URI_WINDS = BASE_CONTENT_URI.buildUpon()
                    .appendPath(PATH_WINDS)
                    .build()

            const val TABLE_WINDS = "winds_table"

            const val COLUMN_WINDS_ID = BaseColumns._ID
            const val COLUMN_WINDS_DATE = "wind_date"
            const val COLUMN_TIME_OF_WIND = "wind_time"
            const val COLUMN_WIND_DIRECTION = "wind_dir"
            const val COLUMN_WIND_SPEED = "wind_speed"
            const val COLUMN_WIND_DIR_DEG = "wind_dir_deg"
        }
    }

    class RiseSetEntry : BaseColumns {
        companion object {
            var CONTENT_URI_RISE_SET = BASE_CONTENT_URI.buildUpon()
                    .appendPath(PATH_RISE_SET)
                    .build()

            const val TABLE_RISE_SET = "rise_set_table"

            const val COLUMN_RISE_SET_ID = BaseColumns._ID
            const val COLUMN_RISE_SET_TYPE = "rise_set_type"
            const val COLUMN_RISE_SET_DATE = "rise_set_date"
            const val COLUMN_TIME_OF_RISE_SET = "rise_set_time"
        }
    }
}
