package hu.krisztiaan.weathertaps.connection;

import com.google.android.gms.maps.model.LatLng;

import hu.krisztiaan.weathertaps.data.Weather;

public abstract class WeatherApi {
    public static final String KEY_PLACE_NAME = "place_name";
    public static final String KEY_TEMPERATURE = "temperature";
    public static final String KEY_WIND_SPEED = "wind_speed";
    public static final String KEY_AIR_PRESSURE = "air_pressure";
    public static final String KEY_HUMIDITY = "humidity";

    private static LatLng lastQuery;

    public void requestWeatherData(LatLng latLng) {
        lastQuery = latLng;
    }

    protected final void onSuccess(Weather weatherData) {
        if(weatherData.latLng==lastQuery) ConnectionManager.onWeatherReady(weatherData);
    }

    protected final void onError(Exception e) {
        ConnectionManager.onRequestError(e);
    }
}
