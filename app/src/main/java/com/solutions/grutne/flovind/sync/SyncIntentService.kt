package com.solutions.grutne.flovind.sync

import android.app.IntentService
import android.content.Intent

import timber.log.Timber

/**
 * Created by Adrian on 30/10/2017.
 */

class SyncIntentService : IntentService("StatsnailSyncIntentService") {
    override fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent, call syncData")
        StatsnailSyncTask.syncData(this, true)
    }
}
