package com.example.wu111;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;



public class BleService extends Service {
    private static final int GATT_INTERNAL_ERROR = 129;
    private UUID SHIMANO_BICYCLE_INFORMATION = UUID.fromString("000018ef-5348-494d-414e-4f5f424c4500");
    private UUID INSTANTANEOUS_INFORMATION = UUID.fromString("00002ac2-5348-494d-414e-4f5f424c4500");
    private static final String TAG = BleService.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt ble;
    private Runnable discoverServicesRunnable;
    private Handler bleCallBackHandler = null;

    private SoundPool soundPool;
    private int sound1;

    public BleService() {

        bleCallBackHandler = new Handler(Looper.getMainLooper());
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        sound1 = soundPool.load(this, R.raw.bicycle_bell, 1);
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();

        String address = intent.getStringExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        device.connectGatt(this, true, gattCallback);
        //device.connectGatt(parent.getContext(), false, gattCallback, TRANSPORT_LE).connect();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private BluetoothGattCallback gattCallback =  new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            int a=1;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    ble = gatt;
                    // We successfully connected, proceed with service discovery
                    int bondstate = ble.getDevice().getBondState();
                    // Take action depending on the bond state
                    if(bondstate == BOND_NONE || bondstate == BOND_BONDED) {

                        // Connected to device, now proceed to discover it's services but delay a bit if needed
                        int delayWhenBonded = 0;
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                            delayWhenBonded = 1000;
                        }
                        final int delay = bondstate == BOND_BONDED ? delayWhenBonded : 0;

                        discoverServicesRunnable = new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, String.format(Locale.ENGLISH, "discovering services of '%s' with delay of %d ms", ble.getDevice().getName(), delay));
                                boolean result = gatt.discoverServices();
                                if (!result) {
                                    Log.e(TAG, "discoverServices failed to start");
                                }
                                discoverServicesRunnable = null;
                            }
                        };

                        bleCallBackHandler.postDelayed(discoverServicesRunnable, delay);
                    } else if (bondstate == BOND_BONDING) {
                        // Bonding process in progress, let it complete
                        Log.i(TAG, "waiting for bonding to complete");
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // We successfully disconnected on our own request
                    //device.connectGatt(getBaseContext(), false, gattCallback);
                    //gatt.connect();
                    //gatt.close();
                } else {
                    // We're CONNECTING or DISCONNECTING, ignore for now
                    //device.connectGatt(getBaseContext(), false, gattCallback);
                    //gatt.connect();
                }
            } else {
                // An error happened...figure out what happened!
                Log.d(TAG, "error occured when connection status code" + status);
                //       ble.disconnect();
                //       ble = device.connectGatt(getBaseContext(), false, gattCallback);
                //       ble.connect();

                //gatt.disconnect();
                //gatt.close();
                //gatt.connect();
                if (status == 8)
                {
                    //gatt.connect();
                    //mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    //mBleScanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                    //        mLeScanCallback);
                }

            }
        };



        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            // Check if the service discovery succeeded. If not disconnect
            if (status == GATT_INTERNAL_ERROR) {
                Log.e(TAG, "Service discovery failed");
                gatt.disconnect();
                return;
            }

            List<BluetoothGattService> services = gatt.getServices();
            BluetoothGattService bikeService = gatt.getService(SHIMANO_BICYCLE_INFORMATION);
            if (bikeService != null)
            {
                BluetoothGattCharacteristic di2SwitchCharacteristic = bikeService.getCharacteristic(INSTANTANEOUS_INFORMATION);
                gatt.setCharacteristicNotification(di2SwitchCharacteristic, true);

                List<BluetoothGattDescriptor> descriptors = di2SwitchCharacteristic.getDescriptors();

                descriptors.forEach(descriptor ->
                        {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                );
            }
            // Write on the config descriptors to be notified when the value changes
            //characteristic?.descriptors?.forEach { descriptor ->
            //        descriptor?.let {
            //    it.value = if (enable) {
            //        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            //    } else {
            //        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            //    }
            //    bluetoothGatt?.writeDescriptor(it)
            //}
            //}
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] characteristicValue = characteristic.getValue();
            int rightButtonValue = ((int) characteristicValue[2]);
            if (rightButtonValue > 47)
            {
                //Uri alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                //Ringtone ringtoneAlarm = RingtoneManager.getRingtone(getApplicationContext(), alarmTone);
                //ringtoneAlarm.play();


                AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0);

                //Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                //MediaPlayer mp=new MediaPlayer();
                //mp.setLooping(false);
                //mp = MediaPlayer.create(MainActivity.this, notification);
                //mp.setVolume(1,1);
                //mp.start();


                soundPool.play(sound1, 1F, 1F, 0, 0, 1F);
                //soundPool.autoPause();
            }
        }
    };
}