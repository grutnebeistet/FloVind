package com.solutions.grutne.flovind.data

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import timber.log.Timber


class WeatherDataProvider : ContentProvider() {

    private var mDbHelper: TidesDbHelper? = null

    override fun onCreate(): Boolean {
        mDbHelper = TidesDbHelper(context)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selArgs: Array<String>?, sortOrder: String?): Cursor? {
        val db = mDbHelper!!.readableDatabase

        val uriMatch = mUriMatcher.match(uri)

        val returnCursor: Cursor

        returnCursor = when (uriMatch) {
            TIDES -> db.query(DbContract.TidesEntry.TABLE_TIDES, projection, selection, selArgs, null, null, sortOrder)
            WINDS -> db.query(DbContract.WindsEntry.TABLE_WINDS, projection, selection, selArgs, null, null, sortOrder)
            TIDES_ID -> {
                val idSelection = DbContract.TidesEntry.COLUMN_TIDES_ID + "=?"
                val idSelectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                db.query(DbContract.TidesEntry.TABLE_TIDES, projection, idSelection, idSelectionArgs, null, null, sortOrder)
            }
            RISE_SETS -> db.query(DbContract.RiseSetEntry.TABLE_RISE_SET, projection, selection, selArgs, null, null, sortOrder)

            else -> throw IllegalArgumentException("Cannot query given uri: " + uri)
        }
        returnCursor.setNotificationUri(context!!.contentResolver, uri)
        return returnCursor
    }


    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val db = mDbHelper!!.writableDatabase
        val newRowId: Long
//        val uriMatch = mUriMatcher.match(uri)
        newRowId = when (mUriMatcher.match(uri)) {
            TIDES -> db.insert(DbContract.TidesEntry.TABLE_TIDES, null, contentValues)
            WINDS -> db.insert(DbContract.WindsEntry.TABLE_WINDS, null, contentValues)
            RISE_SETS -> db.insert(DbContract.RiseSetEntry.TABLE_RISE_SET, null, contentValues)
            else -> throw IllegalArgumentException("Cannot insert for given uri: " + uri)
        }
        if (newRowId == -1L) {
            Log.e(LOG_TAG, "insertion failed for " + uri)
            return null
        }
        // Return the Uri for the newly added Movie
        return ContentUris.withAppendedId(uri, newRowId)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = mDbHelper!!.writableDatabase
        val deletedRows: Int
        deletedRows = when (mUriMatcher.match(uri)) {
            TIDES -> db.delete(DbContract.TidesEntry.TABLE_TIDES, selection, selectionArgs)
            WINDS -> db.delete(DbContract.WindsEntry.TABLE_WINDS, selection, selectionArgs)
            TIDES_ID -> {
                val idSelection = DbContract.TidesEntry.COLUMN_TIDES_ID + "=?"
                val idSelectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                db.delete(DbContract.TidesEntry.TABLE_TIDES, idSelection, idSelectionArgs)
            }
            RISE_SETS -> db.delete(DbContract.RiseSetEntry.TABLE_RISE_SET, selection, selectionArgs)
            else -> throw IllegalArgumentException("Failed to delete: " + uri)
        }
        if (deletedRows != 0) context!!.contentResolver.notifyChange(uri, null)
        return deletedRows
    }

    override fun update(uri: Uri, contentValues: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        // return early if there's no values to update
        if (contentValues!!.size() == 0) return 0

        val db = mDbHelper!!.writableDatabase
        val rowsUpdated: Int

        rowsUpdated = when (mUriMatcher.match(uri)) {
            TIDES -> db.update(DbContract.TidesEntry.TABLE_TIDES, contentValues, selection, selectionArgs)
            WINDS -> db.update(DbContract.WindsEntry.TABLE_WINDS, contentValues, selection, selectionArgs)
            RISE_SETS -> db.update(DbContract.RiseSetEntry.TABLE_RISE_SET, contentValues, selection, selectionArgs)
            TIDES_ID -> {
                val idSelection = DbContract.TidesEntry.COLUMN_TIDES_ID + "=?"
                val idSelectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                db.update(DbContract.TidesEntry.TABLE_TIDES, contentValues, idSelection, idSelectionArgs)
            }
            else -> throw IllegalArgumentException("Cannot update for given uri " + uri)
        }// Log.i(LOG_TAG, "updated movie");
        //  if (rowsUpdated != 0) getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val db = mDbHelper!!.writableDatabase

        val table: String
        when (mUriMatcher.match(uri)) {

            TIDES -> {
                table = DbContract.TidesEntry.TABLE_TIDES
                Timber.d("bulking TIDES")
            }
            WINDS -> {
                table = DbContract.WindsEntry.TABLE_WINDS
                Timber.d("bulking WINDS")
            }
            RISE_SETS ->{
                table = DbContract.RiseSetEntry.TABLE_RISE_SET
            }
            else -> return super.bulkInsert(uri, values)
        }
        db.beginTransaction()
        var rowsInserted = 0
        try {
            for (value in values) {
                var _id: Long = -1
                if (value != null)
                    _id = db.insert(table, null, value)
                if (_id != -1L) {
                    rowsInserted++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        if (rowsInserted > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }
        Log.i(LOG_TAG, "inserted: $rowsInserted rows")
        return rowsInserted
    }


    override fun getType(uri: Uri): String? {
        val match = mUriMatcher.match(uri)
        return "TODO"
    }

    companion object {
        private val LOG_TAG = WeatherDataProvider::class.java.simpleName
        /**
         * URI matcher codes for the content URI:
         * TIDES for general table query
         * TIDES_ID for query on a specific movie
         */
        private const val TIDES = 100
        private const val TIDES_ID = 101
        private const val WINDS = 200
        private const val RISE_SETS = 300

        private val mUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            mUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, DbContract.PATH_TIDES, TIDES)

            mUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, DbContract.PATH_WINDS, WINDS)

            mUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, DbContract.PATH_RISE_SET, RISE_SETS)

            mUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, DbContract.PATH_TIDES + "/#", TIDES_ID)
        }
    }
}

