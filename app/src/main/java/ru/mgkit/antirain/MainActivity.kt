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
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager
import android.view.View

import com.google.android.material.snackbar.Snackbar
import android.graphics.Color
import android.location.*
import org.osmdroid.util.GeoPoint

import android.widget.EditText
import android.widget.ImageButton
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import java.util.*
import kotlin.collections.ArrayList
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.widget.Toast
import org.osmdroid.views.overlay.Overlay

import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Polyline

const val REQUEST_CODE_PERMISSION_LOCATION = 0

class MainActivity : AppCompatActivity() {

    // Инициализируем переменные
    private lateinit var map:MapView
    private lateinit var startPoint: GeoPoint
    private lateinit var endPoint: GeoPoint
    private lateinit var currentPossitionButton: ImageButton
    private lateinit var targetPoint: ImageButton
    private lateinit var position_from_field: EditText
    private lateinit var position_to_field: EditText


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val policy = ThreadPolicy.Builder().permitAll().build() // Разрешение на работу с потоками в карте
        StrictMode.setThreadPolicy(policy)
        val ctx: Context = applicationContext // Для запроса разрешений
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        setContentView(R.layout.activity_main)
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK) // Настройка карты из OSM
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.setMultiTouchControls(true)

        // Задаем массив-список для разрешений
        val arrPerm: ArrayList<String> = ArrayList()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            arrPerm.add(Manifest.permission.ACCESS_COARSE_LOCATION) // Добавить если НЕ
        }

        // Добавляем в массив-список если права не выданы
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            arrPerm.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Запрашиваем права, если список не пустой
        if (arrPerm.isNotEmpty()) {
            var permissions: Array<String?>? = arrayOfNulls(arrPerm.size)
            permissions = arrPerm.toArray(permissions)
            ActivityCompat.requestPermissions(this, permissions,  REQUEST_CODE_PERMISSION_LOCATION)
        }

        // Настройка оверлея для текущего положения пользователя
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

        // Добавление оверлея
        map.getOverlays().add(mLocationOverlay)

        // Настройка оверлея для отображения компаса
        val mCompassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), map)
        mCompassOverlay.enableCompass()
        map.getOverlays().add(mCompassOverlay)

        // Инициализация объектов
        currentPossitionButton = findViewById(R.id.startCurrentPosition)
        targetPoint = findViewById(R.id.EnterPosition)
        position_from_field = findViewById(R.id.et_positionFrom)
        position_to_field = findViewById(R.id.et_positionTo)

        //Текущая позиция
        currentPossitionButton.setOnClickListener {
            val location = mLocationProvider.lastKnownLocation
            val lat = (location.getLatitude() * 1E6).toInt()
            val lng = (location.getLongitude() * 1E6).toInt()
            startPoint  = GeoPoint(lat, lng);

            //Подготавливаем маркер
            val startMarker = Marker(map)
            startMarker.position = startPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.id = "startMarker"

            //Очистка от старого маркера
            for (i in 0 until map.getOverlays().size) {
                val overlay: Overlay = map.getOverlays().get(i)
                if (overlay is Marker && (overlay).id == "startMarker") {
                    map.getOverlays().remove(overlay)
                    break
                }
            }
            //Добавляем новый маркер
            map.overlays.add(startMarker)

            //Перерисовка карты
            map.invalidate()
            startMarker.setTitle("Start point")

            //Обратное геокодирование
            val geocoder: Geocoder
            val addresses: List<Address>
            geocoder = Geocoder(this, Locale.getDefault())

            //Отображение в текстовое поле
            addresses = geocoder.getFromLocation(location.latitude, location.getLongitude(), 1)

            position_from_field.setText(addresses[0].countryName+","
                    +addresses[0].adminArea+","+addresses[0].subAdminArea+","
                    +addresses[0].locality+","+addresses[0].thoroughfare+","
                    +addresses[0].featureName)
        }

        //Запрос маршрута
        targetPoint.setOnClickListener{
            val adr_str = position_to_field.text.toString()
            if (adr_str != "") {
                //Из адреса -> получить координаты
                val nom_coder = GeocoderNominatim(Locale.getDefault(), "AntiRain" )
                val address = nom_coder.getFromLocationName(adr_str, 1)
                val lat = (address[0].latitude * 1E6).toInt()
                val lng = (address[0].longitude * 1E6).toInt()

                //Настройка конечного маркера
                endPoint  = GeoPoint(lat, lng);
                val endMarker = Marker(map)
                endMarker.position = endPoint
                endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                endMarker.setTitle("End point")
                endMarker.id = "endMarker"

                //Очистка маркера
                for (i in 0 until map.overlays.size) {
                    val overlay: Overlay = map.overlays[i]
                    if (overlay is Marker && (overlay).id == "endMarker") {
                        map.getOverlays().remove(overlay)
                        break
                    }
                }

                //Очистка первого маршрута
                for (i in 0 until map.overlays.size) {
                    val overlay: Overlay = map.overlays[i]
                    if (overlay is Polyline) {
                        map.getOverlays().remove(overlay)
                        break
                    }
                }

                //Очистка второго маршрута
                for (i in 0 until map.overlays.size) {
                    val overlay: Overlay = map.overlays[i]
                    if (overlay is Polyline) {
                        map.getOverlays().remove(overlay)
                        break
                    }
                }

                //Добавление второго маркера
                map.overlays.add(endMarker)
                map.invalidate()

                //Точки для построения маршрутов
                val waypoints = ArrayList<GeoPoint>()
                waypoints.add(startPoint)
                waypoints.add(endPoint)

                //Создание объекта roadManager из класса (Utils)

                //Классический маршрут
                val roadManager: RoadManager = ValhalaRoadManager(this, "AntiRain")
                (roadManager as ValhalaRoadManager).setMean(ValhalaRoadManager.MEAN_BY_BIKE)

                val road = roadManager.getRoad(waypoints)
                val roadOverlay = RoadManager.buildRoadOverlay(road, Color.BLUE, 13.5f)
                val roadInfo = "Length = ${road?.mLength} km\nTime = ${road?.mDuration?.div(60.0)} min"

                roadOverlay.setOnClickListener { polyline, mapView, eventPos ->
                Toast.makeText(ctx, roadInfo, Toast.LENGTH_LONG).show()
                true}
                map.getOverlays().add(roadOverlay)

                //Маршрут с учетом осадков
                (roadManager as ValhalaRoadManager).setMean(ValhalaRoadManager.MEAN_BY_BIKE_WH)

                val road_wh = roadManager.getRoad(waypoints)
                val roadOverlay_wh = RoadManager.buildRoadOverlay(road_wh, Color.GREEN, 13.5f)
                val road_whInfo = "Length = ${road_wh?.mLength} km\nTime = ${road_wh?.mDuration?.div(60.0)} min"

                roadOverlay_wh.setOnClickListener { polyline, mapView, eventPos ->
                    Toast.makeText(ctx, road_whInfo, Toast.LENGTH_LONG).show()
                    true}

                map.getOverlays().add(roadOverlay_wh)

                map.invalidate()
            }

        }

    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    // Проверяем разрешил ли пользователь права или нет
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
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
