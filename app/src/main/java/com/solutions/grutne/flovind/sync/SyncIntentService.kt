package com.solutions.grutne.flovind.sync

import android.app.IntentService
import android.content.Intent
import com.solutions.grutne.flovind.utils.Utils

import timber.log.Timber

/**
 * Created by Adrian on 30/10/2017.
 */

class SyncIntentService : IntentService("StatsnailSyncIntentService") {
    override fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent, call syncData")
        val latLng = Utils.getHomeLatLong(this)
        FloVindSyncTask.syncHomeData(this, latLng)
    }
}
