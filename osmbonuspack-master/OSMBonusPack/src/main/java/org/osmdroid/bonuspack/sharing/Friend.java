package org.osmdroid.bonuspack.sharing;

import com.google.gson.JsonObject;

import org.osmdroid.util.GeoPoint;

/**
 * @author M.Kergall
 */
public class Friend {
    public String mId;
    public boolean mMyself;
    public String mNickName;
    public boolean mHasLocation;
    public GeoPoint mPosition;
    /**
     * azimuth in degrees
     */
    public float mBearing;
    public boolean mOnline;
    public String mMessage;

    public Friend(JsonObject joUser) {
        mId = joUser.get("id").getAsString();
        mMyself = joUser.get("myself").getAsBoolean();
        mNickName = joUser.get("nickname").getAsString();
        mHasLocation = (joUser.get("has_location").getAsInt() == 1);
        if (mHasLocation) {
            double lat = joUser.get("lat").getAsDouble();
            double lon = joUser.get("lon").getAsDouble();
            mPosition = new GeoPoint(lat, lon);
        } else
            mPosition = new GeoPoint(0.0, 0.0);
        mBearing = joUser.get("bearing").getAsFloat();
        mOnline = (joUser.get("online").getAsInt() == 1);
        if (joUser.has("message"))
            mMessage = joUser.get("message").getAsString();
        else
            mMessage = "";
    }
}
