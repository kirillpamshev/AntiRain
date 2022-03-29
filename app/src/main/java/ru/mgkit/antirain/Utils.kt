package ru.mgkit.antirain

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadLeg
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.bonuspack.utils.BonusPackHelper
import org.osmdroid.bonuspack.utils.PolylineEncoder
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.HashMap
import org.osmdroid.bonuspack.R

import java.text.NumberFormat

fun round6(d: Double):Double {
    val format = NumberFormat.getInstance()
    format.maximumFractionDigits = 6
    val formattedNumber = format.format(d)
    return formattedNumber.replace(",", ".").toDouble()
}

fun decoder(enc: String): ArrayList<GeoPoint> {
    val kPolylinePrecision = 1E6
    val kInvPolylinePrecision = 1.0 / kPolylinePrecision
    var i =  0
    var a = 0
    val decoded: ArrayList<Array<Double>> = arrayListOf()
    var decoded_item: Array<Double>?;
    var previous: Array<Int> = arrayOf(0, 0)
    while (i < enc.length) {
        var ll: Array<Int> = arrayOf(0, 0)
        for( j in 0 .. 1) {
            var shift = 0
            var byte = 0x20
            while (byte >= 0x20) {
                byte = (enc[i++].code.toByte() - 63)
                a = (byte and 0x1f) shl shift
                ll[j] = ll[j] or a
                shift += 5
            }

            ll[j] = previous[j] + if ((ll[j] and 1) != 0) {
                (ll[j] ushr 1).inv()
            } else {
                ll[j] ushr 1
            }

            previous[j] = ll[j]
        }
        decoded_item = arrayOf(round6(ll[1] * kInvPolylinePrecision),  round6(ll[0] * kInvPolylinePrecision))
        decoded.add(decoded_item)
    }
    val points = arrayListOf<GeoPoint>()
    for(ar in decoded) {
        points.add(GeoPoint(ar[1], ar[0]))
    }
    return points




}


/** get a route between a start and a destination point, going through a list of waypoints.
 * It uses OSRM, a free open source routing service based on OpenSteetMap data. <br></br>
 *
 * It requests by default the OSRM demo site.
 * Use setService() to request an other (for instance your own) OSRM service. <br></br>
 *
 * @see [OSRM](https://github.com/DennisOSRM/Project-OSRM/wiki/Server-api)
 *
 * @see [V5 API](https://github.com/Project-OSRM/osrm-backend/wiki/New-Server-api)
 *
 *
 * @author M.Kergall
 */

class ValhalaRoadManager() :
    RoadManager() {
        //"http://localhost:8002/route --data '{"locations":[{"lat":55.7074932,"lon":37.5690340},{"lat":55.7175925,"lon":37.5496478}],"costing":"bicycle","directions_type":"maneuvers"}' | jq '.'"

    private lateinit var mMeanUrl: String
    private lateinit var mContext: Context
    protected lateinit var mUserAgent: String
    protected lateinit var mServiceUrl: String

    constructor (context: Context, userAgent: String) : this() {
        mContext = context
        mUserAgent = userAgent
        mServiceUrl = ValhalaRoadManager.DEFAULT_SERVICE
        mMeanUrl = ValhalaRoadManager.MEAN_BY_BIKE
    }


    companion object {
        //static final String DEFAULT_SERVICE = "https://routing.openstreetmap.de/";
        const val MEAN_BY_BIKE = "bicycle"
        const val MEAN_BY_BIKE_WH = "bicyclewh"
        const val  DEFAULT_SERVICE= "http://192.168.0.105:8002/"
        /**
         * mapping from OSRM StepManeuver types to MapQuest maneuver IDs:
         */
        val MANEUVERS: HashMap<Int, Int> = HashMap<Int, Int>() // INT - INT

        //From: Project-OSRM-Web / WebContent / localization / OSRM.Locale.en.js
        // driving directions
        // %s: road name
        // %d: direction => removed
        // <*>: will only be printed when there actually is a road name
        val DIRECTIONS: HashMap<Int, Any> = HashMap<Int, Any>()

        init {
            //MANEUVERS.put("new name", 2) //road name change !    !    !    !    !    !    !    !    !
            MANEUVERS.put(22, 1) //Continue straight
            MANEUVERS.put(9, 6) //Slight right
            MANEUVERS.put(10, 7) //Right
            MANEUVERS.put(11, 8) //Sharp right
            MANEUVERS.put(12, 12) //U-turn
            MANEUVERS.put(13, 12) //U-turn
            MANEUVERS.put(14, 5) //Sharp left
            MANEUVERS.put(15, 4) //Left
            MANEUVERS.put(16, 3) //Slight left
            MANEUVERS.put(1, 24) //"Head" => used by OSRM as the start node. Considered here as a "waypoint".
            // TODO - to check...
            MANEUVERS.put(4, 24) //Arrived (at waypoint)
            MANEUVERS.put(27, 27) //Round-about, 1st exit
            MANEUVERS.put(27, 28) //
            MANEUVERS.put(27, 29)


            MANEUVERS.put(2, 30) // "roundabout-4"
            MANEUVERS.put(3, 30) // "roundabout-4"
            MANEUVERS.put(5, 30) // "roundabout-4"
            MANEUVERS.put(6, 30) // "roundabout-4"
            MANEUVERS.put(7, 30) // "roundabout-4"
            MANEUVERS.put(8, 30) // "roundabout-4"
            MANEUVERS.put(20, 30) // "roundabout-4"
            MANEUVERS.put(21, 30) // "roundabout-4"
            MANEUVERS.put(22, 30) // "roundabout-4"
            MANEUVERS.put(23, 30) // "roundabout-4"
            MANEUVERS.put(24, 30) // "roundabout-4"
            MANEUVERS.put(25, 30) // "roundabout-4"
            MANEUVERS.put(26, 30) // "roundabout-4"
            MANEUVERS.put(27, 30) // "roundabout-4"
            MANEUVERS.put(28, 30) // "roundabout-4"
            MANEUVERS.put(29, 30) // "roundabout-4"
            MANEUVERS.put(30, 30) // "roundabout-4"
            MANEUVERS.put(31, 30) // "roundabout-4"
            MANEUVERS.put(32, 30) // "roundabout-4"
            MANEUVERS.put(33, 30) // "roundabout-4"
            MANEUVERS.put(34, 30) // "roundabout-4"
            MANEUVERS.put(35, 30) // "roundabout-4"
            MANEUVERS.put(36, 30) // "roundabout-4"

            //TODO: other OSRM types to handle properly:
            MANEUVERS.put(38, 20)
            //MANEUVERS.put("merge-sharp left", 20)
            //MANEUVERS.put("merge-slight left", 20)
            MANEUVERS.put(37, 21)
            //MANEUVERS.put("merge-sharp right", 21)
            //MANEUVERS.put("merge-slight right", 21)
            //MANEUVERS.put("merge-straight", 22)
            MANEUVERS.put(19, 17)
            //MANEUVERS.put("ramp-sharp left", 17)
            //MANEUVERS.put("ramp-slight left", 17)
            MANEUVERS.put(18, 18)
            //MANEUVERS.put("ramp-sharp right", 18)
            //MANEUVERS.put("ramp-slight right", 18)
            MANEUVERS.put(17, 19)


            DIRECTIONS.put(1, R.string.osmbonuspack_directions_1)
            DIRECTIONS.put(2, R.string.osmbonuspack_directions_2)
            DIRECTIONS.put(3, R.string.osmbonuspack_directions_3)
            DIRECTIONS.put(4, R.string.osmbonuspack_directions_4)
            DIRECTIONS.put(5, R.string.osmbonuspack_directions_5)
            DIRECTIONS.put(6, R.string.osmbonuspack_directions_6)
            DIRECTIONS.put(7, R.string.osmbonuspack_directions_7)
            DIRECTIONS.put(8, R.string.osmbonuspack_directions_8)
            DIRECTIONS.put(12, R.string.osmbonuspack_directions_12)
            DIRECTIONS.put(17, R.string.osmbonuspack_directions_17)
            DIRECTIONS.put(18, R.string.osmbonuspack_directions_18)
            DIRECTIONS.put(19, R.string.osmbonuspack_directions_19)
            //DIRECTIONS.put(20, R.string.osmbonuspack_directions_20);
            //DIRECTIONS.put(21, R.string.osmbonuspack_directions_21);
            //DIRECTIONS.put(22, R.string.osmbonuspack_directions_22);
            DIRECTIONS.put(24, R.string.osmbonuspack_directions_24)
            DIRECTIONS.put(27, R.string.osmbonuspack_directions_27)
            DIRECTIONS.put(28, R.string.osmbonuspack_directions_28)
            DIRECTIONS.put(29, R.string.osmbonuspack_directions_29)
            DIRECTIONS.put(30, R.string.osmbonuspack_directions_30)
            DIRECTIONS.put(31, R.string.osmbonuspack_directions_31)
            DIRECTIONS.put(32, R.string.osmbonuspack_directions_32)
            DIRECTIONS.put(33, R.string.osmbonuspack_directions_33)
            DIRECTIONS.put(34, R.string.osmbonuspack_directions_34)
        }
    }



    /** to switch to another mean of transportation  */
    fun setMean(meanUrl: String) {
        mMeanUrl = meanUrl
    }

    //http://localhost:8002/route?json={"locations":[{"lat":55.7074932,"lon":37.5690340},{"lat":55.7175925,"lon":37.5496478}],"costing":"bicycle","directions_type":"maneuvers"}
    protected fun getUrl(waypoints: ArrayList<GeoPoint>, getAlternate: Boolean): String {
    val p_start_lat = "%.7f".format(waypoints[0].latitude).replace(",", ".")
    val p_start_lon = "%.7f".format(waypoints[0].longitude).replace(",", ".")
    val p_stop_lat = "%.7f".format(waypoints[1].latitude).replace(",", ".")
    val p_stop_lon = "%.7f".format(waypoints[1].longitude).replace(",", ".")
    return "${mServiceUrl}route?json={\"locations\":[{\"lat\":${p_start_lat},\"lon\":${p_start_lon}},{\"lat\":${p_stop_lat},\"lon\":${p_stop_lon}}],\"costing\":\"${mMeanUrl}\",\"directions_type\":\"maneuvers\"}"


    /*val urlString = StringBuilder(mServiceUrl + mMeanUrl)
        for (i in waypoints.indices) {
            val p = waypoints[i]
            if (i > 0) urlString.append(';')
            urlString.append(geoPointAsLonLatString(p))
        }
        urlString.append("?alternatives=" + if (getAlternate) "true" else "false")
        urlString.append("&overview=full&steps=true")
        urlString.append(mOptions)
        return urlString.toString()*/
    }

    protected fun defaultRoad(waypoints: ArrayList<GeoPoint>?): Array<Road?> {
        val roads = arrayOfNulls<Road>(1)
        roads[0] = Road(waypoints)
        return roads
    }

    protected fun getRoads(waypoints: ArrayList<GeoPoint>, getAlternate: Boolean): Array<Road?> {
        val url = getUrl(waypoints, getAlternate)
        Log.d(BonusPackHelper.LOG_TAG, "ValhalaRoadManager.getRoads:$url")
        val jString = BonusPackHelper.requestStringFromUrl(url, mUserAgent)
        if (jString == null) {
            Log.e(BonusPackHelper.LOG_TAG, "ValhalaRoadManager::getRoad: request failed.")
            return defaultRoad(waypoints)
        }
        return try {
            val jObject = JSONObject(jString)
            val jTrip = jObject.getJSONObject("trip")
            val jCode = jTrip.getInt("status")
            if (0 != jCode) {
                Log.e(
                    BonusPackHelper.LOG_TAG,
                    "ValhalaRoadManager::getRoad: error code=$jCode"
                )
                val roads = defaultRoad(waypoints)
                roads[0]!!.mStatus = Road.STATUS_INVALID
                roads
            } else {
                val road = Road()
                val roads = arrayOfNulls<Road>(1)
                road.mStatus = Road.STATUS_OK
                val jsummary = jTrip.getJSONObject("summary")
                val north = jsummary.getDouble("max_lat")
                val west = jsummary.getDouble("min_lon")
                val east = jsummary.getDouble("max_lon")
                val south = jsummary.getDouble("min_lat")
                road.mBoundingBox = BoundingBox(north, east, south, west)
                road.mLength = jsummary.getDouble("length")
                road.mDuration = jsummary.getDouble("time")

                //val startloc = jTrip.getJSONArray("locatons").getJSONObject(0)
                //val startpoint = GeoPoint(startloc.getDouble("lat"), startloc.getDouble("lon"))
                //road.mRouteHigh.add(startpoint)
                //legs:
                val jLegs = jTrip.getJSONArray("legs")
                for (l in 0 until jLegs.length()) {
                    val jLeg = jLegs.getJSONObject(l)
                    val enc_string = jLeg.getString("shape")
                    val enc_shape = decoder(enc_string)
                    road.mRouteHigh.addAll(enc_shape)
                    val leg = RoadLeg()
                    road.mLegs.add(leg)
                    val jLeg_sum = jLeg.getJSONObject("summary")
                    leg.mLength = jLeg_sum.getDouble("length")
                    leg.mDuration = jLeg_sum.getDouble("time")
                    //steps:
                    val jSteps = jLeg.getJSONArray("maneuvers")
                    //var lastNode: RoadNode? = null
                    //    var lastRoadName = ""
                    for (s in 0 until jSteps.length()) {
                        val jStep = jSteps.getJSONObject(s)
                        val node = RoadNode()
                        node.mLength = jStep.getDouble("length")
                        node.mDuration = jStep.getDouble("time")
                        val jLocation_index = jStep.getInt("begin_shape_index")
                        node.mLocation = enc_shape[jLocation_index]
                        val direction = jStep.getInt("type")
                        node.mManeuverType = getManeuverCode(direction)
                        var roadName = ""
                        try {
                            val roadNames = jStep.getJSONArray("street_names")
                            if (roadNames.length() > 0) {
                                roadName = roadNames.getString(0)
                            }
                        }
                        catch (e: JSONException)
                        {

                        }
                        node.mInstructions = buildInstructions(node.mManeuverType, roadName)
                        road.mNodes.add(node)
                    } //steps
                }
                //legs //routes
                //val endloc = jTrip.getJSONArray("locatons").getJSONObject(1)
                //val endpoint = GeoPoint(endloc.getDouble("lat"), startloc.getDouble("lon"))
                //road.mRouteHigh.add(endpoint)
                Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoads - finished")
                roads[0] = road
                roads
            } //if code is Ok
        } catch (e: JSONException) {
            e.printStackTrace()
            defaultRoad(waypoints)
        }
    }

    override fun getRoads(waypoints: ArrayList<GeoPoint>): Array<Road?> {
        return getRoads(waypoints, true)
    }

    override fun getRoad(waypoints: ArrayList<GeoPoint>): Road? {
        val roads: Array<Road?> = getRoads(waypoints, false)
        return roads[0]
    }

    protected fun getManeuverCode(direction: Int?): Int {
        val code: Int? = MANEUVERS.get(direction)
        return code ?: 0
    }

    protected fun buildInstructions(maneuver: Int, roadName: String): String? {
        val resDirection = DIRECTIONS.get(maneuver) as Int
        var direction = mContext.getString(resDirection)
        val instructions: String
        if (roadName == "") //remove "<*>"
            instructions = direction.replaceFirst("\\[[^\\]]*\\]".toRegex(), "") else {
            direction = direction.replace('[', ' ')
            direction = direction.replace(']', ' ')
            instructions = String.format(direction, roadName)
        }
        return instructions
    }

}

