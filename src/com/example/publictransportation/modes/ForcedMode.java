package com.example.publictransportation.modes;

import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.ActivitySensor;
import com.example.publictransportation.sensors.SensorTypes;
import com.example.publictransportation.service.IModeManager;


public class ForcedMode extends AbstractMode {

	final int ON_FOOT_LIMIT = 2;
	int onFootCount;
	
	ModeTypes forcedMode;

	public ForcedMode(AbstractProfile profile, IModeManager manager, ModeTypes forcedMode) {
		super(profile, manager, "");
		this.forcedMode = forcedMode;

		onFootCount = 0;

		AbstractSensor activitySensor = new ActivitySensor(this, profile.getHighFrequencyDelay(), profile.getLowActivitySensorCutoff());
		addSensor(activitySensor);
	}
	
	@Override
	public Boolean isForced() {
		return true;
	}

	@Override
	public void input(AbstractSensor sensor, String data) {
		if (sensor.getType() == SensorTypes.ACTIVITY) {

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
		// If counted 2 or more tilting results, change to Default Mode
		if (onFootCount >= ON_FOOT_LIMIT) {
			changeMode(ModeTypes.DEFAULT, "");
		}
	}

	// pretends to be a different mode when asked
	// this is used to show proper notifications when leaving this fake mode
	@Override
	public ModeTypes getType() {
		return forcedMode;
	}
}
