package com.solutions.grutne.flovind.sync

import android.os.AsyncTask
import android.preference.PreferenceManager

import com.firebase.jobdispatcher.JobParameters
import com.google.android.gms.maps.model.LatLng
import com.solutions.grutne.flovind.MainActivity
import com.solutions.grutne.flovind.utils.Utils

import timber.log.Timber

/**
 * Created by Adrian on 28/10/2017.
 */

class FirebaseJobService : com.firebase.jobdispatcher.JobService() {

    override fun onStartJob(job: JobParameters): Boolean {
        mFetchTidesDataTask = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void?): Void? {
                Timber.d("job, do in BG")
                val context = applicationContext

                val latLng = Utils.getHomeLatLong(context)
                FloVindSyncTask.syncHomeData(context, latLng)
                // NotificationUtils.notifyOfLowTideTest(context, " doInBG");
                jobFinished(job, true)
                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                jobFinished(job, true)
            }
        }
        mFetchTidesDataTask!!.execute()
        return true
    }


    override fun onStopJob(job: JobParameters): Boolean {
        if (mFetchTidesDataTask != null) {
            mFetchTidesDataTask!!.cancel(true)
        }
        return true
    }

    companion object {
        private var mFetchTidesDataTask: AsyncTask<Void, Void, Void>? = null
    }
}
