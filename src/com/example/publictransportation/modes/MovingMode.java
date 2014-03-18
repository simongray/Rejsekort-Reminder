package com.example.publictransportation.modes;

import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.ActivitySensor;
import com.example.publictransportation.sensors.SensorTypes;
import com.example.publictransportation.service.IModeManager;

/*
 * The point of this mode is to limit resource use.
 * The mode is triggered by another mode and then only enables the ActivitySensor.
 * Once we can safely say we are not moving, the mode switches to default again.
 */
public class MovingMode extends AbstractMode {

	final int NOT_ON_FOOT_LIMIT = 1;
	final int TILTING_LIMIT = 2;

	int notOnFootCount;
	int tiltingCount;

	public MovingMode(AbstractProfile profile, IModeManager manager) {
		super(profile, manager, "");

		AbstractSensor activitySensor = new ActivitySensor(this, profile.getHighFrequencyDelay(), profile.getLowActivitySensorCutoff());
		addSensor(activitySensor);
	}

	@Override
	public void input(AbstractSensor sensor, String data) {
		if (sensor.getType() == SensorTypes.ACTIVITY) {

			if (!data.equals(ActivitySensor.ON_FOOT)) {
				if(data.equals(ActivitySensor.TILTING)) {
					tiltingCount += 1;
				} 
				else {
					notOnFootCount += 1;
					tiltingCount = 0;
				}
			}
		}

		// always end with call to evaluate()
		evaluate();
	}

	@Override
	protected void evaluate() {

		// If counted 2 or more tilting results, change to Default Mode
		if (tiltingCount >= TILTING_LIMIT) {
			changeMode(ModeTypes.DEFAULT, "");
		} 
		// If counted more than 1 not on foot results, change to Default Mode
		else if(notOnFootCount >= NOT_ON_FOOT_LIMIT) {
			changeMode(ModeTypes.DEFAULT, "");
		}
	}

	@Override
	public ModeTypes getType() {
		return ModeTypes.MOVING;
	}
}
