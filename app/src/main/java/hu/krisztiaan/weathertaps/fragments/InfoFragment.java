package hu.krisztiaan.weathertaps.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hu.krisztiaan.weathertaps.AutoHideListener;
import hu.krisztiaan.weathertaps.R;
import hu.krisztiaan.weathertaps.data.Weather;

public class InfoFragment extends Fragment {
    public static final int INFO_REVEAL = 1;
    public static final int INFO_HIDE = 0;
    private static final String TAG = InfoFragment.class.getSimpleName();

    @Bind(R.id.txt_city_name)
    TextView txtCity;
    @Bind(R.id.txt_temperature)
    TextView txtTemperature;
    @Bind(R.id.img_weather_icon)
    ImageView imgWeatherIcon;
    @Bind(R.id.txt_wind)
    TextView txtWind;
    @Bind(R.id.txt_humidity)
    TextView txtHumidity;
    @Bind(R.id.txt_pressure)
    TextView txtPressure;

    @Bind(R.id.info_card_basic)
    RelativeLayout rvContent;
    @Bind(R.id.progress_wheel)
    ProgressWheel pvProgress;
    @Bind(R.id.info_card_details)
    LinearLayout lvDetails;
    @Bind(R.id.txt_more)
    TextView txtMore;

    private AutoHideListener hideListener;
    private Weather mWeather;
    private boolean isDetailsDisplayed = false;

    public InfoFragment() {
    }

    public static String getFragmentTag() {
        return TAG;
    }

    @OnClick(R.id.info_card_container)
    public void onCardClick() {
        if (rvContent.getVisibility() == View.GONE) return;
        if (lvDetails.getVisibility() == View.GONE) {
            showDetails(true);
        } else {
            showDetails(false);
        }
    }

    private void showDetails(boolean show) {
        if (show && lvDetails.getVisibility() == View.GONE) {
            if (hideListener != null)
                hideListener.stopAutoHide();
            txtMore.setVisibility(View.GONE);
            lvDetails.setVisibility(View.VISIBLE);
            isDetailsDisplayed = true;
        } else if (!show && lvDetails.getVisibility() == View.VISIBLE) {
            if (hideListener != null)
                hideListener.startAutoHide();
            lvDetails.setVisibility(View.GONE);
            txtMore.setVisibility(View.VISIBLE);
            isDetailsDisplayed = false;
        }
    }

    public void revealNewInfo(Weather weather) {
        mWeather = weather;
        updateInfoView(INFO_REVEAL);
    }

    public synchronized void updateInfoView(final int direction) {
        switch (direction) {
            case INFO_HIDE:
                if (hideListener != null)
                    hideListener.stopAutoHide();
                showDetails(false);
                rvContent.setVisibility(View.GONE);
                pvProgress.setVisibility(View.VISIBLE);
                break;
            case INFO_REVEAL:
                if (hideListener != null)
                    hideListener.startAutoHide();
                setUpData();
                pvProgress.setVisibility(View.GONE);
                rvContent.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setUpData() {
        if (mWeather == null) throw new NullPointerException("no data was preloaded");
        Log.i(TAG, "setUpData for city: " + mWeather.location.getCity());
        Picasso.with(getActivity()).load(mWeather.currentCondition.getIconUrl()).into(imgWeatherIcon);
        txtCity.setText(mWeather.location.getCity());
        txtTemperature.setText(subInvalidData(mWeather.temperature.getTemp())
                + getString(R.string.deg_celsius));
        txtWind.setText(getString(R.string.wind)
                + subInvalidData(mWeather.wind.getSpeed())
                + getString(R.string.kmph));
        txtHumidity.setText(getString(R.string.humidity)
                + subInvalidData(mWeather.currentCondition.getHumidity())
                + getString(R.string.percent));
        txtPressure.setText(getString(R.string.pressure)
                + subInvalidData(mWeather.currentCondition.getPressure())
                + getString(R.string.hpa));
    }

    private String subInvalidData(float input) {
        if (input == -1) return getString(R.string.no_data);
        else return Float.toString(round(input, 1));
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public void notifyPrepareHide() {
        mWeather = null;
    }

    public void notifyLoad() {
        updateInfoView(INFO_HIDE);
    }

    public Weather getWeather() {
        return mWeather;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        hideListener = (AutoHideListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_info, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWeather != null) {
            updateInfoView(INFO_REVEAL);
            showDetails(isDetailsDisplayed);
        } else {
            updateInfoView(INFO_HIDE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWeather == null) {
            updateInfoView(INFO_HIDE);
        }
    }
}
