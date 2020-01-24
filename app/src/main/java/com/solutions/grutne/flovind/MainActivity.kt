package com.solutions.grutne.flovind

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View

import timber.log.Timber
import android.support.design.widget.Snackbar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.solutions.grutne.flovind.sync.SyncUtils


internal class MainActivity : AppCompatActivity() {

    /**
     * Provides the entry point to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null
    private  val TAG = MainActivity::class.java.simpleName

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContentView(R.layout.activity_main)
        Timber.plant(Timber.DebugTree())
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
       // toolbar.setBackgroundColor(Color.BLUE)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        if (!checkPermissions()) {
            requestPermissions()
        } else
            getLastLocation()

        // if not google play:
        // val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        /*       if (mToolbar != null) {
                   setSupportActionBar(mToolbar)
                   supportActionBar!!.setDisplayShowTitleEnabled(false)
               }*/

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState!!.putParcelable(LOCATION, mLastLocation)
        super.onSaveInstanceState(outState)
    }


    private fun getLastLocation() {
        mFusedLocationClient!!.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLastLocation = task.result
                        val preferences = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                        val editor = preferences.edit()
                        // TODO  convert to doubleToRawLongBits
                        /*            editor.putString(EXTRA_LATITUDE, String.valueOf(mLastLocation.getLatitude()));
                        editor.putString(EXTRA_LONGITUDE, String.valueOf(mLastLocation.getLongitude()));*/
                        editor.putString(HOME_LAT, task.result.latitude.toString())
                        editor.putString(HOME_LON, task.result.longitude.toString())
                        editor.apply()

                        SyncUtils.initialize(this@MainActivity)

                        // for now, if location then start tidesfragment
                        val tFrag = TidesFragment.newInstance(task.result)
                        val transaction = supportFragmentManager.beginTransaction()
                        transaction.add(R.id.frag_container, tFrag)
                        transaction.commit()

                    } else {
                        showSnackBar(getString(R.string.no_location_detected))
                        // TODO show each station on the map
                    }
                }
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
                Manifest.permission.ACCESS_COARSE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE)
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)

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

            when{
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
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        // Home location is actual GPS location, primarily used for notifactions
        const val HOME_LAT = "home_lat"
        const val HOME_LON = "home_lon"
        const val LOCATION = "location"
    }
}