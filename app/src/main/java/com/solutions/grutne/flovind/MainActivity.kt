package com.solutions.grutne.flovind

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.os.PersistableBundle
import android.preference.PreferenceManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View

import timber.log.Timber
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.location.*
import com.solutions.grutne.flovind.sync.SyncUtils
import com.solutions.grutne.flovind.utils.FloVindDateUtils


internal class MainActivity : AppCompatActivity() {

    /**
     * Provides the entry point to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null
    private val TAG = MainActivity::class.java.simpleName

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        // toolbar.setBackgroundColor(Color.BLUE)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        if (!checkPermissions()) {
            requestPermissions()
        } else
            getLastLocation()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putParcelable(LOCATION, mLastLocation)
        super.onSaveInstanceState(outState, outPersistentState)
    }
    private val mLocationCallback: LocationCallback? = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            if (p0?.lastLocation != null) {
                mLastLocation = p0.lastLocation

                Timber.d("onLocationResult set new last location")
            }
        }
    }

    private fun getLastLocation() {
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFusedLocationClient?.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper())
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        mFusedLocationClient!!.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLastLocation = task.result

                        SyncUtils.initialize(this@MainActivity)

                    } else {
                        showSnackBar(getString(R.string.no_location_detected))
                    }
                    if (mLastLocation == null)
                        mLastLocation = getDefaultLocation()

                    editor.putString(HOME_LAT_KEY, mLastLocation!!.latitude.toString())
                    editor.putString(HOME_LON_KEY, mLastLocation!!.longitude.toString())
                    editor.putString(ForecastFragment.EXTRA_TIDE_QUERY_DATE, FloVindDateUtils.getPersistentDateFromMillis(System.currentTimeMillis()))
                    editor.apply()

                    val tFrag = ForecastFragment.newInstance(mLastLocation!!)
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.add(R.id.frag_container, tFrag)
                    transaction.commit()
                }
    }

    // Coordinates for Oslo
    private fun getDefaultLocation(): Location {
        val location = Location("")
        location.latitude = DEFAULT_LAT
        location.longitude = DEFAULT_LON
        return location
    }

    private fun showSnackBar(mainTextStringId: Int, actionStringId: Int,
                             listener: View.OnClickListener) {

        Snackbar.make(findViewById<View>(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }

    private fun showSnackBar(text: String) {
        val container = findViewById<View>(R.id.main_activity_container)
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE)
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            showSnackBar(R.string.permission_rationale, android.R.string.ok,
                    View.OnClickListener {
                        // Request permission
                        startLocationPermissionRequest()
                    })

        } else {
            startLocationPermissionRequest()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId

        if (id == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {

            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted.
                    getLastLocation()
                }
                else -> {
                    // Permission denied.

                    // Notify the user via a SnackBar that they have rejected a core permission for the
                    // app, which makes the Activity useless. In a real app, core permissions would
                    // typically be best requested during a welcome-screen flow.

                    // Additionally, it is important to remember that a permission might have been
                    // rejected without asking the user for permission (device policy or "Never ask
                    // again" prompts). Therefore, a user interface affordance is typically implemented
                    // when permissions are denied. Otherwise, your app could appear unresponsive to
                    // touches or interactions which have required permissions.
                    showSnackBar(R.string.permission_denied_explanation, R.string.settings,
                            View.OnClickListener {
                                // Build intent that displays the App settings screen.
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null)
                                intent.data = uri
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            })
                }
            }
        }
    }

    companion object {
        // Home location is actual GPS location, primarily used for notifactions
        const val HOME_LAT_KEY = "home_lat"
        const val HOME_LON_KEY = "home_lon"
        const val LOCATION = "location"
        const val DEFAULT_LAT = 59.9139
        const val DEFAULT_LON = 10.7522
    }
}