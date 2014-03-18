package com.example.publictransportation.modes;

import android.content.res.Resources;

import com.example.publictransportation.R;
import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.ActivitySensor;
import com.example.publictransportation.sensors.SensorTypes;
import com.example.publictransportation.sensors.WifiGroup;
import com.example.publictransportation.sensors.WifiSensor;
import com.example.publictransportation.service.IModeManager;

public class BusMode extends AbstractMode {

	final int ON_FOOT_RESULTS_LIMIT_SIGNAL_LOST = 1;
	final int ON_FOOT_RESULTS_LIMIT = 2;
	int onFootCount;
	
	Boolean originalSignalLost;

	public BusMode(AbstractProfile profile, IModeManager manager, String latestMacAddress) {
		super(profile, manager, latestMacAddress);

		onFootCount = 0;
		originalSignalLost = false;

		Resources r = manager.getApplicationContext().getResources();
		String[] busSsids = r.getStringArray(R.array.bus_ssids);

		WifiGroup busGroup = new WifiGroup(BUS, busSsids);
		WifiGroup[] groupArray = {busGroup};
		AbstractSensor wifiSensor = new WifiSensor(this, groupArray, profile.getWifiSensorDelay(), latestMacAddress); 
		AbstractSensor activitySensor = new ActivitySensor(this, profile.getActivitySensorDelay(), profile.getDefaultActivitySensorCutoff());

		addSensor(activitySensor);
		addSensor(wifiSensor);
	}

	@Override
	public void input(AbstractSensor sensor, String data) {		
		// indicates two scans have passed without the original Mac Address being found
		if (sensor.getType() == SensorTypes.WIFI) {
			if (data.equals(WifiSensor.NEW_SIGNAL) || data.equals(WifiSensor.NOTHING_FOUND)) {
				originalSignalLost = true;
			}
		} 
		// indicates we have started walking a certain distance (i.e. cannot be in a bus)
		else if (sensor.getType() == SensorTypes.ACTIVITY) {
			if (data.equals(ActivitySensor.ON_FOOT)) {
				onFootCount += 1;
			}
			else {
				onFootCount = 0;
			}	
		}

		// always end with call to evaluate()
		evaluate();
	}

	@Override
	protected void evaluate() {
		if (onFootCount >= ON_FOOT_RESULTS_LIMIT) {
			changeMode(ModeTypes.MOVING, ""); 
		}
		// only a single ON_FOOT is needed if we have lost the signal
		else if (onFootCount >= ON_FOOT_RESULTS_LIMIT_SIGNAL_LOST && originalSignalLost) {
			changeMode(ModeTypes.MOVING, ""); 
		}
	}

	@Override
	public ModeTypes getType() {
		return ModeTypes.BUS;
	}
}