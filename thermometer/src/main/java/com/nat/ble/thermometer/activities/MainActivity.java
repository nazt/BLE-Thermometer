package com.nat.ble.thermometer.activities;


import android.app.ActionBar;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.nat.ble.thermometer.R;
import com.nat.ble.thermometer.adapters.LeDeviceListAdapter;
import com.nat.ble.thermometer.containers.BluetoothLeDeviceStore;
import com.nat.ble.thermometer.interfaces.LeStopCallback;
import com.nat.ble.thermometer.utilities.BluetoothLeScanner;
import com.nat.ble.thermometer.utilities.BluetoothUtils;
import com.nat.ble.thermometer.utilities.Constants;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.easycursor.objectcursor.EasyObjectCursor;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class MainActivity extends ListActivity implements OnRefreshListener {
    @InjectView(R.id.tvItemCount)
    TextView mTvItemCount;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothUtils mBluetoothUtils;
    private BluetoothLeScanner mScanner;
    private BluetoothLeDeviceStore mDeviceStore;

    private PullToRefreshLayout mPullToRefreshLayout;

   /************************************************
    *                                              *
    *            LE SCAN CALLBACK                  *
    *                                              *
    ***********************************************/

   protected LeStopCallback mLeStopCallback = new LeStopCallback() {
       @Override
       public void onLeStopScan() {
            stopScan();
       }
   };
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            final BluetoothLeDevice deviceLe = new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis());
            mDeviceStore.addDevice(deviceLe);
            final EasyObjectCursor<BluetoothLeDevice> c = mDeviceStore.getDeviceCursor();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.swapCursor(c);
                    updateItemCount(mLeDeviceListAdapter.getCount());
                }
            });
        }
    };


    private void updateItemCount(int count){
        Log.d("COUNT", String.valueOf(count));
        mTvItemCount.setText(
                getString(
                        R.string.formatter_item_count,
                        String.valueOf(count)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Hide actionbar icon
        ActionBar actionBar = getActionBar();

//        actionBar.setDisplayShowTitleEnabled(false);
//        actionBar.setTitle("BLE");
//        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        ButterKnife.inject(this);

        mDeviceStore = new BluetoothLeDeviceStore();
        mBluetoothUtils = new BluetoothUtils(this);
        mScanner = new BluetoothLeScanner(mLeScanCallback, mBluetoothUtils, mLeStopCallback);

        updateItemCount(0);

        // Now find the PullToRefreshLayout to setup
        mPullToRefreshLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);

        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(this)
                // Mark All Children as pullable
                // .allChildrenArePullable()
                .theseChildrenArePullable(android.R.id.empty, android.R.id.list)
                .options(Options.create()
                .scrollDistance(.25f)
                .build())
                .listener(this)
                // Finally commit the setup to our PullToRefreshLayout
                .setup(mPullToRefreshLayout);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothLeDevice device = (BluetoothLeDevice) mLeDeviceListAdapter.getItem(position);
        if (device == null) return;

        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRA_DEVICE, device);
        startActivity(intent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_device:
                mDeviceStore.clear();
                updateItemCount(0);
                invalidateOptionsMenu();
                break;
        }
        return true;
    }

    private void stopScan() {
        mPullToRefreshLayout.setRefreshComplete();
        invalidateOptionsMenu();
    }

    private void startScan(){
        mPullToRefreshLayout.setRefreshing(true);
        mDeviceStore.clear();
        final boolean mIsBluetoothOn = mBluetoothUtils.isBluetoothOn();
        final boolean mIsBluetoothLePresent = mBluetoothUtils.isBluetoothLeSupported();
        updateItemCount(0);

        mLeDeviceListAdapter = new LeDeviceListAdapter(this, mDeviceStore.getDeviceCursor());
        setListAdapter(mLeDeviceListAdapter);

        mBluetoothUtils.askUserToEnableBluetoothIfNeeded();
        if(mIsBluetoothOn && mIsBluetoothLePresent){
            mScanner.scanLeDevice(Constants.SCAN_DURATION, true);
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onRefreshStarted(View view) {
       startScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
