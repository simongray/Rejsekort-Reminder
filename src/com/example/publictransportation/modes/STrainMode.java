package com.example.publictransportation.modes;

import android.content.res.Resources;

import com.example.publictransportation.R;
import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.CellSensor;
import com.example.publictransportation.sensors.SensorTypes;
import com.example.publictransportation.sensors.WifiGroup;
import com.example.publictransportation.sensors.WifiSensor;
import com.example.publictransportation.service.IModeManager;

/*
 * We can say we are in a train with a certain confidence solely based on wifi.
 * The mode works by setting a lower limit based on initial signal strength,
 * and then changing mode to DefaultMode once this lower limit has been reached.
 */
public class STrainMode extends AbstractMode {

	final String TRAIN = "TRAIN";
	final String TRAIN_TUNNEL = "TRAIN_TUNNEL";
	Boolean allSignalsLost;
	Boolean inTunnel;

	public STrainMode(AbstractProfile profile, IModeManager manager, String latestMacAddress) {
		super(profile, manager, latestMacAddress);

		Resources r = manager.getApplicationContext().getResources();
		String[] trainSsids = r.getStringArray(R.array.train_ssids);
		int[] trainUndergroundCells = r.getIntArray(R.array.metro_underground_cells);
		int[] trainUndergrundCellsIgnore = new int[0];
		
		// if we entered this mode without a MAC address, it is a sure indication we are in a tunnel
		inTunnel = latestMacAddress.isEmpty()? true : false;
		
		WifiGroup trainGroup = new WifiGroup(TRAIN, trainSsids);
		WifiGroup[] wifiGroups = {trainGroup};

		allSignalsLost = false;
		
		AbstractSensor wifiSensor = new WifiSensor(this, wifiGroups, profile.getWifiSensorDelay(), latestMacAddress);
		AbstractSensor trainCellSensor = new CellSensor(this, trainUndergroundCells, trainUndergrundCellsIgnore, TRAIN_TUNNEL);
		
		addSensor(wifiSensor);
		addSensor(trainCellSensor);
	}

	@Override
	public void input(AbstractSensor sensor, String data) {
		if (sensor.getType() == SensorTypes.CELL && data.equals(CellSensor.NOTHING_FOUND)) {
			inTunnel = false;
		}
		else if (sensor.getType() == SensorTypes.WIFI) {
			// only a total absence of whitelisted SSIDS will exit this mode!
			if (data.equals(WifiSensor.NOTHING_FOUND)) {
				allSignalsLost = true;
			}
		}

		// always end with call to evaluate()
		evaluate();
	}

	@Override
	protected void evaluate() {		
		if (allSignalsLost && !inTunnel) {
			changeMode(ModeTypes.DEFAULT, "");
		}
	}

	@Override
	public ModeTypes getType() {
		return ModeTypes.S_TRAIN;
	}
}
