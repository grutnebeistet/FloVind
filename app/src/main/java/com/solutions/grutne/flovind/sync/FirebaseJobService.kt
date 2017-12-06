package com.solutions.grutne.flovind.sync
import android.os.AsyncTask

import com.firebase.jobdispatcher.JobParameters

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
                FloVindSyncTask.syncData(context, true)
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
