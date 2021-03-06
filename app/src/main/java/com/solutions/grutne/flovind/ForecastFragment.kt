package com.solutions.grutne.flovind


import android.content.*
import android.database.Cursor
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.solutions.grutne.flovind.adapters.TidesDataAdapter
import com.solutions.grutne.flovind.adapters.WindsDataAdapter
import com.solutions.grutne.flovind.data.DbContract
import com.solutions.grutne.flovind.sync.FloVindSyncTask.syncData
import com.solutions.grutne.flovind.utils.FloVindDateUtils
import com.solutions.grutne.flovind.utils.Utils
import kotlinx.android.synthetic.main.fragment_forecast.view.*
import kotlinx.android.synthetic.main.sun_rise_set.view.*
import timber.log.Timber
import java.io.IOException
import java.text.ParseException


/**
 * Created by Adrian on 24/10/2017.
 */

class ForecastFragment : Fragment(),
        LoaderManager.LoaderCallbacks<Cursor>,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback {
    private lateinit var mTidesRecyclerView: RecyclerView
    private lateinit var mWindsRecyclerView: RecyclerView
    private lateinit var mLocationTextView: TextView
    private lateinit var mDateTimeTextView: TextView
    private lateinit var mErrorTextView: TextView
    private lateinit var mNextDay: RelativeLayout
    private lateinit var mPrevDay: RelativeLayout
    private lateinit var mContainer: CardView


    private var mCurrentMarker: Marker? = null

    private var mMap: GoogleMap? = null
    private var mLocationButton: View? = null
    // Stores the ID of the currently selected style, so that we can re-apply it when
    // the activity restores state, for example when the device changes orientation.
    private val mSelectedStyleId = R.string.style_label_default

    // These are simply the string resource IDs for each of the style names. We use them
    // as identifiers when choosing which style to apply.
    private val mStyleIds = intArrayOf(R.string.style_label_retro, R.string.style_label_night, R.string.style_label_grayscale, R.string.style_label_no_pois_no_transit, R.string.style_label_default)
    private var mLocation: Location? = null
    private var mTidesAdapter: TidesDataAdapter? = null
    private var mWindsAdapter: WindsDataAdapter? = null
    private var mMapZoom = MAP_ZOOM_DEFAULT
    private var mPreferences: SharedPreferences? = null
    private var mVisibility = View.VISIBLE

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) { // Get extra data included in the Intent
            Timber.d("onReceive ${intent.action}")
            when (intent.action) {
                "FORECAST_INSERTED" -> {
                    Timber.d("FORECAST_INSERTED")
                    restartLoaders()//intent.getBooleanExtra("IS_HOME_LOCATION", true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        initLocation()

        mTidesAdapter = TidesDataAdapter(context!!)
        mWindsAdapter = WindsDataAdapter(context!!)

    }

    private fun initLocation() {
        try {
            mLocation = arguments!!.getParcelable(LOCATION)
            LAT_LNG_HOME = LatLng(mLocation!!.latitude, mLocation!!.longitude)
            LAT_LNG_FORECAST = LAT_LNG_HOME
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Timber.d("onCreateView")
        val view = inflater.inflate(R.layout.fragment_forecast, container, false)
        mLocationButton = (view.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById(Integer.parseInt("2"))

        mContainer = view.findViewById(R.id.cardview_container) as CardView
        mDateTimeTextView = view.findViewById(R.id.forecast_date) as TextView
        mErrorTextView = view.findViewById(R.id.tides_error_tv) as TextView
        mLocationTextView = view.findViewById(R.id.location_name) as TextView
        mNextDay = view.findViewById(R.id.next_day_button) as RelativeLayout
        mPrevDay = view.findViewById(R.id.prev_day_button) as RelativeLayout

        mTidesRecyclerView = view.findViewById<RecyclerView>(R.id.tides_recycler_view) as RecyclerView
        mTidesRecyclerView.layoutManager = LinearLayoutManager(activity)
        mTidesRecyclerView.adapter = mTidesAdapter

        mWindsRecyclerView = view.findViewById<RecyclerView>(R.id.winds_recycler_view) as RecyclerView
        mWindsRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        mWindsRecyclerView.adapter = mWindsAdapter

        return view
    }

    private val onNextDayClick = View.OnClickListener {
        Timber.e("nextDayOnClick")
        try {
            val tomorrowPersistentDate = FloVindDateUtils.getPersistentDatePlusOne(currentPersistentDate!!)
            mPreferences!!.edit().putString(EXTRA_TIDE_QUERY_DATE, tomorrowPersistentDate).apply()
            val prettyDate = FloVindDateUtils.getPrettyDateFromPersistentDate(tomorrowPersistentDate)
            mDateTimeTextView.text = prettyDate

            restartLoaders()
        } catch (e: ParseException) {
            Timber.e("failed to increase date")
            e.printStackTrace()
        }

        mPrevDay.visibility = View.VISIBLE
    }
    private val onPrevDayClick = View.OnClickListener {
        Timber.e("prevDayOnClick")
        try {
            val yesterdayPersistentDate = FloVindDateUtils.getPersistentDateMinusOne(currentPersistentDate!!)
            mPreferences!!.edit().putString(EXTRA_TIDE_QUERY_DATE, yesterdayPersistentDate).apply()

            val prettyDate = FloVindDateUtils.getPrettyDateFromPersistentDate(yesterdayPersistentDate)
            mDateTimeTextView.text = prettyDate

            restartLoaders()
        } catch (e: ParseException) {
            Timber.e("failed to decrease date")
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (savedInstanceState != null) {
            Timber.d("retrievingsavedstate map zoom $mMapZoom")
//            mMapZoom = savedInstanceState.getFloat(MAP_ZOOM)
        }

        mNextDay.setOnClickListener(onNextDayClick)
        mPrevDay.setOnClickListener(onPrevDayClick)

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        Timber.d("onViewStateRestored")
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        mMapZoom = mPreferences?.getString(MAP_ZOOM, mMapZoom.toString())!!.toFloat()

        if (!Utils.workingConnection(context!!)) { // TODO and adapter empty for day btns?
            mNextDay.visibility = View.INVISIBLE
            mPrevDay.visibility = View.INVISIBLE
            showSnackBar(getString(R.string.connection_error))
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction("FORECAST_INSERTED")
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMessageReceiver, intentFilter)

        if (mMap != null) {
            onMapReady(mMap)
        }
    }

    override fun onPause() {
        Timber.d("onPause")
        super.onPause()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(mMessageReceiver)
        mPreferences?.edit()?.putString(MAP_ZOOM, mMap!!.cameraPosition.zoom.toString())?.apply()
    }

    private fun showSnackBar(text: String) {
        val container = activity!!.findViewById<View>(R.id.tides_content)
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private val currentPersistentDate: String?
        get() = mPreferences!!.getString(EXTRA_TIDE_QUERY_DATE,
                FloVindDateUtils.millisToPersistentFormat(System.currentTimeMillis()))

    private fun restartLoaders() {
        try {
            mLocationTextView.text = (Utils.getAccuratePlaceName(context!!, LAT_LNG_FORECAST!!))

            val prettyDate = FloVindDateUtils.getPrettyDateFromPersistentDate(currentPersistentDate!!)

            mDateTimeTextView.text = prettyDate

        } catch (e: IOException) {
            e.printStackTrace()
            mErrorTextView.setText(R.string.error_unknown)
        } catch (e: NullPointerException) {
            e.printStackTrace()
            mErrorTextView.setText(R.string.error_unknown)
        } catch (e: ParseException) {
            e.printStackTrace()
            mErrorTextView.setText(R.string.error_unknown)
        }

        Timber.d("LAT_LNG_FORECAST == LAT_LNG_HOME ${LAT_LNG_FORECAST == LAT_LNG_HOME}")
        if (LAT_LNG_FORECAST == LAT_LNG_HOME)
            activity!!.supportLoaderManager.restartLoader(LOADER_ID_TIDES_HOME, null, this)
        else activity!!.supportLoaderManager.restartLoader(LOADER_ID_TIDES, null, this)

//         activity!!.supportLoaderManager.restartLoader(LOADER_ID_TIDES, null, this)

        activity!!.supportLoaderManager.restartLoader(LOADER_ID_WINDS, null, this)
        activity!!.supportLoaderManager.restartLoader(LOADER_ID_RISE_SET, null, this)
    }


    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        Timber.d("onCr Loader $i")
        val sortOrder: String
        val selection: String

        val selectionArgs = arrayOf(currentPersistentDate)

        when (i) {
            LOADER_ID_RISE_SET -> {
                sortOrder = DbContract.RiseSetEntry.COLUMN_RISE_SET_TYPE + " DESC"
                selection = DbContract.RiseSetEntry.COLUMN_RISE_SET_DATE + "=?"
                return CursorLoader(activity!!, DbContract.RiseSetEntry.CONTENT_URI_RISE_SET,
                        RISE_SET_PROJECTION, selection, selectionArgs, sortOrder)
            }
            LOADER_ID_TIDES -> {
                sortOrder = DbContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " ASC"
                selection = DbContract.TidesEntry.COLUMN_TIDES_DATE + "=?"

                return CursorLoader(activity!!, DbContract.TidesEntry.CONTENT_URI_TIDES,
                        TIDES_PROJECTION, selection, selectionArgs, sortOrder)
            }
            LOADER_ID_TIDES_HOME -> {
                sortOrder = DbContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " ASC"
                selection = DbContract.TidesEntry.COLUMN_TIDES_DATE + "=?"

                return CursorLoader(activity!!, DbContract.TidesEntry.CONTENT_URI_TIDES_HOME,
                        TIDES_PROJECTION, selection, selectionArgs, sortOrder)
            }

            else -> {
                sortOrder = DbContract.WindsEntry.COLUMN_TIME_OF_WIND + " ASC"
                selection = DbContract.WindsEntry.COLUMN_WINDS_DATE + "=?"

                return CursorLoader(activity!!, DbContract.WindsEntry.CONTENT_URI_WINDS,
                        WINDS_PROJECTION, selection, selectionArgs, sortOrder)
            }
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        mMap!!.uiSettings.isZoomControlsEnabled = mContainer.visibility != View.VISIBLE

        when (loader.id) {
            LOADER_ID_RISE_SET -> {
                Timber.d("onLoadFinished Rise Set count: ${cursor?.count}")
                mContainer.sunrise_set.visibility = if (cursor == null || cursor.count > 0) View.VISIBLE else View.GONE
                mContainer.moonrise_set.visibility = if (cursor == null || cursor.count > 0) View.VISIBLE else View.GONE

                while (cursor!!.moveToNext()) {
                    val type = cursor.getString(INDEX_RISE_SET_TYPE)
                    val time = cursor.getString(INDEX_RISE_SET_TIME)
                    val prettyTime = FloVindDateUtils.getFormattedTime(time)
                    when (type) {
                        "sunrise" -> {
                            mContainer.sunrise_set.rising_time.text = prettyTime
                        }
                        "sunset" -> {
                            mContainer.sunrise_set.setting_time.text = prettyTime
                        }
                        "moonrise" -> {
                            mContainer.moonrise_set.rising_time.text = prettyTime
                        }
                        "moonset" -> {
                            mContainer.moonrise_set.setting_time.text = prettyTime
                        }
                    }

                }
            }
            LOADER_ID_WINDS -> {
                Timber.d("onLoadFinished Winds: count: " + cursor?.count)
                mWindsAdapter!!.swapCursor(cursor)

            }
            else -> {
                when (cursor?.count) {

                    0 -> {
                        Timber.d("onLoadFinished Tides 1: count: " + cursor.count + "  tidesapterCount ${mTidesRecyclerView.adapter?.itemCount}")
                        mErrorTextView.visibility = View.VISIBLE
                        mErrorTextView.setText(R.string.connection_error)
                        mNextDay.visibility = View.INVISIBLE
                        mTidesAdapter!!.swapCursor(cursor)
                    }
                    in 1..2 -> {// cursor.count <= 2) {
                        Timber.d("onLoadFinished Tides 2: count: " + cursor?.count)
                        mWindsRecyclerView.visibility = View.GONE
                        cursor?.moveToFirst()
                        mErrorTextView.visibility = View.VISIBLE
                        mErrorTextView.text = cursor?.getString(INDEX_ERROR)
                        mNextDay.visibility = View.INVISIBLE
                        mTidesAdapter!!.swapCursor(null)
                    }
                    else -> {
                        Timber.d("onLoadFinished Tides 3: count: " + cursor?.count)
                        mWindsRecyclerView.visibility = View.VISIBLE
                        mTidesAdapter!!.swapCursor(cursor)
                        mErrorTextView.visibility = View.GONE
                        mNextDay.visibility = View.VISIBLE
                    }
                }
                val tomorrow = FloVindDateUtils.getPersistentDatePlusOne(currentPersistentDate!!)
                if (FloVindDateUtils.isDateLastInBatch(tomorrow, System.currentTimeMillis())) {
                    mNextDay.visibility = View.INVISIBLE
                }
                val isFirstDay = currentPersistentDate == FloVindDateUtils.getPersistentDateFromMillis(System.currentTimeMillis())
                if (isFirstDay) {
                    mPrevDay.visibility = View.INVISIBLE
                }
            }
        }
        if (locationBtnCliked) {
            mContainer.visibility = View.VISIBLE
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mTidesAdapter!!.swapCursor(null)
        mWindsAdapter!!.swapCursor(null)
    }

    private fun updateValuesOnLocationChange(latLng: LatLng) {
        val thread = Thread(Runnable { syncData(context!!, latLng) })
        thread.start()
    }

    private var mapInitialized: Boolean = false
    @SuppressWarnings("MissingPermission")
    override fun onMapReady(map: GoogleMap?) {
        mMap = map

        setMapClickListeners()

        mMap!!.isMyLocationEnabled = true
        if (!mapInitialized)
            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LAT_LNG_FORECAST, mMapZoom))
        mapInitialized = true

        Timber.d("Map camera set to lat: " + LAT_LNG_FORECAST!!.latitude)

        val maptype = mPreferences!!.getString(getString(R.string.pref_map_type_key), getString(R.string.map_type_def_value))

        mMap!!.mapType = Integer.parseInt(maptype!!)
        setSelectedStyle()


        mMap!!.setOnCameraIdleListener {
            Timber.d("map Cam Idle:\nlocationBtnCliked $locationBtnCliked \nmVisibility ${mVisibility == View.VISIBLE}")
            mContainer.visibility = if (locationBtnCliked || mVisibility == View.VISIBLE) View.VISIBLE else View.GONE
            locationBtnCliked = false
        }

        mMap!!.setOnCameraMoveStartedListener {
            Timber.d("map Cam started")
            mContainer.visibility = View.GONE
            mVisibility = View.GONE
            mMap!!.uiSettings.isZoomControlsEnabled = true
        }

    }

    private fun setMapClickListeners() {
        mMap!!.setOnMyLocationButtonClickListener(this)

        mMap!!.setOnMapClickListener { latLng ->
            //            LAT_LNG = latLng

//            setMapPrefs(latLng.latitude, latLng.longitude)
            var place = ""
            try {
                place = Utils.getAccuratePlaceName(context!!, latLng)
                //Timber.d(Utils.getAccuratePlaceName(getActivity(), latLng));
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (mCurrentMarker != null) mCurrentMarker!!.remove()
            mCurrentMarker = mMap!!.addMarker(MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.conditions_label))
                    .snippet(place))
            mCurrentMarker!!.showInfoWindow()
            mMap!!.setOnInfoWindowClickListener(this@ForecastFragment)
        }
    }

//    private fun setMapPrefs(latitude: Double, longitude: Double) {
//        val editor = mPreferences!!.edit()
//        editor.putString(EXTRA_TIDE_QUERY_DATE, FloVindDateUtils.getPersistentDateFromMillis(System.currentTimeMillis()))
//        editor.putString(FORECAST_LATITUDE_KEY, latitude.toString())
//        editor.putString(FORECAST_LONGITUDE_KEY, longitude.toString())
//        editor.apply()
//    }

    override fun onInfoWindowClick(marker: Marker) {
        locationBtnCliked = true
        LAT_LNG_FORECAST = LatLng(marker.position.latitude, marker.position.longitude)

//        setMapPrefs(marker.position.latitude, marker.position.longitude)

        updateValuesOnLocationChange(LAT_LNG_FORECAST!!)
        marker.hideInfoWindow()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        mCurrentMarker!!.showInfoWindow()
        return true
    }


    private var locationBtnCliked = false
    override fun onMyLocationButtonClick(): Boolean {
        Timber.d("onMyLocationButtonClick")
        locationBtnCliked = true
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        mPreferences?.edit()?.putString(EXTRA_TIDE_QUERY_DATE, FloVindDateUtils.getPersistentDateFromMillis(System.currentTimeMillis()))?.apply()
        LAT_LNG_FORECAST = LAT_LNG_HOME
//        updateValuesOnLocationChange(LAT_LNG_FORECAST!!)

        restartLoaders()

        return false
    }


//    override fun onSaveInstanceState(outState: Bundle) {
//        Timber.d("onSaveInstanceState")
//        // Store the selected map style, so we can assign it when the activity resumes.
//        //   outState.putInt(SELECTED_STYLE, mSelectedStyleId);
//
//        if (mMap != null) {
//            Timber.d("savingState zoom: ${mMap!!.cameraPosition.zoom}")
//            outState.putFloat(MAP_ZOOM, mMap!!.cameraPosition.zoom)
//        }
//        // outState.putParcelable(LOCATION, mLocation);
////        if (LAT_LNG_FORECAST != null) { // TODO Useful still?
////            outState.putDouble(FORECAST_LATITUDE_KEY, LAT_LNG_FORECAST!!.latitude)
////            outState.putDouble(FORECAST_LONGITUDE_KEY, LAT_LNG_FORECAST!!.longitude)
////        }
//        //outState.putString(PLACE_NAME, mLocationTextView.getText().toString());
////        outState.putInt(CONTAINER_VISIBILITY, mContainer.visibility)
//        super.onSaveInstanceState(outState)
//    }


    /**
     * Creates a [MapStyleOptions] object via loadRawResourceStyle() (or via the
     * constructor with a JSON String), then sets it on the [GoogleMap] instance,
     * via the setMapStyle() method.
     */
    private fun setSelectedStyle() {
        /*        mSelectedStyleId = Integer.valueOf(
                mPreferences.getString(getString(R.string.map_pref_key), getString(R.string.style_value_default)));*/
        val mapStyle = mPreferences!!.getString(getString(R.string.map_pref_key), getString(R.string.style_label_default))

        Timber.d("setSelStyle style: " + getString(mSelectedStyleId))
        val style: MapStyleOptions?
        when (mapStyle) {
            "Retro" ->
                // Sets the retro style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(activity!!, R.raw.mapstyle_retro)
            "Night" ->
                // Sets the night style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(activity!!, R.raw.mapstyle_night)
            "Grayscale" ->
                // Sets the grayscale style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(activity!!, R.raw.mapstyle_grayscale)
            "No POIs or transit" ->
                // Sets the no POIs or transit style via JSON string.
                style = MapStyleOptions("[" +
                        "  {" +
                        "    \"featureType\":\"poi.business\"," +
                        "    \"elementType\":\"all\"," +
                        "    \"stylers\":[" +
                        "      {" +
                        "        \"visibility\":\"off\"" +
                        "      }" +
                        "    ]" +
                        "  }," +
                        "  {" +
                        "    \"featureType\":\"transit\"," +
                        "    \"elementType\":\"all\"," +
                        "    \"stylers\":[" +
                        "      {" +
                        "        \"visibility\":\"off\"" +
                        "      }" +
                        "    ]" +
                        "  }" +
                        "]")
            "Default" ->
                // Removes previously set style, by setting it to null.
                style = null
            else -> return
        }
        mMap!!.setMapStyle(style)
    }

    companion object {

        private const val LOADER_ID_TIDES_HOME = 1348
        private const val LOADER_ID_TIDES = 1349
        private const val LOADER_ID_WINDS = 1350
        private const val LOADER_ID_RISE_SET = 1351

        val TIDES_PROJECTION = arrayOf(DbContract.TidesEntry.COLUMN_TIDES_DATE, DbContract.TidesEntry.COLUMN_WATER_LEVEL, DbContract.TidesEntry.COLUMN_LEVEL_FLAG, DbContract.TidesEntry.COLUMN_TIME_OF_LEVEL, DbContract.TidesEntry.COLUMN_TIDE_ERROR_MSG, DbContract.TidesEntry.COLUMN_TIDES_DATE_RAW)
        const val INDEX_TIDE_DATE = 0
        const val INDEX_TIDE_LEVEL = 1
        const val INDEX_LEVEL_TIME = 3
        const val INDEX_FLAG = 2
        const val INDEX_ERROR = 4
        const val INDEX_TIDE_DATE_RAW = 5

        val WINDS_PROJECTION = arrayOf(DbContract.WindsEntry.COLUMN_WINDS_DATE, DbContract.WindsEntry.COLUMN_TIME_OF_WIND, DbContract.WindsEntry.COLUMN_WIND_DIR_DEG, DbContract.WindsEntry.COLUMN_WIND_SPEED, DbContract.WindsEntry.COLUMN_WIND_DIRECTION)
        const val INDEX_WIND_TIME = 1
        const val INDEX_WIND_SPEED = 3
        const val INDEX_WIND_DIR = 4

        val RISE_SET_PROJECTION = arrayOf(DbContract.RiseSetEntry.COLUMN_RISE_SET_TYPE, DbContract.RiseSetEntry.COLUMN_RISE_SET_DATE, DbContract.RiseSetEntry.COLUMN_TIME_OF_RISE_SET)
        const val INDEX_RISE_SET_TYPE = 0
        const val INDEX_RISE_SET_TIME = 2

        const val EXTRA_TIDE_QUERY_DATE = "tides_date"
        private const val SELECTED_STYLE = "selected_style"
        private const val MAP_ZOOM = "map_zoom"
        private const val PLACE_NAME = "location_name"
        private const val LOCATION = "location"
        private const val CONTAINER_VISIBILITY = "visibility"

        private const val MAP_ZOOM_DEFAULT = 8f
        private var LAT_LNG_FORECAST: LatLng? = null
        private var LAT_LNG_HOME: LatLng? = null

        fun newInstance(location: Location): ForecastFragment {
            val args = Bundle()
            args.putParcelable(LOCATION, location)
            val f = ForecastFragment()
            f.arguments = args

            return f
        }
    }
}