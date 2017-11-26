package com.solutions.grutne.flovind.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import com.statsnail.roberts.statsnail.data.DbContract

import timber.log.Timber


class TidesDataProvider : ContentProvider() {

    internal var mDbHelper: TidesDbHelper? = null

    override fun onCreate(): Boolean {
        mDbHelper = TidesDbHelper(context)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selArgs: Array<String>?, sortOrder: String?): Cursor? {
        var selection = selection
        var selArgs = selArgs
        val db = mDbHelper!!.readableDatabase

        val uriMatch = mUriMatcher.match(uri)

        val returnCursor: Cursor

        when (uriMatch) {
            TIDES -> returnCursor = db.query(DbContract.TidesEntry.TABLE_TIDES, projection, selection, selArgs, null, null, sortOrder)
            WINDS -> returnCursor = db.query(DbContract.TidesEntry.TABLE_WINDS, projection, selection, selArgs, null, null, sortOrder)
            TIDES_ID -> {
                selection = DbContract.TidesEntry.COLUMN_TIDES_ID + "=?"
                selArgs = arrayOf(ContentUris.parseId(uri).toString())
                returnCursor = db.query(DbContract.TidesEntry.TABLE_TIDES, projection, selection, selArgs, null, null, sortOrder)
            }
            else -> throw IllegalArgumentException("Cannot query given uri: " + uri)
        }
        returnCursor.setNotificationUri(context!!.contentResolver, uri)
        return returnCursor
    }


    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val db = mDbHelper!!.writableDatabase
        val newRowId: Long
        val uriMatch = mUriMatcher.match(uri)
        when (uriMatch) {
            TIDES -> newRowId = db.insert(DbContract.TidesEntry.TABLE_TIDES, null, contentValues)
            WINDS -> newRowId = db.insert(DbContract.TidesEntry.TABLE_WINDS, null, contentValues)
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
        var selection = selection
        var selectionArgs = selectionArgs
        val db = mDbHelper!!.writableDatabase
        val deletedRows: Int
        when (mUriMatcher.match(uri)) {
            TIDES -> deletedRows = db.delete(DbContract.TidesEntry.TABLE_TIDES, selection, selectionArgs)
            WINDS -> deletedRows = db.delete(DbContract.TidesEntry.TABLE_WINDS, selection, selectionArgs)
            TIDES_ID -> {
                selection = DbContract.TidesEntry.COLUMN_TIDES_ID + "=?"
                selectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                deletedRows = db.delete(DbContract.TidesEntry.TABLE_TIDES, selection, selectionArgs)
            }
            else -> throw IllegalArgumentException("Failed to delete: " + uri)
        }
        if (deletedRows != 0) context!!.contentResolver.notifyChange(uri, null)
        return deletedRows
    }

    override fun update(uri: Uri, contentValues: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        var selection = selection
        var selectionArgs = selectionArgs

        // return early if there's no values to update
        if (contentValues!!.size() == 0) return 0

        val db = mDbHelper!!.writableDatabase
        val rowsUpdated: Int

        when (mUriMatcher.match(uri)) {
            TIDES -> rowsUpdated = db.update(DbContract.TidesEntry.TABLE_TIDES, contentValues, selection, selectionArgs)
            WINDS -> rowsUpdated = db.update(DbContract.TidesEntry.TABLE_WINDS, contentValues, selection, selectionArgs)
            TIDES_ID -> {
                selection = DbContract.TidesEntry.COLUMN_TIDES_ID + "=?"
                selectionArgs = arrayOf(ContentUris.parseId(uri).toString())
                rowsUpdated = db.update(DbContract.TidesEntry.TABLE_TIDES, contentValues, selection, selectionArgs)
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
                table = DbContract.TidesEntry.TABLE_WINDS
                Timber.d("bulking WINDS")
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
        private val LOG_TAG = TidesDataProvider::class.java.simpleName
        /**
         * URI matcher codes for the content URI:
         * TIDES for general table query
         * TIDES_ID for query on a specific movie
         */
        private val TIDES = 100
        private val TIDES_ID = 101
        private val WINDS = 200

        private val mUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {

            mUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, DbContract.PATH_TIDES, TIDES)

            mUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, DbContract.PATH_WINDS, WINDS)

            mUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, DbContract.PATH_TIDES + "/#", TIDES_ID)
        }
    }
}

