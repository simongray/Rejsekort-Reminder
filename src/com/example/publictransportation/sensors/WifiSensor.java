package com.example.publictransportation.sensors;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import com.example.publictransportation.modes.AbstractMode;

/*
 * The type of output this sensor creates is of the form:
 * 
 * 		"NOTHING_FOUND" - for when no whitelisted SSIDs are found
 * 		"SIGNAL_LOST" - in the event a signal has been lost
 * 		"SIGNAL_FOUND -70 00:23:23:5f:54" - a negative number from -79 to 0, when a result has been found again
 * 		"NEW_SIGNAL" - when connecting to a different MAC address
 * 		"NO_WIFI" - tracker service eventually receives this and shuts down
 */
public class WifiSensor extends AbstractSensor {

	public Context context;
	private String label;
	private WifiManager wifiManager;
	private WifiReceiver wifiReceiver;
	private WifiStateReceiver wifiStateReceiver;

	// save the current connection info
	final int SIGNAL_STRENGTH_CUTOFF_POINT = -80;

	// signals to send back as output
	final public static String NOTHING_FOUND = "NOTHING_FOUND";
	final public static String SIGNAL_LOST = "SIGNAL_LOST";
	final public static String NO_WIFI = "NO_WIFI";
	final public static String SIGNAL_FOUND = "SIGNAL_FOUND"; // this is appended with the actual signal strength
	final public static String NEW_SIGNAL = "NEW_SIGNAL"; // when connecting to a different MAC address
	final public static String SEPARATOR = " ";
		
	int wifiState;
	boolean killed; // attempting this as way to prevent threading issues
	
	private Boolean scanningAlwaysOn;
	private Handler handler = new Handler();
	private WifiGroup[] whitelist;

	private String currentMacAddress;
	private String lastOutput;
	
	private int delay;

	// use "" for previouslyConnectedMacAddress and it gets ignored!
	@SuppressLint("NewApi")
	public WifiSensor(AbstractMode parentMode, WifiGroup[] whitelist, int delay, String originalMacAddress) {
		super(parentMode);
		this.context = parentMode.getContext();
		this.whitelist = whitelist;
		this.delay = delay;
		
		// set up of potential inherited MAC address
		if (originalMacAddress.isEmpty()) {
			lastOutput = NOTHING_FOUND; // checkScanResults() will *NOT* attempt to find a specific BSSID
		}
		else {
			lastOutput = SIGNAL_FOUND; // checkScanResults() *WILL* attempt to find a specific BSSID
			currentMacAddress = originalMacAddress;
		}

		// for getting scan results (async)
		handler = new Handler();

		wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		wifiState = wifiManager.getWifiState();

		// We need to know if API 18/scanning always on mode is available
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
			if (wifiManager.isScanAlwaysAvailable()) {
				scanningAlwaysOn = true;
			}
		} else{
			scanningAlwaysOn = false;
		}

		// for scan results
		wifiReceiver = new WifiReceiver();
		context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		// for wifi on/off info
		wifiStateReceiver = new WifiStateReceiver();
		context.registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

		// initial wifi scan should start with no delay
		startScan(0);
	}

	private Boolean isWifiEnabled() {
		if (wifiState == WifiManager.WIFI_STATE_ENABLED || scanningAlwaysOn) {
			return true;
		}
		return false;
	}

	@Override
	public void kill() {
		killed = true;
		context.unregisterReceiver(wifiReceiver);
		context.unregisterReceiver(wifiStateReceiver);
	}
	
	
	/*
	 * This method makes up the analysis part of WifiSensor.
	 * The 4 different signals it outputs are analysed in the mode.
	 */
	public void checkScanResults(List<ScanResult> results) {
		
		String data = NOTHING_FOUND; // default
		
		// finding any whitelisted SSID
		// (for signal strength above the limit)
		if (lastOutput.equals(SIGNAL_LOST) || lastOutput.equals(NOTHING_FOUND)) {
			for (ScanResult result : results) {
				if (result.level > SIGNAL_STRENGTH_CUTOFF_POINT) {
					
					// this will return a label if in a whitelisted group
					String newLabel = getLabelFromScanResult(result);
					
					if (!newLabel.isEmpty()) {
						currentMacAddress = result.BSSID;
						data = NEW_SIGNAL+SEPARATOR+result.level+SEPARATOR+result.BSSID;
						label = newLabel;
						break;
					}
				}
			}
		}
		// finding the same MAC address again
		// (no signal strength requirements)
		else {
			data = SIGNAL_LOST; // default when old result is not rediscovered in loop
			
			for (ScanResult result : results) {
				if (result.BSSID.equals(currentMacAddress)) {
					currentMacAddress = result.BSSID;
					data = SIGNAL_FOUND+SEPARATOR+result.level+SEPARATOR+result.BSSID;
					label = getLabelFromScanResult(result);
					break;
				}
			}
		}
		
		lastOutput = data; // <---- the key to making the logic in this method work
		output(data);
	}
	
	// compares to the whitelisted groups and returns label or empty string
	private String getLabelFromScanResult(ScanResult result) {
		for (WifiGroup whitelistedGroup : whitelist) {
			for (String whitelistedSsid : whitelistedGroup.whitelistedSsids) {
				if (result.SSID.equals(whitelistedSsid)) {
					return whitelistedGroup.label;
				}
			}
		}
		
		return "";
	}

	@Override
	public SensorTypes getType() {
		return SensorTypes.WIFI;
	}

	@Override
	public String getLabel() {
		if(label==null) { label = ""; }
		return label;
	}

	private void startScan(final int delayInMilliseconds) {
		if (isWifiEnabled()) {
			handler.postDelayed( new Runnable() {
				@Override 
				public void run(){
					try {
						wifiManager.startScan();
						Log.i("WifiSensor", "wifiManager.startScan();");
					}
					catch (Exception e) {
						Log.i("WifiSensor", "exception: "+e.getMessage());
					}
				}}, delayInMilliseconds);
		}
	}

	// splits string of format "SIGNAL_FOUND signalStrength macAddress"
	// returns int value of signalStrength
	public static int getSignalStrength(String data) {
		String[] splitData = data.split(SEPARATOR);

		return Integer.parseInt(splitData[1]);
	}

	// returns int value of macAddress
	public static String getMacAddress(String data) {
		String[] splitData = data.split(SEPARATOR);

		if (splitData.length >= 2) {
			return splitData[2];
		}else{
			return "";	
		}		
	}

	// this inner class deals with scanning results
	public class WifiReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			if (!killed) {
				Log.i("WifiSensor", "WifiSensor.onReceive()");

				// perform analysis of scanning result
				checkScanResults(wifiManager.getScanResults());

				// schedule a new scan after a small delay
				startScan(delay);
			}
		}
	}

	// this inner class deals with connection info
	public class WifiStateReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			if (!killed) {
				wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				Log.i("WifiSensor", "wifi state changed: "+wifiState);

				// inform current transportation mode when wifi is gone
				if (wifiState == WifiManager.WIFI_STATE_DISABLED && !scanningAlwaysOn) {
					output(NO_WIFI);
					Log.i("WifiSensor","no wifi");
				}
			}
		}
	}
}