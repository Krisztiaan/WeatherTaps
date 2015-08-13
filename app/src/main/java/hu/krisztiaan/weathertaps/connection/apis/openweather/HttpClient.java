package hu.krisztiaan.weathertaps.connection.apis.openweather;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class HttpClient {
    private static final String TAG = HttpClient.class.getName();

    public static final String LATITUDE = "{lat}";
    public static final String LONGITUDE = "{lon}";
    public static final String IMG_ID = "{img_id}";

    public static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?lat=" + LATITUDE + "&lon=" + LONGITUDE + "&units=metric";
    public static final String IMG_URL = "http://openweathermap.org/img/w/{img_id}.png";


    public String getWeatherData(double latitude, double longitude) {
        HttpURLConnection con = null;
        InputStream is = null;

        String genBaseUrl = BASE_URL.replace("{lat}", String.valueOf(latitude))
                .replace("{lon}", String.valueOf(longitude));

        try {
            Log.i(TAG, "getWeatherData requesting " + genBaseUrl);
            con = (HttpURLConnection) (new URL(genBaseUrl).openConnection());
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();

            StringBuilder buffer = new StringBuilder();
            is = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null)
                buffer.append(line).append("\r\n");

            is.close();
            con.disconnect();
            return buffer.toString();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Throwable ignored) {
            }
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}