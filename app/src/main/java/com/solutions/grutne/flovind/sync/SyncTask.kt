package com.solutions.grutne.flovind.sync

import android.content.Context
import com.solutions.grutne.flovind.NetworkUtils
import com.statsnail.roberts.statsnail.data.DbContract


import timber.log.Timber

/**
 * Created by Adrian on 30/10/2017.
 */

object StatsnailSyncTask {
    @Synchronized
    fun syncData(context: Context, homeLocation: Boolean) {

        try {
            Timber.d("SyncData")

            val windsRequestUrl = NetworkUtils.buildWindsRequestUrl(context, homeLocation)
            val windsData = NetworkUtils.loadWindsXml(windsRequestUrl)

            val tidesRequestUrl = NetworkUtils.buildTidesRequestUrl(context, homeLocation)
            val tidesData = NetworkUtils.loadNearbyXml(context, tidesRequestUrl)

            val resolver = context.contentResolver
            if (null != tidesData && tidesData.isNotEmpty()) {
                resolver.delete(
                        DbContract.TidesEntry.CONTENT_URI_TIDES, null, null)

                resolver.bulkInsert(DbContract.TidesEntry.CONTENT_URI_TIDES, tidesData!!)
            }
            if (null != windsData && windsData.isNotEmpty()) {
                resolver.delete(
                        DbContract.TidesEntry.CONTENT_URI_WINDS, null, null
                )
                resolver.bulkInsert(DbContract.TidesEntry.CONTENT_URI_WINDS, windsData!!)
            }

        } catch (e: Exception) {
            Timber.d("failed to sync data")
            e.printStackTrace()
        }
    }
}