package com.solutions.grutne.flovind.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.RemoteException
import android.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng
import com.solutions.grutne.flovind.MainActivity
import com.solutions.grutne.flovind.MainActivity.Companion.HOME_LAT_KEY
import com.solutions.grutne.flovind.MainActivity.Companion.HOME_LON_KEY
import com.solutions.grutne.flovind.R
import timber.log.Timber
import java.io.IOException
import java.util.*


/**
 * Created by Adrian on 24/11/2017.
 */

object Utils {
    fun getHomeLatLong(context: Context): LatLng {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val homeLat = preferences.getString(HOME_LAT_KEY, MainActivity.DEFAULT_LAT.toString())
        val homeLon = preferences.getString(HOME_LON_KEY, MainActivity.DEFAULT_LON.toString())

        return LatLng(homeLat!!.toDouble(), homeLon!!.toDouble())
    }

    /**
     * The Geocoder is inconsistent in how it organizes various parts of an Norwegian address.
     * This is an attempt to provide meaningful place names (stedsnavn), and to extract the 'tettsted' from a given location.
     *
     * Consider using http://api.sehavniva.no/ <location name="___"
     * */

    @Throws(IOException::class, IndexOutOfBoundsException::class, RemoteException::class)
    fun getAccuratePlaceName(context: Context, latLng: LatLng): String {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            val currentAddress = addresses[0]
            var place = ""
            var area = ""
            val adminArea = addresses[0].adminArea
            val subAdminArea = addresses[0].subAdminArea

            if (isPlaceNameMeaningful(currentAddress.featureName) &&
                    !subAdminArea.isNullOrEmpty() && !subAdminArea.contains(currentAddress.featureName)) {
                place = currentAddress.featureName
                area = if (isPlaceNameMeaningful(subAdminArea)) subAdminArea else adminArea
            } else if (isPlaceNameMeaningful(currentAddress.thoroughfare)) {
                place = currentAddress.thoroughfare
                if (isPlaceNameMeaningful(subAdminArea)) area = subAdminArea else if (isPlaceNameMeaningful(adminArea)) area = adminArea
            } else if (isPlaceNameMeaningful(currentAddress.adminArea) && isPlaceNameMeaningful(currentAddress.subAdminArea)) {
                place = currentAddress.subAdminArea
                area = currentAddress.adminArea
            }
            Timber.d("getAccuratePlaceName\n$currentAddress\n$place\n$area\n$adminArea\n$subAdminArea")

            return "$place, $area"

        } catch (e: RemoteException) {
            return context.getString(R.string.address_unavailable)
        } catch (e: IndexOutOfBoundsException) {
            return context.getString(R.string.address_unavailable)
        } catch (e: IOException) {
            return context.getString(R.string.address_unavailable)
        }
    }

    private fun isPlaceNameMeaningful(placeName: String?): Boolean {
        return !placeName.isNullOrEmpty() && placeName != "null" && !placeName.matches(Regex(".*\\d.*"))
    }


    fun isGPSEnabled(mContext: Context): Boolean {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Checks internet connection status
     *
     * @param context
     * @return true if the user has a internet connection, false otherwise
     */
    fun workingConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkInfo = connectivityManager.activeNetworkInfo

        return networkInfo != null && networkInfo.isConnected
    }
}
