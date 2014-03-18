package com.example.publictransportation.service;

import android.content.Context;

import com.example.publictransportation.modes.ModeTypes;
/*
 * This interface allows the Activity/Service implementing it to associate with Modes and by extension Sensors.
 */
public interface IModeManager {
	
	void changeMode(ModeTypes newMode, String data);
	
	// to allow Modes to access this standard method of the Activity class
	public Context getApplicationContext();
	
	// to allow the transportation mode to kill the service
	public void abortOnMissingWifi();
}
