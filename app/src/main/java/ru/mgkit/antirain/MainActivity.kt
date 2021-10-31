package ru.mgkit.antirain

import android.Manifest
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
import android.location.Location
import android.location.LocationListener
import org.osmdroid.util.GeoPoint

import org.osmdroid.api.IMapController
import android.location.LocationManager
import org.osmdroid.views.CustomZoomButtonsController
import java.lang.Exception


const val REQUEST_CODE_PERMISSION_LOCATION = 0

class MainActivity : AppCompatActivity() {
    private lateinit var map:MapView
    private var mCurrentLocation: Location? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
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
