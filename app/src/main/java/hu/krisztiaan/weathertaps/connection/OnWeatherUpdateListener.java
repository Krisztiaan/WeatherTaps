package hu.krisztiaan.weathertaps.connection;

import hu.krisztiaan.weathertaps.data.Weather;

public interface OnWeatherUpdateListener {
    void onWeatherData(Weather weatherData);

    void onRequestError(Exception e);
}
