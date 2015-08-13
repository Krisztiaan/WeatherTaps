package hu.krisztiaan.weathertaps.data;

public class TempConverter {
    public static float fahrenheitToKelvin(float degFarenheit) {
        return celsiusToKelvin(fahrenheitToCelsius(degFarenheit));
    }

    public static float celsiusToKelvin(float degCelcius) {
        return degCelcius + 273.15f;
    }

    public static float fahrenheitToCelsius(float degFahrenheit) {
        return (degFahrenheit - 32) * 5 / 9;
    }

    public static float kelvinToFahrenheit(float degKelvin) {
        return celsiusToFahrenheit(kelvinToCelcius(degKelvin));
    }

    public static float celsiusToFahrenheit(float degCelsius) {
        return degCelsius * 9 / 5 + 32;
    }

    public static float kelvinToCelcius(float degKelvin) {
        return degKelvin - 273.15f;
    }
}
