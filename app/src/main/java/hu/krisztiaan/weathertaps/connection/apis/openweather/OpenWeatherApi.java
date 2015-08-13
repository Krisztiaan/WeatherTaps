package hu.krisztiaan.weathertaps.connection.apis.openweather;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.net.ConnectException;

import hu.krisztiaan.weathertaps.connection.WeatherApi;
import hu.krisztiaan.weathertaps.data.Weather;

public class OpenWeatherApi extends WeatherApi {
    public static final String TAG = OpenWeatherApi.class.getSimpleName();

    private static OpenWeatherApi sMOpenWeatherApi;

    private OpenWeatherApi() {
    }

    public static OpenWeatherApi getInstance() {
        if (sMOpenWeatherApi == null) {
            sMOpenWeatherApi = new OpenWeatherApi();
        }
        return sMOpenWeatherApi;
    }

    @Override
    public void requestWeatherData(LatLng latLng) {
        super.requestWeatherData(latLng);
        new JSONWeatherTask().execute(latLng);
    }

    private class JSONWeatherTask extends AsyncTask<LatLng, Void, Weather> {

        @Override
        protected Weather doInBackground(LatLng... params) {
            Weather weather;
            String data = ((new HttpClient()).getWeatherData(params[0].latitude, params[0].longitude));
            if (data != null) {
                try {
                    weather = JSONWeatherParser.getWeather(data);
                    weather.latLng = params[0];
                } catch (JSONException e) {
                    e.printStackTrace();
                    onError(e);
                    return null;
                }
                return weather;
            } else {
                onError(new ConnectException("Failed to get data."));
                return null;
            }
        }

        @Override
        protected void onPostExecute(Weather weather) {
            super.onPostExecute(weather);
            if (weather != null) {
                weather.currentCondition.setIconUrl(HttpClient.IMG_URL.replace(HttpClient.IMG_ID,
                        weather.currentCondition.getIcon()));
                onSuccess(weather);
            }
        }
    }
}