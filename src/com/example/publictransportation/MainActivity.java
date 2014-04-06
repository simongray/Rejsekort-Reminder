package com.example.publictransportation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

//import com.example.publictransportation.service.TrackerService;

public class MainActivity extends Activity {

	// UI
	Switch onOffSwitch;

	// used to toggle switch off when there's no wifi
	private WifiStateReceiver wifiStateReceiver;

	// system services
	private WifiManager wifiManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// On/Off switch to turn on and off the whole service
		onOffSwitch = (Switch) findViewById(R.id.on_off_switch);

		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		Log.i("onCreate", "onCreate");
		
		// using onClickListener to prevent being checked automatically by system
		// http://stackoverflow.com/questions/9129858/how-can-i-distinguish-whether-value-is-changed-by-user-or-programmatically-incl
		onOffSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CompoundButton buttonView = (CompoundButton) v;
				if (buttonView.isChecked()) {
					if (isWifiEnabled()) {
						Log.i("onCheckedChange", "startTracker");
						startTracker();
					} else {
						Log.i("onCheckedChange", "wifinotenabled message");
						displayWifiNotEnabledMessage();
						onOffSwitch.setChecked(false);
					}
				} else {
					Log.i("onCheckedChange", "stopTracker");
					stopTracker();
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		if (isTrackerServiceRunning())
		{
			Log.i("onResume", "trackerservice is running");
			onOffSwitch.setChecked(true);
		}
		else {
			Log.i("onResume", "trackerservice is NOT running!!");
			onOffSwitch.setChecked(false);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		// Register the receiver, to retrieve broadcasts about the Wifi state, from the Android system
		wifiStateReceiver = new WifiStateReceiver();
		registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
	}

	@Override
	public void onStop() {
		super.onStop();
		unregisterReceiver(wifiStateReceiver);
	}

	// Method for starting the tracker service (with an intent)
	public void startTracker() {		
		//		 good explanation of startService/stopService vs bindService/unbindService:
		//		 http://stackoverflow.com/questions/3514287/android-service-startservice-and-bindservice
		Intent intent = new Intent(MainActivity.this, com.example.publictransportation.service.TrackerService.class);
		startService(intent);
	}

	public void stopTracker() {
		Intent intent = new Intent(MainActivity.this, com.example.publictransportation.service.TrackerService.class);		
		stopService(intent);
	}

	// used every time we launch the app, to set the UI up correctly
	// CONSIDER: finding a quicker, less intensive way of getting the same info
	private boolean isTrackerServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (service.service.getClassName().equals("com.example.publictransportation.service.TrackerService")) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressLint("NewApi")
	private Boolean isWifiEnabled() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
			return (wifiManager.isScanAlwaysAvailable() || wifiManager.isWifiEnabled());
		}
		else {
			return wifiManager.isWifiEnabled();
		}
	}

	private void displayWifiNotEnabledMessage() {
		Toast.makeText(this, getString(R.string.wifiNotEnabledMessage), Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// Inner class: Receives the broadcasts about the Wifi state, if it's disabled or not
	public class WifiStateReceiver extends BroadcastReceiver {
		@SuppressLint("NewApi")
		public void onReceive(Context context, Intent intent) {
			int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

			// disable the service on/off switch when wifi is disabled
			if (wifiState == WifiManager.WIFI_STATE_DISABLED && isTrackerServiceRunning()) {

				// We need to know if API 18/scanning always on mode is available
				// because if it's available, there's no need to switch off the service
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
					onOffSwitch.setChecked(wifiManager.isScanAlwaysAvailable());
				}
				else {
					onOffSwitch.setChecked(false);
				}
			}
		}
	}
}
