package com.example.publictransportation.profiles;

public abstract class AbstractProfile {
	abstract public String getName();
	
	// numbers should be in milliseconds
	abstract public int getWifiSensorDelay();
	abstract public int getActivitySensorDelay();
	abstract public int getTimeSensorDelay();
	abstract public int getLowFrequencyDelay(); // Updating slow
	abstract public int getHighFrequencyDelay();  // Updating fast
	
	abstract public int getDefaultActivitySensorCutoff();
	abstract public int getLowActivitySensorCutoff();
}
