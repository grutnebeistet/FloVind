package com.solutions.grutne.flovind.sync

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.solutions.grutne.flovind.utils.NetworkUtils
import com.solutions.grutne.flovind.data.DbContract


import timber.log.Timber

/**
 * Created by Adrian on 30/10/2017.
 */

object FloVindSyncTask {
    @Synchronized
    fun syncData(context: Context, homeLocation: Boolean) {

        try {
            Timber.d("SyncData")

            val windsRequestUrl = NetworkUtils.buildWindsRequestUrl(context, homeLocation)
            val windsData = NetworkUtils.loadWindsXml(windsRequestUrl)

            val tidesRequestUrl = NetworkUtils.buildTidesRequestUrl(context, homeLocation)
            val tidesData = NetworkUtils.loadTidesXml(context, tidesRequestUrl)

            val riseSetRequestUrl = NetworkUtils.buildRiseSetRequestUrl(context, homeLocation)
            val riseSetData = NetworkUtils.loadRiseSetXml(riseSetRequestUrl)

            val resolver = context.contentResolver
            if (null != tidesData && tidesData.isNotEmpty()) {
                resolver.delete(
                        DbContract.TidesEntry.CONTENT_URI_TIDES, null, null)

                resolver.bulkInsert(DbContract.TidesEntry.CONTENT_URI_TIDES, tidesData)
            }
            if (null != windsData && windsData.isNotEmpty()) {
                resolver.delete(
                        DbContract.WindsEntry.CONTENT_URI_WINDS, null, null
                )
                resolver.bulkInsert(DbContract.WindsEntry.CONTENT_URI_WINDS, windsData)
            }
            if (!riseSetData.isNullOrEmpty()) {
                resolver.delete(
                        DbContract.RiseSetEntry.CONTENT_URI_RISE_SET, null, null)
                resolver.bulkInsert(DbContract.RiseSetEntry.CONTENT_URI_RISE_SET, riseSetData)
            }

            val intent = Intent("FORECAST_INSERTED")
            intent.putExtra("IS_HOME_LOCATION", homeLocation)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        } catch (e: Exception) {
            Timber.d("failed to sync data")
            e.printStackTrace()
        }
    }
}
