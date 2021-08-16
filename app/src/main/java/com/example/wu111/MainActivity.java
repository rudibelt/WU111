package com.example.wu111;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static androidx.core.app.NotificationCompat.PRIORITY_LOW;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static final long SCAN_PERIOD = 5000;
    private ScanCallback mScanCallback;
    private ScanResultAdapter mAdapter;
    private static final String TAG = MainActivity.class.getSimpleName();

    private UUID SHIMANO_BICYCLE_INFORMATION = UUID.fromString("000018ef-5348-494d-414e-4f5f424c4500");
    private UUID INSTANTANEOUS_INFORMATION = UUID.fromString("00002ac2-5348-494d-414e-4f5f424c4500");



    //private BluetoothDevice device;
    private BluetoothLeScanner mBleScanner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        
        ListView listView = (ListView) findViewById(R.id.listView);
        mAdapter = new ScanResultAdapter(getApplicationContext(),
                LayoutInflater.from(this));
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = mAdapter.getDevice(position);
                if (device == null) return;
                Intent intent = new Intent(parent.getContext(), DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                //if (mScanning) {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    //mBluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) mScanCallback);
                    //mScanning = false;
                //}
                Intent notificationIntent = new Intent(parent.getContext(), BleService.class);
                notificationIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                notificationIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());


                startForegroundService(notificationIntent);

            }
        });


        if (savedInstanceState == null) {
            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    // Make sure we have access coarse location enabled, if not, prompt the user to enable it
                    if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("This app needs location access");
                        builder.setMessage("Please grant location access so this app can detect peripherals.");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.PERMISSION_REQUEST_COARSE_LOCATION);
                            }
                        });
                        builder.show();
                    }

                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    if (mBluetoothLeScanner != null) {
                        startScanning();
                        Log.d(TAG, "scan started");
                    }  else {
                        Log.e(TAG, "could not get scanner object");
                    }
                    // Are Bluetooth Advertisements supported on this device?
                    //if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {


                    //} else {

                    // Bluetooth Advertisements are not supported.
                    //    showErrorText(R.string.bt_ads_not_supported);
                    //}
                } else {

                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {

                // Bluetooth is not supported.
                showErrorText(R.string.bt_not_supported);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showErrorText(int messageId) {

        //TextView view = (TextView) findViewById(R.id.error_textview);
        //view.setText(getString(messageId));
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(new ParcelUuid(SHIMANO_BICYCLE_INFORMATION));
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES )
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L);
        return builder.build();
    }
    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

            // Will stop the scanning after a set time.
            //mHandler.postDelayed(new Runnable() {
            //   @Override
            //    public void run() {
            //        stopScanning();
            //    }
            //}, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.stopScan(mScanCallback);
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(),mScanCallback);

            String toastText = getString(R.string.scan_start_toast);
            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.already_scanning, Toast.LENGTH_SHORT).show();
        }
    }






    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //if (result.getDevice().getAddress().equals(mBluetoothDeviceAddress)) {
                //mMainHandler.post(mConnectRunnable);
                if (mBleScanner != null) {
                    mBleScanner.stopScan(mLeScanCallback);
                    mBleScanner = null;
                }
            //}
        }
    };

    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                mAdapter.add(result);
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            mAdapter.add(result);
            mAdapter.notifyDataSetChanged();

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getApplicationContext(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG).show();

        }


    }

}