package com.example.publictransportation.modes;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.Toast;

import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.ActivitySensor;
import com.example.publictransportation.sensors.SensorTypes;
import com.example.publictransportation.sensors.WifiSensor;
import com.example.publictransportation.service.IModeManager;
import com.example.publictransportation.service.LogTypes;

public abstract class AbstractMode {

	protected IModeManager manager;
	protected AbstractProfile profile;
	protected List<AbstractSensor> sensors;
	final String BUS = "BUS";
	final String TRAIN = "TRAIN";
	final String TRAIN_STATION = "TRAIN_STATION";
	protected String latestMacAddress; // Contains the MAC address to be passed to other modes

	public AbstractMode(AbstractProfile profile, IModeManager manager, String latestMacAddress) {
		this.profile = profile;
		this.manager = manager;
		this.latestMacAddress = latestMacAddress;
		sensors = new ArrayList<AbstractSensor>();
	}
	
	public Boolean isForced() {
		return false;
	}

	protected void changeMode(ModeTypes newMode, String latestMacAddress) {
		manager.changeMode(newMode, latestMacAddress);
	}

	// called by a child sensor
	abstract public void input(AbstractSensor sensor, String data);

	// where all transportation mode changing decisions are made
	// should be called every time input() is called
	abstract protected void evaluate();

	// called by the parent activity
	public void kill() {
		killAllSensors();
	}

	// needed for the Activity to ID the mode
	abstract public ModeTypes getType();

	// needed to give sensors access to system services (e.g. TelephonyManager)
	public Context getContext() {
		return manager.getApplicationContext();
	}

	protected void addSensor(AbstractSensor sensor) {
		sensors.add(sensor);
	}

	// called upon getting a kill signal
	protected void killAllSensors() {
		for (AbstractSensor sensor : sensors) {
			sensor.kill();
		}
	}

	public void log(SensorTypes type, String label, String data) {
		manager.log(LogTypes.SENSOR, String.valueOf(type) + " (" + label + "): " + data);
	}
}
