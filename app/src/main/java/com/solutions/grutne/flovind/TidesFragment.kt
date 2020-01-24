package com.solutions.grutne.flovind

import android.content.SharedPreferences
import android.database.Cursor
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

import com.solutions.grutne.flovind.MainActivity.Companion.EXTRA_LATITUDE
import com.solutions.grutne.flovind.MainActivity.Companion.EXTRA_LONGITUDE


import java.io.IOException
import java.text.ParseException
import java.util.concurrent.TimeUnit
import com.solutions.grutne.flovind.adapters.TidesDataAdapter
import com.solutions.grutne.flovind.adapters.WindsDataAdapter
import com.solutions.grutne.flovind.data.DbContract
import com.solutions.grutne.flovind.sync.FloVindSyncTask.syncData
import com.solutions.grutne.flovind.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

/**
 * Created by Adrian on 24/10/2017.
 */

class TidesFragment : android.support.v4.app.Fragment(),
        android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>,
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
    private var mMapZoom = MAP_ZOOM_DEFAULT.toFloat()
    private var mPreferences: SharedPreferences? = null
    private var mVisibility = View.VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = mPreferences!!.edit() // make member?

        if (savedInstanceState != null) {
            Timber.d("Saved not null")
            //  mSelectedStyleId = savedInstanceState.getInt(SELECTED_STYLE);
            mMapZoom = savedInstanceState.getFloat(MAP_ZOOM)

            val longitude = savedInstanceState.getDouble(EXTRA_LONGITUDE)
            val latitude = savedInstanceState.getDouble(EXTRA_LATITUDE)
            editor.putString(EXTRA_LATITUDE, latitude.toString())
            editor.putString(EXTRA_LONGITUDE, longitude.toString())
            editor.apply()

            LAT_LNG = LatLng(latitude, longitude)
        }


        editor.putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis())).apply()
        mTidesAdapter = TidesDataAdapter(context!!)
        mWindsAdapter = WindsDataAdapter(context!!)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tides, container, false)
        mLocationButton = (view.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById(Integer.parseInt("2"))

        val rlp = mLocationButton!!.layoutParams as RelativeLayout.LayoutParams
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
        rlp.setMargins(0, resources.getInteger(R.integer.loc_butt_padding_top), 32, 0)

        mContainer = view.findViewById<CardView>(R.id.cardview_container) as CardView
        mDateTimeTextView = view.findViewById<TextView>(R.id.forecast_date) as TextView
        mErrorTextView = view.findViewById<TextView>(R.id.tides_error_tv) as TextView
        mLocationTextView = view.findViewById<TextView>(R.id.location_name) as TextView
        mNextDay = view.findViewById<RelativeLayout>(R.id.next_day_button) as RelativeLayout
        mPrevDay = view.findViewById<RelativeLayout>(R.id.prev_day_button) as RelativeLayout
        if (savedInstanceState != null) {
            mVisibility = savedInstanceState.getInt(CONTAINER_VISIBILITY)
            mContainer.visibility = mVisibility

        }
        mTidesRecyclerView = view.findViewById<RecyclerView>(R.id.tides_recycler_view) as RecyclerView
        mTidesRecyclerView.layoutManager = LinearLayoutManager(activity)
        mTidesRecyclerView.adapter = mTidesAdapter

        mWindsRecyclerView = view.findViewById<RecyclerView>(R.id.winds_recycler_view) as RecyclerView
        mWindsRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        mWindsRecyclerView.adapter = mWindsAdapter


        return view
    }

    private fun initLocation() {

        try {
            mLocation = arguments!!.getParcelable(LOCATION)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

        val useHomeLocation = true
        val editor = mPreferences!!.edit()
        val longitude: String
        val latitude: String
        if (mLocation != null) {
            Timber.d("loc notNull latitude: " + mLocation!!.latitude)
            LAT_LNG = LatLng(mLocation!!.latitude, mLocation!!.longitude)
            longitude = mLocation!!.longitude.toString()
            latitude = mLocation!!.latitude.toString()
            editor.putString(EXTRA_LONGITUDE, longitude)
            editor.putString(EXTRA_LATITUDE, latitude)
            editor.commit()


        } else {
            // i statsnail satt til trondheim -
            showSnackbar("Location") // TODO
        }
        updateValuesOnLocationChange(useHomeLocation)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        if (savedInstanceState == null) {
            Timber.d("saved == null")
            initLocation()
        } else
            restartLoader(false)

        mNextDay.setOnClickListener(View.OnClickListener {
            val currentDate = mPreferences!!.getString(EXTRA_TIDE_QUERY_DATE,
                    Utils.getDate(System.currentTimeMillis()))
            try {
                val tomorrow = Utils.getDatePlusOne(currentDate)

                if (Utils.isTomorrowLast(tomorrow)) {
                    mNextDay.visibility = View.INVISIBLE
                    return@OnClickListener
                } else {
                    mPreferences!!.edit().putString(EXTRA_TIDE_QUERY_DATE, tomorrow).apply()
                    mDateTimeTextView.text = (Utils.getPrettyDate(Utils.getDateInMillisec(tomorrow))) // TODO not very efficient, lag egen metode
                    activity!!.supportLoaderManager.restartLoader(LOADER_ID_TIDES, null, this@TidesFragment)
                    activity!!.supportLoaderManager.restartLoader(LOADER_ID_WINDS, null, this@TidesFragment)
                }
            } catch (e: ParseException) {
                Timber.e("failed to increase date")
                e.printStackTrace()
            }

            mPrevDay.visibility = View.VISIBLE
        })
        mPrevDay.setOnClickListener {
            val currentDate = mPreferences!!.getString(EXTRA_TIDE_QUERY_DATE,
                    Utils.getDate(System.currentTimeMillis()))
            try {
                val yesterday = Utils.getDateMinusOne(currentDate)
                mPreferences!!.edit().putString(EXTRA_TIDE_QUERY_DATE, yesterday).apply()
                mDateTimeTextView.text = (Utils.getPrettyDate(Utils.getDateInMillisec(yesterday)))
                activity!!.supportLoaderManager.restartLoader(LOADER_ID_TIDES, null, this@TidesFragment)
                activity!!.supportLoaderManager.restartLoader(LOADER_ID_WINDS, null, this@TidesFragment)
            } catch (e: ParseException) {
                Timber.e("failed to decrease date")
                e.printStackTrace()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (!Utils.workingConnection(context!!)) { // TODO and adapter empty for day btns?
            mNextDay.visibility = View.INVISIBLE
            mPrevDay.visibility = View.INVISIBLE
            showSnackbar(getString(R.string.connection_error))
        }
    }

    private fun showSnackbar(text: String) {
        val container = activity!!.findViewById<View>(R.id.tides_content)
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun restartLoader(homeLocation: Boolean) {
        try {
            mLocationTextView.text = Utils.getPlaceName(context!!, homeLocation)
//            mLocationTextView!!.text = (Utils.getAccuratePlaceName(context!!, homeLocation))
            val dateShown = mPreferences!!.getString(EXTRA_TIDE_QUERY_DATE,
                    Utils.getDate(System.currentTimeMillis()))
            mDateTimeTextView.text = (Utils.getPrettyDate(Utils.getDateInMillisec(dateShown!!)))
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

        activity!!.supportLoaderManager.restartLoader(LOADER_ID_TIDES, null, this)
        activity!!.supportLoaderManager.restartLoader(LOADER_ID_WINDS, null, this)
    }


    override fun onCreateLoader(i: Int, bundle: Bundle?): android.support.v4.content.Loader<Cursor>? {
        Timber.d("onCr Loader")
        val sortOrder: String
        val selection: String
        val selectionArgs = arrayOf(mPreferences!!.getString(EXTRA_TIDE_QUERY_DATE,
                Utils.getDate(System.currentTimeMillis())))
        when (i) {
            LOADER_ID_TIDES -> {
                sortOrder = DbContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " ASC"
                selection = DbContract.TidesEntry.COLUMN_TIDES_DATE + "=?"

                return android.support.v4.content.CursorLoader(activity!!, DbContract.TidesEntry.CONTENT_URI_TIDES,
                        TIDES_PROJECTION, selection, selectionArgs, sortOrder)
            }

            LOADER_ID_WINDS -> {
                sortOrder = DbContract.WindsEntry.COLUMN_TIME_OF_WIND + " ASC"
                selection = DbContract.WindsEntry.COLUMN_WINDS_DATE + "=?"

                return android.support.v4.content.CursorLoader(activity!!, DbContract.WindsEntry.CONTENT_URI_WINDS,
                        WINDS_PROJECTION, selection, selectionArgs, sortOrder)
            }
        }
        return null
    }

    override fun onLoadFinished(loader: android.support.v4.content.Loader<Cursor>, cursor: Cursor?) {

        mMap!!.uiSettings.isZoomControlsEnabled = mContainer.visibility != View.VISIBLE

//        val bounds = LatLngBounds(LatLng(57.817944, 7.600304), LatLng(71.171213, 25.793040))
//        mMap!!.setLatLngBoundsForCameraTarget(bounds)

        val currentDate = mPreferences!!.getString(EXTRA_TIDE_QUERY_DATE,
                Utils.getDate(System.currentTimeMillis()))
        if (currentDate < (Utils.getDate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)))) {
            mPrevDay.visibility = View.INVISIBLE
        }
        when (loader.id) {
            LOADER_ID_WINDS -> {
                Timber.d("onLoadFinished Winds: count: " + cursor?.count)
                mWindsAdapter!!.swapCursor(cursor)

            }
            LOADER_ID_TIDES -> {
                when (cursor?.count) {

                    0 -> {
                        Timber.d("onLoadFinished Tides 1: count: " + cursor.count + "  tidesapterCount ${mTidesRecyclerView.adapter.itemCount}")
                        mErrorTextView.visibility = View.VISIBLE
                        mErrorTextView.setText(R.string.connection_error)
                        mNextDay.visibility = View.INVISIBLE
                    }
                    in 1..2 -> {// cursor.count <= 2) {
                        Timber.d("onLoadFinished Tides 2: count: " + cursor?.count)
                        mWindsRecyclerView.visibility = View.GONE // TODO fikse - problemet er eldre api ~23
                        cursor?.moveToFirst()
                        mErrorTextView.visibility = View.VISIBLE
                        mErrorTextView.text = cursor?.getString(INDEX_ERROR)
                        //  Toast.makeText(getActivity(), "Error: " + cursor.getString(INDEX_ERROR), Toast.LENGTH_SHORT).show();
                        mNextDay.visibility = View.INVISIBLE
                        mTidesAdapter!!.swapCursor(null)
                    }
                    else -> {
                        Timber.d("onLoadFinished Tides 3: count: " + cursor?.count)
                        mWindsRecyclerView.visibility = View.VISIBLE // TODO fikse - problemet er eldre api ~23
                        mTidesAdapter!!.swapCursor(cursor)
                        mErrorTextView.visibility = View.GONE
                        mNextDay.visibility = View.VISIBLE
                    }
                }
            }
        }
        if (locationBtnCliked) {
            mContainer.visibility = View.VISIBLE
//            locationBtnCliked = false
        }
    }

    override fun onLoaderReset(loader: android.support.v4.content.Loader<Cursor>) {
        mTidesAdapter!!.swapCursor(null)
        mWindsAdapter!!.swapCursor(null)
    }

    private fun updateValuesOnLocationChange(homeLocation: Boolean) {
        val thread = Thread(Runnable { syncData(context!!, homeLocation) })
        thread.start()
        restartLoader(homeLocation)

    }

    @SuppressWarnings("MissingPermission")
    override fun onMapReady(map: GoogleMap?) {
        mMap = map
        //mResetLoc!!.visibility = View.GONE
        /*     mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(!(mVisibility == View.VISIBLE));
        mMap.getUiSettings().setZoomGesturesEnabled(true);*/

        mMap!!.setOnMyLocationButtonClickListener(this)
        mMap!!.isMyLocationEnabled = true
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LAT_LNG, mMapZoom))


        Timber.d("Map camera set to lat: " + LAT_LNG!!.latitude)

        val maptype = mPreferences!!.getString(getString(R.string.pref_map_type_key), getString(R.string.map_type_def_value))
/*        if (maptype == GoogleMap.MAP_TYPE_HYBRID.toString() || maptype == GoogleMap.MAP_TYPE_SATELLITE.toString())
            mResetLoc!!.setBackgroundColor(Color.WHITE)
        if (maptype == GoogleMap.MAP_TYPE_NONE.toString())
            mResetLoc!!.visibility = View.GONE*/

        mMap!!.mapType = Integer.parseInt(maptype)
        setSelectedStyle()

        mMap!!.setOnMapLongClickListener { latLng ->
            LAT_LNG = latLng
            val editor = mPreferences!!.edit()
            editor.putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis()))
            editor.putString(EXTRA_LATITUDE, latLng.latitude.toString())
            editor.putString(EXTRA_LONGITUDE, latLng.longitude.toString())
            editor.commit()

            updateValuesOnLocationChange(false)
            mVisibility = View.VISIBLE
        }

        mMap!!.setOnMapClickListener { latLng ->
            LAT_LNG = latLng
            val editor = mPreferences!!.edit()
            editor.putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis()))
            editor.putString(EXTRA_LATITUDE, latLng.latitude.toString())
            editor.putString(EXTRA_LONGITUDE, latLng.longitude.toString())
            editor.commit()
            var place = ""
            try {
                place = Utils.getAccuratePlaceName(context!!, latLng) // TODO pinpusse accurateplace
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
            mMap!!.setOnInfoWindowClickListener(this@TidesFragment)
        }

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

    override fun onInfoWindowClick(marker: Marker) {
        locationBtnCliked = true

        val editor = mPreferences!!.edit()
        editor.putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis()))
        editor.putString(EXTRA_LATITUDE, marker.position.latitude.toString())
        editor.putString(EXTRA_LONGITUDE, marker.position.longitude.toString())
        editor.commit()

        updateValuesOnLocationChange(false)
        marker.hideInfoWindow()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        mCurrentMarker!!.showInfoWindow()
        return true
    }


    private var locationBtnCliked = false
    override fun onMyLocationButtonClick(): Boolean {
        Timber.d("onMyLocationButtonClick")
//        mVisibility = View.VISIBLE
        locationBtnCliked = true
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        initLocation()
//        mMapZoom = MAP_ZOOM_DEFAULT

        return false
    }


    override fun onSaveInstanceState(outState: Bundle) {
        // Store the selected map style, so we can assign it when the activity resumes.
        //   outState.putInt(SELECTED_STYLE, mSelectedStyleId);
        if (mMap != null) outState.putFloat(MAP_ZOOM, mMap!!.cameraPosition.zoom)
        // outState.putParcelable(LOCATION, mLocation);
        if (LAT_LNG != null) {
            outState.putDouble(EXTRA_LATITUDE, LAT_LNG!!.latitude)
            outState.putDouble(EXTRA_LONGITUDE, LAT_LNG!!.longitude)
        }
        //outState.putString(PLACE_NAME, mLocationTextView.getText().toString());
        outState.putInt(CONTAINER_VISIBILITY, mContainer.visibility)
        super.onSaveInstanceState(outState)
    }

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

        private const val LOADER_ID_TIDES = 1349
        private const val LOADER_ID_WINDS = 1350

        val TIDES_PROJECTION = arrayOf<String>(DbContract.TidesEntry.COLUMN_TIDES_DATE, DbContract.TidesEntry.COLUMN_WATER_LEVEL, DbContract.TidesEntry.COLUMN_LEVEL_FLAG, DbContract.TidesEntry.COLUMN_TIME_OF_LEVEL, DbContract.TidesEntry.COLUMN_TIDE_ERROR_MSG)
        val INDEX_TIDE_DATE = 0
        val INDEX_TIDE_LEVEL = 1
        val INDEX_LEVEL_TIME = 3
        val INDEX_FLAG = 2
        val INDEX_ERROR = 4

        val WINDS_PROJECTION = arrayOf<String>(DbContract.WindsEntry.COLUMN_WINDS_DATE, DbContract.WindsEntry.COLUMN_TIME_OF_WIND, DbContract.WindsEntry.COLUMN_WIND_DIR_DEG, DbContract.WindsEntry.COLUMN_WIND_SPEED, DbContract.WindsEntry.COLUMN_WIND_DIRECTION)
        val INDEX_WIND_DATE = 0
        val INDEX_WIND_TIME = 1
        val INDEX_WIND_DIR_DEG = 2
        val INDEX_WIND_SPEED = 3
        val INDEX_WIND_DIR = 4


        val EXTRA_TIDE_QUERY_DATE = "tides_date"
        private val SELECTED_STYLE = "selected_style"
        private val MAP_ZOOM = "map_zoom"
        private val PLACE_NAME = "location_name"
        private val LOCATION = "location"
        private val CONTAINER_VISIBILITY = "visibility"

        private val FORECAST_DAYS = 7
        private val MAP_ZOOM_DEFAULT = 8f
        private var LAT_LNG: LatLng? = null

        fun newInstance(location: Location): TidesFragment {
            val args = Bundle()
            args.putParcelable(LOCATION, location)
            val f = TidesFragment()
            f.arguments = args

            return f
        }
    }
}