package com.example.publictransportation.modes;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.Toast;

import com.example.publictransportation.TextToSpeechActuator;
import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.ActivitySensor;
import com.example.publictransportation.sensors.SensorTypes;
import com.example.publictransportation.sensors.WifiSensor;
import com.example.publictransportation.service.IModeManager;

public abstract class AbstractMode {

	protected IModeManager manager;
	protected AbstractProfile profile;
	protected List<AbstractSensor> sensors;
	private TextToSpeechActuator tts;
	final String BUS = "BUS";
	final String TRAIN = "TRAIN";
	final String TRAIN_STATION = "TRAIN_STATION";
	protected String latestMacAddress; // Contains the MAC address to be passed to other modes

	public AbstractMode(AbstractProfile profile, IModeManager manager, String latestMacAddress) {
		this.profile = profile;
		this.manager = manager;
		this.latestMacAddress = latestMacAddress;
		sensors = new ArrayList<AbstractSensor>();

		if (profile.isSpeechOn()) {
			tts = new TextToSpeechActuator(this);
		}
	}
	
	public Boolean isForced() {
		return false;
	}

	protected void changeMode(ModeTypes newMode, String latestMacAddress) {
		// Speak the mode change via TTS
		say(newMode);

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

	/////////////////////////////
	///// DEBUGGING METHODS /////
	/////////////////////////////

	// used for debugging (called from mode itself)
	public void say(ModeTypes type) {
		String output = "Changing to "+type+" Mode";

		if (profile.isSpeechOn()) {
			tts.say(output);
		}

		if (profile.isToastOn()) {
			Toast.makeText(manager.getApplicationContext(), output, Toast.LENGTH_SHORT).show();
		}
	}

	// used for debugging (called from sensor)
	public void say(SensorTypes type, String label, String data) {
		String output = data+" "+type+".";

		if (profile.isSpeechOn()) {

			// BUS, TRAIN, METRO (WIFI SENSOR)
			if(data.contains(WifiSensor.SIGNAL_FOUND)) {
				if(label.equals(BUS) ) {
					tts.say("Bus was found"); //found bus
				}else if(label.equals(TRAIN)) {
					tts.say("Train was found"); //found train
				}else if(label.equals(TRAIN_STATION)) {
					tts.say("A train station was found"); //found train station
				}
			}
			else if(data.equals(WifiSensor.SIGNAL_LOST)) {
				if(label.equals(BUS) ) {
					tts.say("Bus singal lost"); //left bus
				}else if(label.equals(TRAIN)) {
					tts.say("Train signal lost"); //left train
				}else if(label.equals(TRAIN_STATION)) {
					tts.say("Train Station signal lost"); //left train station
				}
			}
			else if(data.equals(WifiSensor.NEW_SIGNAL)) {
				if(label.equals(BUS)) {
					tts.say("Bus new singal"); //left bus
				}else if(label.equals(TRAIN)){
					tts.say("Train new singal"); //left bus
				}else if(label.equals(TRAIN_STATION)){
					tts.say("Train Station new singal"); //left bus
				}
			}

			// ON FOOT, IN VEHICLE ETC. (ACTIVITY SENSOR)
			if(data.equals(ActivitySensor.ON_FOOT)) {
				tts.say("On foot activity"); //on foot
			}else if(data.equals(ActivitySensor.IN_VEHICLE)){
				tts.say("In the car or bus activity"); //in bus or car
			}else if(data.equals(ActivitySensor.STILL)){
				tts.say("Still activity"); //standing or sitting still
			}else if(data.equals(ActivitySensor.TILTING)){
				tts.say("Tilt activity"); //tilting
			}else if(data.equals(ActivitySensor.ON_BICYCLE) || data.equals(ActivitySensor.UNKNOWN)){
				tts.say("Unknown activity"); //unknown
			}

		}

		if (profile.isToastOn()) {
			Toast.makeText(manager.getApplicationContext(), label + " " + output, Toast.LENGTH_SHORT).show();
		}
	}
}
