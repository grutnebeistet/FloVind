package com.solutions.grutne.flovind.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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

        val SQL_CREATE_WINDS_TABLE = "CREATE TABLE " + DbContract.WindsEntry.TABLE_WINDS + " (" +
                DbContract.WindsEntry.COLUMN_WINDS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DbContract.WindsEntry.COLUMN_WINDS_DATE + " TEXT, " +
                DbContract.WindsEntry.COLUMN_TIME_OF_WIND + " TEXT, " +
                DbContract.WindsEntry.COLUMN_WIND_DIRECTION + " TEXT, " +
                DbContract.WindsEntry.COLUMN_WIND_DIR_DEG + " TEXT, " +
                DbContract.WindsEntry.COLUMN_WIND_SPEED + " TEXT " +
                ");"

        val SQL_CREATE_RISE_SET_TABLE = "CREATE TABLE ${DbContract.RiseSetEntry.TABLE_RISE_SET} (" +
                "${DbContract.RiseSetEntry.COLUMN_RISE_SET_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "${DbContract.RiseSetEntry.COLUMN_RISE_SET_TYPE} TEXT, " +
                "${DbContract.RiseSetEntry.COLUMN_RISE_SET_DATE} TEXT, " +
                "${DbContract.RiseSetEntry.COLUMN_TIME_OF_RISE_SET} TEXT " +
                ");"

        sqLiteDatabase.execSQL(SQL_CREATE_TIDES_TABLE)
        sqLiteDatabase.execSQL(SQL_CREATE_WINDS_TABLE)
        sqLiteDatabase.execSQL(SQL_CREATE_RISE_SET_TABLE)

    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DbContract.TidesEntry.TABLE_TIDES)
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DbContract.WindsEntry.TABLE_WINDS)
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DbContract.RiseSetEntry.TABLE_RISE_SET)
        onCreate(sqLiteDatabase)
    }

    companion object {

        const val DATABASE_NAME = "weather.db"

        const val DATABASE_VERSION = 8
    }
}
