package com.solutions.grutne.flovind.sync

import android.content.Context
import android.content.Intent

import com.firebase.jobdispatcher.Constraint
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.Lifetime
import com.firebase.jobdispatcher.RetryStrategy
import com.firebase.jobdispatcher.Trigger

import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by Adrian on 29/10/2017.
 */

object SyncUtils {
    private val PERIODIC_EXECUTION_SECONDS = TimeUnit.DAYS.toSeconds(2).toInt()

    private const val TIDES_SYNC_TAG = "tides-sync"

    private var sInitialized: Boolean = false

    private fun scheduleFireBaseJobDispatcher(context: Context) {
        val driver = GooglePlayDriver(context)
        val jobDispatcher = FirebaseJobDispatcher(driver)

        val isSyncOnlyWiFi = false //

        val syncTidesJob = jobDispatcher.newJobBuilder()
                .setService(FirebaseJobService::class.java)
                .setConstraints(if (isSyncOnlyWiFi) Constraint.ON_UNMETERED_NETWORK else Constraint.ON_ANY_NETWORK)
                .setTag(TIDES_SYNC_TAG)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(
                        PERIODIC_EXECUTION_SECONDS,
                        PERIODIC_EXECUTION_SECONDS + 60))
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .build()

        Timber.d("Schedule job...")
        jobDispatcher.schedule(syncTidesJob)
    }

    @Synchronized
    fun initialize(context: Context) {
        Timber.d("initialize")
        /*
         * Only perform initialization once per app lifetime. If initialization has already been
         * performed, we have nothing to do in this method.
         */
        if (sInitialized) return

        sInitialized = true

        scheduleFireBaseJobDispatcher(context)

        startImmediateSync(context)
    }

    private fun startImmediateSync(context: Context) {
        Timber.d("startImmediateSync")
        val intentToSyncImmediately = Intent(context, SyncIntentService::class.java)
        context.startService(intentToSyncImmediately)
    }
}
