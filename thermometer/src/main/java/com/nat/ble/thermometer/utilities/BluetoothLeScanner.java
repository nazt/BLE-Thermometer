package com.nat.ble.thermometer.utilities;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.util.Log;

import com.nat.ble.thermometer.interfaces.LeStopCallback;

public class BluetoothLeScanner {
	private final Handler mHandler;
	private final BluetoothAdapter.LeScanCallback mLeScanCallback;
	private final BluetoothUtils mBluetoothUtils;
	private boolean mScanning;
    private final LeStopCallback mLeStopCallback;
	
	public BluetoothLeScanner(BluetoothAdapter.LeScanCallback leScanCallback, BluetoothUtils bluetoothUtils, LeStopCallback stopCallback){
		mHandler = new Handler();
		mLeScanCallback = leScanCallback;
		mBluetoothUtils = bluetoothUtils;
        mLeStopCallback = stopCallback;
	}
	
	public boolean isScanning() {
		return mScanning;
	}

	public void scanLeDevice(final int duration, final boolean enable) {
        if (enable) {
        	if(mScanning){return;}
        	Log.d("TAG", "~ Starting Scan");
            // Stops scanning after a pre-defined scan period.
        	if(duration > 0){
	            mHandler.postDelayed(new Runnable() {
	                @Override
	                public void run() {
	                	Log.d("TAG", "~ Stopping Scan (timeout)");
	                    mScanning = false;
                        mLeStopCallback.onLeStopScan();
	                    mBluetoothUtils.getBluetoothAdapter().stopLeScan(mLeScanCallback);
	                }
	            }, duration);
        	}
            mScanning = true;
            mBluetoothUtils.getBluetoothAdapter().startLeScan(mLeScanCallback);
        } else {
        	Log.d("TAG", "~ Stopping Scan");
            mScanning = false;
            mBluetoothUtils.getBluetoothAdapter().stopLeScan(mLeScanCallback);
            mLeStopCallback.onLeStopScan();
        }
    }
}
