package com.example.wu111;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ServiceCompat;

import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static androidx.core.app.ServiceCompat.stopForeground;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static final long SCAN_PERIOD = 5000;
    private ScanCallback mScanCallback;
    private ScanResultAdapter mAdapter;
    private static final String TAG = MainActivity.class.getSimpleName();

    private UUID SHIMANO_BICYCLE_INFORMATION = UUID.fromString("000018ef-5348-494d-414e-4f5f424c4500");
    private UUID INSTANTANEOUS_INFORMATION = UUID.fromString("00002ac2-5348-494d-414e-4f5f424c4500");

    private ConnectionEventListener myEventListener = new MyEventListener();
    private BluetoothDevice device;
    private Intent notificationIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Log.d(TAG,"OnCreate");
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ConnectionManager.registerListener(myEventListener);

        ListView listView = (ListView) findViewById(R.id.listView);
        mAdapter = new ScanResultAdapter(getApplicationContext(),
                LayoutInflater.from(this));
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {



            }
        });



        if (savedInstanceState == null) { //savedInstanceState == null



            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    ScanAndConnect();
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
    protected void onStart() {

        super.onStart();
    }
    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            stopService(new Intent(this, BleService.class));
        }
        super.onDestroy();

    }

    private void ScanAndConnect() {
        //BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("DB:2D:A0:DA:8F:F1");
        //StartService(getBaseContext(), device);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         if (requestCode == Constants.REQUEST_ENABLE_BT && resultCode == -1)
         {
             ScanAndConnect();
         }
    }

    private void StartService(Context context, BluetoothDevice bleDevice) {

        if (bleDevice == null) return;

        //if (mScanning) {
        //mBluetoothLeScanner.stopScan(mScanCallback);
        //mBluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) mScanCallback);
        //mScanning = false;
        //}
        notificationIntent = new Intent(context, BleService.class);
        notificationIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, bleDevice.getName());
        notificationIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, bleDevice.getAddress());


        startForegroundService(notificationIntent);

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
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES )
                .setMatchMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
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
            //mBluetoothLeScanner.stopScan(mScanCallback);
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(),mScanCallback);

            String toastText = getString(R.string.scan_start_toast);
            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.already_scanning, Toast.LENGTH_SHORT).show();
        }
    }








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

            if (mAdapter.add(result) == true) //first hit
            {
                mBluetoothLeScanner.stopScan(this);
                // auto connect
                StartService(getApplicationContext(), result.getDevice());
            }

            mAdapter.notifyDataSetChanged();


        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getApplicationContext(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG).show();

        }


    }

    public class MyEventListener implements ConnectionEventListener{

        @Override
        public void onConnectionSetupComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textview = findViewById(R.id.connectionTextView);
                    textview.setText("Connected");
                 }
            });
            }

        @Override
        public void onDisconnect() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textview = findViewById(R.id.connectionTextView);
                    textview.setText("Not Connected");

                }
            });
        }
    }



    

}