package hu.krisztiaan.weathertaps.fragments;

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
    public static final int INFO_REPLACE = 2;
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

    @Bind(R.id.info_card_content_layout)
    RelativeLayout rvContent;
    @Bind(R.id.progress_wheel)
    ProgressWheel pvProgress;
    @Bind(R.id.rv_in_card)
    RelativeLayout rvContainer;
    @Bind(R.id.lv_details)
    LinearLayout lvDetails;
    @Bind(R.id.txt_more)
    TextView txtMore;

    private AutoHideListener hideListener;
    private Weather mWeather;

    public InfoFragment() {
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private String subInvalidData(float input) {
        if (input == -1) return getActivity().getString(R.string.no_data);
        else return Float.toString(round(input, 1));
    }

    @OnClick(R.id.info_card_view)
    public void onCardClick() {
        if (lvDetails.getParent() == null) {
            showDetails(true);
        } else {
            showDetails(false);
        }
    }

    private void showDetails(boolean show) {
        if (show && lvDetails.getParent() == null) {
            if (hideListener != null)
                hideListener.postponeAutoHide();
            rvContainer.addView(lvDetails);
            txtMore.setVisibility(View.GONE);

        } else {
            if (hideListener != null)
                hideListener.resumeAutoHide();
            rvContainer.removeView(lvDetails);
            txtMore.setVisibility(View.VISIBLE);
        }
    }

    public void revealNewInfo(Weather weather) {
        mWeather = weather;
        updateInfoView(INFO_REVEAL);
    }

    public void prepareHide() {
        mWeather = null;
        updateInfoView(INFO_HIDE);
    }

    public void notifyLoad() {
        updateInfoView(INFO_HIDE);
    }

    public void updateInfoView(final int direction) {
        rvContainer.removeAllViews();
        switch (direction) {
            case INFO_HIDE:
                showDetails(false);
                rvContainer.addView(pvProgress, 0);
                break;
            case INFO_REVEAL:
                setUpData();
                rvContainer.addView(rvContent, 0);
                break;
            case INFO_REPLACE:
                updateInfoView(INFO_HIDE);
                updateInfoView(INFO_REVEAL);
                break;
            default:
                throw new IllegalArgumentException("direction must be INFO_HIDE, INFO_REVEAL, or INFO_REPLACE");
        }
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
        if (getActivity() instanceof AutoHideListener)
            hideListener = (AutoHideListener) getActivity();
        if (mWeather != null) {
            updateInfoView(INFO_REVEAL);
            setUpData();
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

    private void setUpData() {
        if (mWeather == null) throw new NullPointerException("no data was preloaded");
        Log.i(TAG, "setUpData for city: " + mWeather.location.getCity());
        Picasso.with(getActivity()).load(mWeather.currentCondition.getIconUrl()).into(imgWeatherIcon);
        txtCity.setText(mWeather.location.getCity());
        txtTemperature.setText(subInvalidData(mWeather.temperature.getTemp())
                + getActivity().getString(R.string.deg_celsius));
        txtWind.setText(getActivity().getString(R.string.wind)
                + subInvalidData(mWeather.wind.getSpeed())
                + getActivity().getString(R.string.kmph));
        txtHumidity.setText(getActivity().getString(R.string.humidity)
                + subInvalidData(mWeather.currentCondition.getHumidity())
                + getActivity().getString(R.string.percent));
        txtPressure.setText(getActivity().getString(R.string.pressure)
                + subInvalidData(mWeather.currentCondition.getPressure())
                + getActivity().getString(R.string.hpa));
    }
}
