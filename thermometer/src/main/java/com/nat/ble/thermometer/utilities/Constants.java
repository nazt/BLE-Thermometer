package com.nat.ble.thermometer.utilities;

import java.text.DecimalFormat;

public class Constants {
	public static final DecimalFormat DOUBLE_TWO_DIGIT_ACCURACY = new DecimalFormat("#.##");
	public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final int SCAN_DURATION = 3*1000;
    public static final int BLE_CALIBRATION_TIMES = 3;
    public static  final int BLE_IDLE = 0x00;
    public static  final int BLE_CONNECTING   = 0x01;
    public static  final int BLE_CONNECTED    = 0x02;
    public static  final int BLE_DISCONNECTED = 0x03;
    public static  final int BLE_UNAVAILABLE  = 0x04;
    public static  final int BLE_CALIBRATING  = 0x05;
    public static  final int BLE_CALIBRATED   = 0x06;
}
