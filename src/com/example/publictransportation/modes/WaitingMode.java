package com.example.publictransportation.modes;

import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.ActivitySensor;
import com.example.publictransportation.service.IModeManager;

public class WaitingMode extends AbstractMode {

	int notStill = 0;
	
	final int NOT_STILL_LIMIT = 1; 

	public WaitingMode(AbstractProfile profile, IModeManager manager) {
		super(profile, manager, "");

		AbstractSensor activitySensor = new ActivitySensor(this, profile.getLowFrequencyDelay(), profile.getLowActivitySensorCutoff());
		addSensor(activitySensor);
	}

	@Override
	public void input(AbstractSensor sensor, String data) {
		if (!data.equals(ActivitySensor.STILL)) {			
				notStill += 1;
		}

		// always end with call to evaluate()
		evaluate();
	}

	@Override
	protected void evaluate() {
		if(notStill >= NOT_STILL_LIMIT) {
			changeMode(ModeTypes.DEFAULT, "");
		}

	}

	@Override
	public ModeTypes getType() {
		return ModeTypes.WAITING;
	}

}
