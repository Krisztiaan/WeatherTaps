package hu.krisztiaan.weathertaps;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.net.ConnectException;

import hu.krisztiaan.weathertaps.connection.ConnectionManager;
import hu.krisztiaan.weathertaps.connection.OnWeatherUpdateListener;
import hu.krisztiaan.weathertaps.connection.network.NetworkChangeReceiver;
import hu.krisztiaan.weathertaps.data.Weather;
import hu.krisztiaan.weathertaps.fragments.BlankFragment;
import hu.krisztiaan.weathertaps.fragments.ErrorFragment;
import hu.krisztiaan.weathertaps.fragments.InfoFragment;

public class MapsActivity extends Activity implements GoogleMap.OnMapClickListener,
        OnWeatherUpdateListener, NetworkChangeReceiver.NetworkChangeListener, AutoHideListener {
    public static final String TAG = MapsActivity.class.getSimpleName();

    private static final int PLAY_SERVICES_REQUEST = 999;
    private static final float OPTIMAL_ZOOM = 8;
    private static final int INFO_DISPLAY_TIMEOUT = 5000;
    private static final Handler mInfoCardTimeoutHandler = new Handler();
    private static BroadcastReceiver mNetworkChangeReceiver;
    private InfoFragment mInfoFragment;
    private ErrorFragment mErrorFragment;
    private BlankFragment mBlankFragment;
    private GoogleMap mMap;
    private Marker mSingleMarker;
    private boolean errorDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpFragments();
        checkPlayServices();
        setUpMapIfNeeded();
        ConnectionManager.init(getApplicationContext());
        ConnectionManager.setOnDataReceivedListener(this);

        mNetworkChangeReceiver = new NetworkChangeReceiver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnection();
        registerReceiver(mNetworkChangeReceiver,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        setUpMapIfNeeded();
        if (mInfoFragment.isAdded()) {
            mSingleMarker = mMap.addMarker(new MarkerOptions().position(mInfoFragment.getWeather().latLng));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInfoCardTimeoutHandler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(mNetworkChangeReceiver);
        } catch (Exception ignored) {
        }
    }

    private void setUpFragments() {
        mErrorFragment = new ErrorFragment();
        mBlankFragment = new BlankFragment();

        mInfoFragment = (InfoFragment) getFragmentManager().findFragmentByTag(InfoFragment.getFragmentTag());
        if (mInfoFragment == null) {
            mInfoFragment = new InfoFragment();
            showBlankFragment();
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_REQUEST).show();
            } else {
                new MaterialDialog.Builder(MapsActivity.this)
                        .theme(Theme.LIGHT)
                        .title(R.string.play_error_title)
                        .content(R.string.play_error_body)
                        .positiveText(R.string.error_button_ok)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (mMap != null) {
                mMap.setOnMapClickListener(this);
                mMap.setMyLocationEnabled(true);
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        onMapClick(marker.getPosition());
                        return true;
                    }
                });
            }
        }
    }

    private void showBlankFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.info_fragment_container, mBlankFragment);
        transaction.commit();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (ConnectionManager.isOnline()) {
            showInfoDisplay();
            // approximate the zoom level to optimal
            float zoomLevel = mMap.getCameraPosition().zoom <= OPTIMAL_ZOOM ?
                    (OPTIMAL_ZOOM - (OPTIMAL_ZOOM - mMap.getCameraPosition().zoom) / 2)
                    : mMap.getCameraPosition().zoom;

            mMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(new CameraPosition.Builder()
                            .target(latLng)
                            .zoom(zoomLevel)
                            .build()));
            if (mSingleMarker != null) {
                mSingleMarker.remove();
            }
            mSingleMarker = mMap.addMarker(new MarkerOptions().position(latLng));
            ConnectionManager.requestWeatherData(latLng);
        } else {
            onNetworkChange(false);
        }
    }

    private void showInfoDisplay() {
        Log.i(TAG, "showInfoDisplay showing card if not added. isAdded()=" + mInfoFragment.isAdded());
        if (!mInfoFragment.isAdded() && !isFinishing()) {
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                    .replace(R.id.info_fragment_container, mInfoFragment, InfoFragment.getFragmentTag())
                    .commit();
        } else {
            mInfoFragment.notifyLoad();
        }
    }

    @Override
    public void onNetworkChange(boolean isOnline) {
        checkConnection();
    }

    public void checkConnection() {
        if (ConnectionManager.isOnline()) {
            if (mErrorFragment.isAdded())
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                        .replace(R.id.info_fragment_container, mBlankFragment).commit();
        } else {
            showConnectionError();
        }
    }

    public void showConnectionError() {
        if (!errorDisplayed) {
            errorDisplayed = true;
            new MaterialDialog.Builder(this)
                    .theme(Theme.LIGHT)
                    .title(R.string.noconn_title)
                    .content(R.string.noconn_body)
                    .positiveText(R.string.noconn_button_settings)
                    .negativeText(R.string.noconn_button_cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            errorDisplayed = false;
                            Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                            startActivity(i);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            super.onNegative(dialog);
                            errorDisplayed = false;
                            MapsActivity.this.getFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                                    .replace(R.id.info_fragment_container, mErrorFragment)
                                    .commit();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onWeatherData(Weather weatherData) {
        showInfoDisplay();
        mInfoFragment.revealNewInfo(weatherData);
    }

    @Override
    public void onRequestError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof ConnectException) showConnectionError();
                else {
                    new MaterialDialog.Builder(MapsActivity.this)
                            .theme(Theme.LIGHT)
                            .title(R.string.error_title)
                            .content(R.string.error_body)
                            .positiveText(R.string.error_button_ok)
                            .show();
                }
            }
        });
    }

    @Override
    public void stopAutoHide() {
        mInfoCardTimeoutHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void startAutoHide() {
        Log.i(TAG, "startAutoHide called");
        mInfoCardTimeoutHandler.removeCallbacksAndMessages(null);
        mInfoCardTimeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run autoHide");
                hideInfoDisplay();
                if (mSingleMarker != null) {
                    mSingleMarker.remove();
                }
            }
        }, INFO_DISPLAY_TIMEOUT);
    }

    private void hideInfoDisplay() {
        mInfoCardTimeoutHandler.removeCallbacksAndMessages(null);
        Log.i(TAG, "hideInfoDisplay hiding card");
        if (mBlankFragment.isAdded()) {
            getFragmentManager().beginTransaction()
                    .remove(mBlankFragment)
                    .commit();
        }
        mInfoFragment.notifyPrepareHide();
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .replace(R.id.info_fragment_container, mBlankFragment)
                .commit();
    }
}