package com.solutions.grutne.flovind.sync

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager

import com.firebase.jobdispatcher.Constraint
import com.firebase.jobdispatcher.Driver
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.Job
import com.firebase.jobdispatcher.Lifetime
import com.firebase.jobdispatcher.RetryStrategy
import com.firebase.jobdispatcher.Trigger

import java.util.concurrent.TimeUnit

import timber.log.Timber

/**
 * Created by Adrian on 29/10/2017.
 */

object SyncUtils {
    private val SYNC_INTERVAL_HOURS = 5
    private val SYNC_INTERVAL_SECONDS = TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS.toLong()).toInt()
    private val SYNC_FLEXTIME_SECONDS = 300//SYNC_INTERVAL_SECONDS / 3

    private val TIDES_SYNC_TAG = "tides-sync"

    private var sInitialized: Boolean = false

    internal fun scheduleFirebaseJobDispatcher(context: Context) {
        val driver = GooglePlayDriver(context)
        val jobDispatcher = FirebaseJobDispatcher(driver)

        val isSyncOnlyWiFi = false // TODO setting this

        // if prefs ikke har notifik-tid, sett job til

        val syncTidesJob = jobDispatcher.newJobBuilder()
                .setService(FirebaseJobService::class.java)
                .setConstraints(if (isSyncOnlyWiFi) Constraint.ON_UNMETERED_NETWORK else Constraint.ON_ANY_NETWORK)
                .setTag(TIDES_SYNC_TAG)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(
                        SYNC_FLEXTIME_SECONDS,
                        SYNC_FLEXTIME_SECONDS + SYNC_FLEXTIME_SECONDS))
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

        scheduleFirebaseJobDispatcher(context)

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        //      if (preferences.getString("nextLowTideTime", null) == null) {
        startImmediateSync(context)
        //        }
    }

    fun startImmediateSync(context: Context) {
        Timber.d("startImmediateSync")
        val intentToSyncImmediately = Intent(context, SyncIntentService::class.java)
        context.startService(intentToSyncImmediately)
    }
}
