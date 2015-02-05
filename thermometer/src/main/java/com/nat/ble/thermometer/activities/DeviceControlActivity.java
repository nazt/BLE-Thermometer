package com.nat.ble.thermometer.activities;


import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;


import com.nat.ble.thermometer.R;
import com.nat.ble.thermometer.interfaces.OnFragmentInteractionListener;
import com.nat.ble.thermometer.services.BluetoothLeService;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;


public class DeviceControlActivity extends Activity implements OnFragmentInteractionListener {
    @InjectView(R.id.ffe0_value) TextView mValueInput;
    @InjectView(R.id.battery_level_text_view) TextView mBatteryLevelTextView;


    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    public static final String EXTRA_DEVICE = "extra_device";
    private BluetoothLeDevice mDevice;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic = null;

    private boolean mServiceBound = false;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mServiceBound = true;
            
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                Toast.makeText(DeviceControlActivity.this, "Unable to initialize Bluettoth",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            else {
                // Automatically connects to the device upon successful start-up initialization.
                mBluetoothLeService.connect(mDevice.getAddress());
                Log.d(TAG, "mBluetoothLeService is connecting....");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            mServiceBound = false;
            mBluetoothLeService = null;
        }

    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the Title bar of this activity screen
//        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        // Hide the Title bar of this activity screen
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_device_control);

        ButterKnife.inject(this);


        if (savedInstanceState == null) {
            Bundle bundle = new Bundle();
            mDevice = getIntent().getParcelableExtra(EXTRA_DEVICE);
            bundle.putParcelable(EXTRA_DEVICE, mDevice);
            bindService();

            updateConnectionState(R.string.connecting);

        }
    }

    private void bindService() {
        final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        this.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void updateConnectionState(final int resourceId) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int colourId;

                switch (resourceId) {
                    case R.string.connected:
                        Toast.makeText(DeviceControlActivity.this, "BLE Connected",
                                Toast.LENGTH_SHORT).show();
                        if (mWriteCharacteristic != null) {
                            mWriteCharacteristic.setValue("AT+BATT?");
                            mBluetoothLeService.writeGatt(mWriteCharacteristic);
                        }
                        invalidateOptionsMenu();

                        break;
                    case R.string.disconnected:
                        invalidateOptionsMenu();
                        Toast.makeText(DeviceControlActivity.this, "Disconnected",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case R.string.unavailable:
                        invalidateOptionsMenu();
                        //Toast.makeText(DeviceControlActivity.this, "No compatible service found.",
                        //       Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "No compatible service found.");
                        break;
                    case R.string.connecting:
                        invalidateOptionsMenu();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);

        menu.findItem(R.id.menu_connect).setVisible(false);
        menu.findItem(R.id.menu_disconnect).setVisible(false);
        menu.findItem(R.id.menu_calibrate).setVisible(false);
        menu.setGroupVisible(R.id.menu_group_calibrate_options, false);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                Intent homeIntent = new Intent(this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
                break;
            case R.id.action_settings:
                break;
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDevice.getAddress());

                invalidateOptionsMenu();
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_calibrate:
                invalidateOptionsMenu();
                return true;
            case R.id.menu_force_display_raw_value:
                item.setChecked(true);
                return true;
            case R.id.menu_display_calibrated_value:
                item.setChecked(true);
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(TAG, "................. " + uri.toString());
    }

    @Override
    public void onFragmentCreated(Fragment fragment) {
    }

    @Override
    public void onResume() {
        super.onResume();

        this.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        bindService();

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDevice.getAddress());
//            ((DeviceControlActivity) mContext).setBluetoothLeService(mFragmentBluetoothLeService);
            Log.d(TAG, "Connect request result=" + result);
        }
        else {
        }
    }


    @Override
    protected void onStop() {
        Log.d(TAG, "ON STOP");
        if (mGattUpdateReceiver != null) {
            unregisterReceiver(mGattUpdateReceiver);
        }

        if (mServiceBound) {
            unbindService(mServiceConnection);
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "ON PAUSE");

        super.onPause();
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.
    //					      this can be a result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();


            Log.d(TAG, "========= mgat Update mBlueooth ========");
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                clearUI();
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                readDataFromGattCharacteristic(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final String noData = getString(R.string.no_data);
                final String uuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID_CHAR);
                final byte[] dataArr = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_RAW);

                Log.d(TAG, "uuid === " + uuid);

                String data = new String(dataArr).trim();

                if(data.startsWith("OK+Get:")) {
                    Log.d(TAG, data.substring(7));
                    DeviceControlActivity.this.
                            mBatteryLevelTextView.setText(data.substring(7) + "%");
                }
                else {
                    mWriteCharacteristic.setValue("AT+BATT?");
                    mBluetoothLeService.writeGatt(mWriteCharacteristic);
                    DeviceControlActivity.this.mValueInput.setText(data);
                }
            }
        }
    };


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void readDataFromGattCharacteristic(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        Resources res = getResources();
        boolean match_characteristic = false;
        for(BluetoothGattService gattService: gattServices) {
            uuid = gattService.getUuid().toString();
            String[] services = res.getStringArray(R.array.supported_service);
            String[] support_characteristics = res.getStringArray(R.array.supported_characteristic);

            boolean service_supported =  Arrays.asList(services).contains(uuid.toLowerCase());

            if (service_supported) {
                for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                    uuid = gattCharacteristic.getUuid().toString();
                    boolean chara_supported =  Arrays.asList(support_characteristics).contains(
                            uuid.toLowerCase());
                    if (chara_supported) {
                        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
                        final int charaProp = gattCharacteristic.getProperties();

                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }

                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                            mWriteCharacteristic = characteristic;
                        }

                        match_characteristic = true;
                        break;
                    }
                }
                if (match_characteristic == true) {
                    break;
                }
            }
        }

        if (match_characteristic == false) {
            updateConnectionState(R.string.unavailable);
        }

        invalidateOptionsMenu();

    }

    private void clearUI() {

    }

}


