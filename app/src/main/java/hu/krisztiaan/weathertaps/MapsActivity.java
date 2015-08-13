package hu.krisztiaan.weathertaps;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
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
import hu.krisztiaan.weathertaps.connection.NetworkChangeReceiver;
import hu.krisztiaan.weathertaps.connection.OnWeatherUpdateListener;
import hu.krisztiaan.weathertaps.data.Weather;
import hu.krisztiaan.weathertaps.fragments.BlankFragment;
import hu.krisztiaan.weathertaps.fragments.ErrorFragment;
import hu.krisztiaan.weathertaps.fragments.InfoFragment;

public class MapsActivity extends Activity implements GoogleMap.OnMapClickListener,
        OnWeatherUpdateListener, NetworkChangeReceiver.NetworkChangeListener, AutoHideListener {
    public static final String TAG = MapsActivity.class.getSimpleName();

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 999;
    private static final float OPTIMAL_ZOOM = 8;
    private static final int INFO_DISPLAY_TIMEOUT = 5000;

    private BroadcastReceiver mNetworkChangeReceiver;
    private final InfoFragment mInfoFragment = new InfoFragment();
    private final ErrorFragment mErrorFragment = new ErrorFragment();
    private final BlankFragment mBlankFragment = new BlankFragment();
    private final Handler infoCardTimeoutHandler = new Handler();
    private GoogleMap mMap;
    private Marker mSingleMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        checkPlayServices();
        setUpMapIfNeeded();
        ConnectionManager.init(getApplicationContext());
        ConnectionManager.setOnDataReceivedListener(this);
        moveToLastLocation();
        mNetworkChangeReceiver = new NetworkChangeReceiver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnection();
        registerReceiver(mNetworkChangeReceiver,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        infoCardTimeoutHandler.removeCallbacksAndMessages(null);
        hideInfoDisplay();
        try {unregisterReceiver(mNetworkChangeReceiver);}
        catch (Exception ignored) {}
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (mMap != null) {
                mMap.setOnMapClickListener(this);
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if(ConnectionManager.isOnline()) {
            initInfoDisplay();
            // approximate the zoom level to optimal
            float zoomLevel = mMap.getCameraPosition().zoom <= OPTIMAL_ZOOM ?
                    (OPTIMAL_ZOOM - (OPTIMAL_ZOOM - mMap.getCameraPosition().zoom)/2)
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
        }
        else{
            onNetworkChange(false);
        }
    }

    private void moveToLastLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), OPTIMAL_ZOOM));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                    .zoom(OPTIMAL_ZOOM - 5)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }
    }

    private void initInfoDisplay() {
        Log.i(TAG, "initInfoDisplay showing card if not added. isAdded()=" + mInfoFragment.isAdded());
        if (!mInfoFragment.isAdded()) {
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                    .replace(R.id.info_fragment_container, mInfoFragment)
                    .commit();
        } else {
            mInfoFragment.notifyLoad();
        }
    }

    private void setAutoHide() {
        Log.i(TAG, "setAutoHide called");
        infoCardTimeoutHandler.removeCallbacksAndMessages(null);
        infoCardTimeoutHandler.postDelayed(new Runnable() {
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
        infoCardTimeoutHandler.removeCallbacksAndMessages(null);
        Log.i(TAG, "hideInfoDisplay hiding card if added. isAdded()=" + mInfoFragment.isAdded());
        if (mInfoFragment.isAdded()) {
            mInfoFragment.prepareHide();
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                    .replace(R.id.info_fragment_container, mBlankFragment)
                    .commit();
        }
    }

    @Override
    public void onWeatherData(Weather weatherData) {
        initInfoDisplay();
        mInfoFragment.revealNewInfo(weatherData);
        setAutoHide();
    }

    @Override
    public void onRequestError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof ConnectException) checkConnection();
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

    public void checkConnection() {
        if(ConnectionManager.isOnline()) {
            if (mErrorFragment.isAdded())
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                        .replace(R.id.info_fragment_container, mBlankFragment).commit();
        } else {
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
                            Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                            startActivity(i);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            super.onNegative(dialog);
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
    public void onNetworkChange(boolean isOnline) {
        checkConnection();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
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

    @Override
    public void postponeAutoHide() {
        infoCardTimeoutHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void resumeAutoHide() {
        setAutoHide();
    }
}