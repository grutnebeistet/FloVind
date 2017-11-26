package com.solutions.grutne.flovind.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.statsnail.roberts.statsnail.data.DbContract


class TidesDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        val SQL_CREATE_TIDES_TABLE = "CREATE TABLE " + DbContract.TidesEntry.TABLE_TIDES + " (" +
                DbContract.TidesEntry.COLUMN_TIDES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DbContract.TidesEntry.COLUMN_TIDES_DATE + " TEXT, " +
                DbContract.TidesEntry.COLUMN_LEVEL_FLAG + " TEXT, " +
                DbContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " TEXT, " +
                DbContract.TidesEntry.COLUMN_WATER_LEVEL + " TEXT, " +
                DbContract.TidesEntry.COLUMN_TIDE_ERROR_MSG + " TEXT " +
                ");"

        val SQL_CREATE_WINDS_TABLE = "CREATE TABLE " + DbContract.TidesEntry.TABLE_WINDS + " (" +
                DbContract.TidesEntry.COLUMN_WINDS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DbContract.TidesEntry.COLUMN_WINDS_DATE + " TEXT, " +
                DbContract.TidesEntry.COLUMN_TIME_OF_WIND + " TEXT, " +
                DbContract.TidesEntry.COLUMN_WIND_DIRECTION + " TEXT, " +
                DbContract.TidesEntry.COLUMN_WIND_DIR_DEG + " TEXT, " +
                DbContract.TidesEntry.COLUMN_WIND_SPEED + " TEXT " +
                ");"

        sqLiteDatabase.execSQL(SQL_CREATE_TIDES_TABLE)
        sqLiteDatabase.execSQL(SQL_CREATE_WINDS_TABLE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DbContract.TidesEntry.TABLE_TIDES)
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DbContract.TidesEntry.TABLE_WINDS)
        onCreate(sqLiteDatabase)
    }

    companion object {

        val DATABASE_NAME = "weather.db"

        val DATABASE_VERSION = 7
    }
}
