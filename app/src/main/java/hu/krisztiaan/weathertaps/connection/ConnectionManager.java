package hu.krisztiaan.weathertaps.connection;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.net.ConnectException;

import hu.krisztiaan.weathertaps.connection.apis.WeatherApi;
import hu.krisztiaan.weathertaps.connection.apis.openweather.OpenWeatherApi;
import hu.krisztiaan.weathertaps.connection.network.NetworkUtil;
import hu.krisztiaan.weathertaps.data.Weather;

public class ConnectionManager {
    public static final String TAG = ConnectionManager.class.getSimpleName();

    private static final WeatherApi mWeatherApi = OpenWeatherApi.getInstance();
    private static Context mContext;
    private static OnWeatherUpdateListener mListener;

    private static LatLng tempLatLng;

    public static void init(Context context) {
        mContext = context;
    }

    public static void onWeatherReady(Weather weatherData) {
        if (weatherData.latLng.equals(tempLatLng)) {
            Log.i(TAG, "onWeatherReady calling back with weather data: " + weatherData.toString());
            mListener.onWeatherData(weatherData);
            tempLatLng = null;
        }
    }

    public static void onRequestError(Exception e) {
            e.printStackTrace();
            mListener.onRequestError(e);
    }

    public static void requestWeatherData(LatLng latLng) {
        Log.i(TAG, "requestWeatherData run");
        if (!latLng.equals(tempLatLng)) {
            tempLatLng = latLng;
        }
        if (isOnline()) {
            mWeatherApi.requestWeatherData(latLng);
        } else {
            mListener.onRequestError(new ConnectException("No connection!"));
        }
    }

    public static boolean isOnline() {
        return NetworkUtil.getConnectivityStatus(mContext) != NetworkUtil.NETWORK_STATUS_NOT_CONNECTED;
    }

    public static void setOnDataReceivedListener(OnWeatherUpdateListener listener) {
        mListener = listener;
    }

    /*
    private static class Retrier {
        static final int MAX_TRY = 3;
        static int tryCount = 0;
        static LatLng mLatLng;

        protected static boolean retry(LatLng latLng) {
            if (latLng == null) {
                stop();
                return true;
            }

            if (!mLatLng.equals(latLng)) {
                tryCount = 0;
            }

            if (tryCount++ <= MAX_TRY) {
                requestWeatherData(latLng);
                return true;
            } else {
                return false;
            }
        }

        protected static void stop() {
            mLatLng = null;
            tryCount = MAX_TRY + 1;
        }
    }
    */
}
