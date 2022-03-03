package ru.mgkit.antirain

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager
import android.view.View

import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import android.content.Intent
import android.location.*
import org.osmdroid.util.GeoPoint

import org.osmdroid.api.IMapController
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.views.CustomZoomButtonsController
import java.lang.Exception
import org.osmdroid.views.overlay.Marker
import java.util.*
import kotlin.collections.ArrayList
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.bonuspack.routing.OSRMRoadManager

import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.views.overlay.Polyline

const val REQUEST_CODE_PERMISSION_LOCATION = 0

class MainActivity : AppCompatActivity() {
    private lateinit var map:MapView
    private var mCurrentLocation: Location? = null
    private lateinit var startPoint: GeoPoint
    private lateinit var endPoint: GeoPoint
    private lateinit var currentPossitionButton: ImageButton
    private lateinit var targetPoint: ImageButton
    private lateinit var position_from_field: EditText
    private lateinit var position_to_field: EditText


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        val ctx: Context = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        setContentView(R.layout.activity_main)
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.setMultiTouchControls(true)

        val arrPerm: ArrayList<String> = ArrayList()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            arrPerm.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            arrPerm.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (arrPerm.isNotEmpty()) {
            var permissions: Array<String?>? = arrayOfNulls(arrPerm.size)
            permissions = arrPerm.toArray(permissions)
            ActivityCompat.requestPermissions(this, permissions,  REQUEST_CODE_PERMISSION_LOCATION)
        }

        val mLocationProvider = GpsMyLocationProvider(ctx)
        val mLocationOverlay = MyLocationNewOverlay(mLocationProvider, map)
        mLocationOverlay.enableMyLocation()
        mLocationOverlay.enableFollowLocation()
        mLocationOverlay.isDrawAccuracyEnabled = true
        val mapController = map.controller
        mLocationOverlay.runOnFirstFix{runOnUiThread {
            mapController.animateTo(mLocationOverlay.myLocation)
            mapController.setZoom(9.5)
        }}
        map.getOverlays().add(mLocationOverlay)

        val mCompassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay)

        currentPossitionButton = findViewById(R.id.startCurrentPosition)
        targetPoint = findViewById(R.id.EnterPosition)
        position_from_field = findViewById(R.id.et_positionFrom)
        position_to_field = findViewById(R.id.et_positionTo)

        currentPossitionButton.setOnClickListener {
            val location = mLocationProvider.lastKnownLocation
            val lat = (location.getLatitude() * 1E6).toInt()
            val lng = (location.getLongitude() * 1E6).toInt()
            startPoint  = GeoPoint(lat, lng);
            val startMarker = Marker(map)
            startMarker.position = startPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.id = "startMarker"

            for (i in 0 until map.getOverlays().size) {
                val overlay: Overlay = map.getOverlays().get(i)
                if (overlay is Marker && (overlay).id == "startMarker") {
                    map.getOverlays().remove(overlay)
                    break
                }
            }

            map.overlays.add(startMarker)
            map.invalidate();
            //startMarker.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
            startMarker.setTitle("Start point")
            val geocoder: Geocoder
            val addresses: List<Address>
            geocoder = Geocoder(this, Locale.getDefault())
            addresses = geocoder.getFromLocation(location.latitude, location.getLongitude(), 1)
            position_from_field.setText(addresses[0].countryName+","
                    +addresses[0].adminArea+","+addresses[0].subAdminArea+","
                    +addresses[0].locality+","+addresses[0].thoroughfare+","
                    +addresses[0].featureName)
        }
        targetPoint.setOnClickListener{
            val adr_str = position_to_field.text.toString()
            if (adr_str != "") {
                val nom_coder = GeocoderNominatim(Locale.getDefault(), "AntiRain" )
                val address = nom_coder.getFromLocationName(adr_str, 1)
                // проверка есть ли кординаты и перебор вариантов, если нет
                //address[0].latitude
                //*Доделать второй маркер, geoPoint - double*
                val lat = (address[0].latitude * 1E6).toInt()
                val lng = (address[0].longitude * 1E6).toInt()
                endPoint  = GeoPoint(lat, lng);
                val endMarker = Marker(map)
                endMarker.position = endPoint
                endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                endMarker.setTitle("End point")
                endMarker.id = "endMarker"

                for (i in 0 until map.overlays.size) {
                    val overlay: Overlay = map.overlays[i]
                    if (overlay is Marker && (overlay).id == "endMarker") {
                        map.getOverlays().remove(overlay)
                        break
                    }
                }

                map.overlays.add(endMarker)
                map.invalidate()
                val roadManager: RoadManager = ValhalaRoadManager(this, "AntiRain")
                (roadManager as ValhalaRoadManager).setMean(ValhalaRoadManager.MEAN_BY_BIKE)
                val waypoints = ArrayList<GeoPoint>()
                waypoints.add(startPoint)
                waypoints.add(endPoint)
                val road = roadManager.getRoad(waypoints)
                val roadOverlay = RoadManager.buildRoadOverlay(road)
                map.getOverlays().add(roadOverlay)
                map.invalidate()
            }


        }

    }


    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSION_LOCATION -> {
                if (grantResults.size == 2 && (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) return
                val parentLayout: View = findViewById(android.R.id.content)
                Snackbar.make(parentLayout, "Ooops!", Snackbar.LENGTH_LONG).setAction("CLOSE"
                ) { finish() }

                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
                return
            }
        }
    }

}
