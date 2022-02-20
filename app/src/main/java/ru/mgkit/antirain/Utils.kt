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

class ValhalaRoadManager(private val mContext: Context, protected var mUserAgent: String) :
    RoadManager() {

        //"http://localhost:8002/route --data '{"locations":[{"lat":55.7074932,"lon":37.5690340},{"lat":55.7175925,"lon":37.5496478}],"costing":"bicycle","directions_type":"maneuvers"}' | jq '.'"

    companion object {
        //static final String DEFAULT_SERVICE = "https://routing.openstreetmap.de/";
        const val MEAN_BY_CAR = "routed-car/route/v1/driving/"
        const val MEAN_BY_BIKE = "routed-bike/route/v1/driving/"
        const val MEAN_BY_FOOT = "routed-foot/route/v1/driving/"
        const val mServiceUrl: String = "http://127.0.0.1:8002/"
        /**
         * mapping from OSRM StepManeuver types to MapQuest maneuver IDs:
         */
        val MANEUVERS: HashMap<String, Int> = HashMap<String, Int>()

        //From: Project-OSRM-Web / WebContent / localization / OSRM.Locale.en.js
        // driving directions
        // %s: road name
        // %d: direction => removed
        // <*>: will only be printed when there actually is a road name
        val DIRECTIONS: HashMap<Int, Any> = HashMap<Int, Any>()

        init {
            MANEUVERS.put("new name", 2) //road name change
            MANEUVERS.put("turn-straight", 1) //Continue straight
            MANEUVERS.put("turn-slight right", 6) //Slight right
            MANEUVERS.put("turn-right", 7) //Right
            MANEUVERS.put("turn-sharp right", 8) //Sharp right
            MANEUVERS.put("turn-uturn", 12) //U-turn
            MANEUVERS.put("turn-sharp left", 5) //Sharp left
            MANEUVERS.put("turn-left", 4) //Left
            MANEUVERS.put("turn-slight left", 3) //Slight left
            MANEUVERS.put("depart", 24) //"Head" => used by OSRM as the start node. Considered here as a "waypoint".
            // TODO - to check...
            MANEUVERS.put("arrive", 24) //Arrived (at waypoint)
            MANEUVERS.put("roundabout-1", 27) //Round-about, 1st exit
            MANEUVERS.put("roundabout-2", 28) //2nd exit, etc ...
            MANEUVERS.put("roundabout-3", 29)
            MANEUVERS.put("roundabout-4", 30)
            MANEUVERS.put("roundabout-5", 31)
            MANEUVERS.put("roundabout-6", 32)
            MANEUVERS.put("roundabout-7", 33)
            MANEUVERS.put("roundabout-8", 34) //Round-about, 8th exit
            //TODO: other OSRM types to handle properly:
            MANEUVERS.put("merge-left", 20)
            MANEUVERS.put("merge-sharp left", 20)
            MANEUVERS.put("merge-slight left", 20)
            MANEUVERS.put("merge-right", 21)
            MANEUVERS.put("merge-sharp right", 21)
            MANEUVERS.put("merge-slight right", 21)
            MANEUVERS.put("merge-straight", 22)
            MANEUVERS.put("ramp-left", 17)
            MANEUVERS.put("ramp-sharp left", 17)
            MANEUVERS.put("ramp-slight left", 17)
            MANEUVERS.put("ramp-right", 18)
            MANEUVERS.put("ramp-sharp right", 18)
            MANEUVERS.put("ramp-slight right", 18)
            MANEUVERS.put("ramp-straight", 19)
            //MANEUVERS.put("fork", );
            //MANEUVERS.put("end of road", );
            //MANEUVERS.put("continue", );

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

    /** allows to request on an other site than OSRM demo site  */
    fun setService(serviceUrl: String) {
        mServiceUrl = serviceUrl
    }

    /** to switch to another mean of transportation  */
    fun setMean(meanUrl: String) {
        mMeanUrl = meanUrl
    }

    protected fun getUrl(waypoints: ArrayList<GeoPoint>, getAlternate: Boolean): String {
        val urlString = StringBuilder(mServiceUrl + mMeanUrl)
        for (i in waypoints.indices) {
            val p = waypoints[i]
            if (i > 0) urlString.append(';')
            urlString.append(geoPointAsLonLatString(p))
        }
        urlString.append("?alternatives=" + if (getAlternate) "true" else "false")
        urlString.append("&overview=full&steps=true")
        urlString.append(mOptions)
        return urlString.toString()
    }

    protected fun defaultRoad(waypoints: ArrayList<GeoPoint>?): Array<Road?> {
        val roads = arrayOfNulls<Road>(1)
        roads[0] = Road(waypoints)
        return roads
    }

    protected fun getRoads(waypoints: ArrayList<GeoPoint>, getAlternate: Boolean): Array<Road?> {
        val url = getUrl(waypoints, getAlternate)
        Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoads:$url")
        val jString = BonusPackHelper.requestStringFromUrl(url, mUserAgent)
        if (jString == null) {
            Log.e(BonusPackHelper.LOG_TAG, "OSRMRoadManager::getRoad: request failed.")
            return defaultRoad(waypoints)
        }
        return try {
            val jObject = JSONObject(jString)
            val jCode = jObject.getString("code")
            if ("Ok" != jCode) {
                Log.e(
                    BonusPackHelper.LOG_TAG,
                    "OSRMRoadManager::getRoad: error code=$jCode"
                )
                val roads = defaultRoad(waypoints)
                if ("NoRoute" == jCode) {
                    roads[0]!!.mStatus = Road.STATUS_INVALID
                }
                roads
            } else {
                val jRoutes = jObject.getJSONArray("routes")
                val roads = arrayOfNulls<Road>(jRoutes.length())
                for (i in 0 until jRoutes.length()) {
                    val road = Road()
                    roads[i] = road
                    road.mStatus = Road.STATUS_OK
                    val jRoute = jRoutes.getJSONObject(i)
                    val route_geometry = jRoute.getString("geometry")
                    road.mRouteHigh = PolylineEncoder.decode(route_geometry, 10, false)
                    road.mBoundingBox = BoundingBox.fromGeoPoints(road.mRouteHigh)
                    road.mLength = jRoute.getDouble("distance") / 1000.0
                    road.mDuration = jRoute.getDouble("duration")
                    //legs:
                    val jLegs = jRoute.getJSONArray("legs")
                    for (l in 0 until jLegs.length()) {
                        //leg:
                        val jLeg = jLegs.getJSONObject(l)
                        val leg = RoadLeg()
                        road.mLegs.add(leg)
                        leg.mLength = jLeg.getDouble("distance")
                        leg.mDuration = jLeg.getDouble("duration")
                        //steps:
                        val jSteps = jLeg.getJSONArray("steps")
                        var lastNode: RoadNode? = null
                        var lastRoadName = ""
                        for (s in 0 until jSteps.length()) {
                            val jStep = jSteps.getJSONObject(s)
                            val node = RoadNode()
                            node.mLength = jStep.getDouble("distance") / 1000.0
                            node.mDuration = jStep.getDouble("duration")
                            val jStepManeuver = jStep.getJSONObject("maneuver")
                            val jLocation = jStepManeuver.getJSONArray("location")
                            node.mLocation =
                                GeoPoint(jLocation.getDouble(1), jLocation.getDouble(0))
                            var direction = jStepManeuver.getString("type")
                            if (direction == "turn" || direction == "ramp" || direction == "merge") {
                                val modifier = jStepManeuver.getString("modifier")
                                direction = "$direction-$modifier"
                            } else if (direction == "roundabout") {
                                val exit = jStepManeuver.getInt("exit")
                                direction = "$direction-$exit"
                            } else if (direction == "rotary") {
                                val exit = jStepManeuver.getInt("exit")
                                direction = "roundabout-$exit" //convert rotary in roundabout...
                            }
                            node.mManeuverType = getManeuverCode(direction)
                            val roadName = jStep.optString("name", "")
                            node.mInstructions = buildInstructions(node.mManeuverType, roadName)
                            if (lastNode != null && node.mManeuverType == 2 && lastRoadName == roadName) {
                                //workaround for https://github.com/Project-OSRM/osrm-backend/issues/2273
                                //"new name", but identical to previous name:
                                //skip, but update values of last node:
                                lastNode.mDuration += node.mDuration
                                lastNode.mLength += node.mLength
                            } else {
                                road.mNodes.add(node)
                                lastNode = node
                                lastRoadName = roadName
                            }
                        } //steps
                    } //legs
                } //routes
                Log.d(BonusPackHelper.LOG_TAG, "OSRMRoadManager.getRoads - finished")
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

    protected fun getManeuverCode(direction: String?): Int {
        val code: Int? = MANEUVERS.get(direction)
        return code ?: 0
    }

    protected fun buildInstructions(maneuver: Int, roadName: String): String? {
        val resDirection = DIRECTIONS.get(maneuver) as Int
            ?: return null
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

