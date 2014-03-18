package com.example.publictransportation.profiles;



public class DefaultProfile extends AbstractProfile {


	@Override
	public String getName() {
		return "DEFAULT";
	}


	@Override
	public int getLowFrequencyDelay() {
		return 60000; // 1 minute
	}

	@Override
	public int getHighFrequencyDelay() {
		return 5000; // 5 seconds
	}

	@Override
	public int getWifiSensorDelay() {
		return 15000; // 15 seconds
	}

	@Override
	public int getActivitySensorDelay() {
		return 15000; // 15 seconds
	}

	@Override
	public int getTimeSensorDelay() {
		return 20000; // 20 seconds
	}

	@Override
	public Boolean isSpeechOn() {
		return false; // true: makes my phone crash when receiving a notification (with sound)!! - Simon
	}

	@Override
	public Boolean isToastOn() {
		return false;
	}


	@Override
	public int getDefaultActivitySensorCutoff() {
		return 50;
	}


	@Override
	public int getLowActivitySensorCutoff() {
		return 1;
	}

}
