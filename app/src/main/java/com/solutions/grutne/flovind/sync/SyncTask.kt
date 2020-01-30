package com.solutions.grutne.flovind.sync

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.google.android.gms.maps.model.LatLng
import com.solutions.grutne.flovind.utils.NetworkUtils
import com.solutions.grutne.flovind.data.DbContract


import timber.log.Timber
import java.io.IOException

/**
 * Created by Adrian on 30/10/2017.
 */

object FloVindSyncTask {
    @Synchronized
    fun syncData(context: Context, latLng: LatLng) {
        syncWindData(context, latLng)
        syncTides(context,latLng)
        syncRiseSet(context, latLng)

    }
    private fun syncWindData(context: Context, latLng: LatLng) {
        val windsRequestUrl = NetworkUtils.buildWindsRequestUrl(latLng)
        val windsData = NetworkUtils.loadWindsXml(windsRequestUrl)
        try {
            val resolver = context.contentResolver
            if (null != windsData && windsData.isNotEmpty()) {
                resolver.delete(
                        DbContract.WindsEntry.CONTENT_URI_WINDS, null, null
                )
                resolver.bulkInsert(DbContract.WindsEntry.CONTENT_URI_WINDS, windsData)
            }
        } catch (e: IOException) {
            Timber.d("failed to sync wind data")
            e.printStackTrace()
        }
    }

    private fun syncTides(context: Context, latLng: LatLng) {
        val resolver = context.contentResolver
        try {
            Timber.d("SyncData")
            val tidesRequestUrl = NetworkUtils.buildTidesRequestUrl(context, latLng)
            val tidesData = NetworkUtils.loadTidesXml(context, tidesRequestUrl)

            if (null != tidesData && tidesData.isNotEmpty()) {
                resolver.delete(
                        DbContract.TidesEntry.CONTENT_URI_TIDES, null, null)

                resolver.bulkInsert(DbContract.TidesEntry.CONTENT_URI_TIDES, tidesData)
            }

            val intent = Intent("FORECAST_INSERTED")
//            intent.putExtra("IS_HOME_LOCATION", homeLocation)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        } catch (e: IOException) {
            Timber.d("failed to sync tides data")
            e.printStackTrace()
        }

    }

    private fun syncRiseSet(context: Context, latLng: LatLng) {
        val resolver = context.contentResolver
        val riseSetRequestUrl = NetworkUtils.buildRiseSetRequestUrl(latLng)
        try {
            val riseSetData = NetworkUtils.loadRiseSetXml(riseSetRequestUrl)
            if (!riseSetData.isNullOrEmpty()) {
                resolver.delete(
                        DbContract.RiseSetEntry.CONTENT_URI_RISE_SET, null, null)
                resolver.bulkInsert(DbContract.RiseSetEntry.CONTENT_URI_RISE_SET, riseSetData)
            }
        } catch (e: IOException) {
            Timber.d("failed to sync set/rise data")
            e.printStackTrace()
        }
    }
}
