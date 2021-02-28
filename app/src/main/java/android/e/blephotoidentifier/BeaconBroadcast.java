package android.e.blephotoidentifier;

import android.app.AlertDialog;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;


public class BeaconBroadcast extends IntentService {

    protected static final String TAG = "BeaconBroadcast";
    private static final ParcelUuid SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

    private BluetoothManager manager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeAdvertiser adv;
    private AdvertiseSettings advertiseSettings;
    private AdvertiseCallback advertiseCallback;

    AdvertisingSetParameters.Builder parameters = (new AdvertisingSetParameters.Builder())
            .setLegacyMode(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setSecondaryPhy(BluetoothDevice.PHY_LE_2M);

    AdvertisingSetCallback callback = new AdvertisingSetCallback() {
        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
            Log.i("BLE 5", "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                    + status);
        }

        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
            Log.i("BLE 5", "onAdvertisingSetStopped():");
        }
    };

    public BeaconBroadcast() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeBT();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String name = intent.getStringExtra("name");
            String shirt = intent.getStringExtra("shirt");
            String pants = intent.getStringExtra("pants");
            String bcStr = name + "*" + shirt + "*" + pants;
            Fragmenter.advertise(adv,245,bcStr.getBytes(),SERVICE_UUID,parameters.build(),callback);
        }
    }

    private void initializeBT(){
        manager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();
        if (btAdapter == null) {
            Log.e("Bluetooth Error", "Bluetooth not detected on device");
        } else if (!btAdapter.isEnabled()) {
            Log.e("Error","Need Bluetooth Permissions");
        } else if (!btAdapter.isMultipleAdvertisementSupported()) {
            Log.e("Not supported", "BLE advertising not supported on this device");
        }
        adv = btAdapter.getBluetoothLeAdvertiser();
        if (!btAdapter.isLe2MPhySupported()) {
            Log.e("BLE 5", "2M PHY not supported!");
            return;
        }
        if (!btAdapter.isLeExtendedAdvertisingSupported()) {
            Log.e("BLE 5", "LE Extended Advertising not supported!");
            return;
        }
        advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();
        advertiseCallback = createAdvertiseCallback();
    }

    private AdvertiseCallback createAdvertiseCallback() {
        return new AdvertiseCallback() {

            @Override

            public void onStartFailure(int errorCode) {
                switch (errorCode) {
                    case ADVERTISE_FAILED_DATA_TOO_LARGE:
                        Log.e("Failure","ADVERTISE_FAILED_DATA_TOO_LARGE");
                        break;
                    case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        Log.e("Failure","ADVERTISE_FAILED_TOO_MANY_ADVERTISERS");
                        break;
                    case ADVERTISE_FAILED_ALREADY_STARTED:
                        Log.e("Failure","ADVERTISE_FAILED_ALREADY_STARTED");
                        break;
                    case ADVERTISE_FAILED_INTERNAL_ERROR:
                        Log.e("Failure","ADVERTISE_FAILED_INTERNAL_ERROR");
                        break;
                    case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        Log.e("Failure","ADVERTISE_FAILED_FEATURE_UNSUPPORTED");
                        break;
                    default:
                        Log.e("Failure","startAdvertising failed with unknown error " + errorCode);
                        break;
                }
            }

        };
    }

}
