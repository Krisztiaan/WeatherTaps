package hu.krisztiaan.weathertaps.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {
    public static final String TAG = NetworkChangeReceiver.class.getSimpleName();
    public static NetworkChangeListener sListener;

    public NetworkChangeReceiver(NetworkChangeListener listener) {
        sListener = listener;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        int status = NetworkUtil.getConnectivityStatusString(context);
        Log.i(TAG, "onReceive changed status: " + status);
        if (!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            sListener.onNetworkChange(status != NetworkUtil.NETWORK_STATUS_NOT_CONNECTED);
        }
    }

    public interface NetworkChangeListener {
        void onNetworkChange(boolean isOnline);
    }
}
