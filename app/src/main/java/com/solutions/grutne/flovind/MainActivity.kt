package com.solutions.grutne.flovind

import android.Manifest
import android.app.FragmentTransaction
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View

import timber.log.Timber
import android.location.LocationManager
import android.support.design.widget.Snackbar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.solutions.grutne.flovind.sync.SyncUtils


internal class MainActivity : AppCompatActivity(), GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener, OnMapReadyCallback, {

    private val TAB_POS = "tab_post"

    //   private var mTidesFragment: TidesFragment? = null
/*    @BindView(R.id.toolbar)
    internal var mToolbar: Toolbar? = null*/
    /**
     * Provides the entry point to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    protected var mLastLocation: Location? = null
    private val TAG = MainActivity::class.java.simpleName

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContentView(R.layout.activity_main)
        Timber.plant(Timber.DebugTree())
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
        mFusedLocationClient!!.getLastLocation()
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

                        FragmentTransaction.

                    } else {
                        showSnackbar(getString(R.string.no_location_detected))
                        // TODO show each station on the map
                    }
                }
    }


    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int,
                             listener: View.OnClickListener) {

        Snackbar.make(findViewById<View>(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }

    private fun showSnackbar(text: String) {
        var container = findViewById<View>(R.id.main_activity_container)  // as view ?
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
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    View.OnClickListener {
                        // Request permission
                        startLocationPermissionRequest()
                    })

        } else {
            startLocationPermissionRequest()
        }
    }
/*

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        */
/* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater *//*

        val inflater = menuInflater
        */
/* Use the inflater's inflate method to inflate our menu layout to this menu *//*

        inflater.inflate(R.menu.menu_main, menu)
        */
/* Return true so that the menu is displayed in the Toolbar *//*

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
*/

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation()
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
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

    companion object {
        val EXTRA_LATITUDE = "latitude"
        val EXTRA_LONGITUDE = "longitude"
        // Home location is actual GPS location, primarily used for notifactions
        val HOME_LAT = "home_lat"
        val HOME_LON = "home_lon"
        val LOCATION = "location"
    }
}