package com.example.publictransportation.sensors;

import com.example.publictransportation.modes.AbstractMode;

public abstract class AbstractSensor {
	
	protected AbstractMode parentMode;
	
	public AbstractSensor(AbstractMode parentMode) {
		this.parentMode = parentMode;
	}
	
	// send data back to the parent transport mode
	// (this is called by whatever processes the derived non-abstract class launches)
	void output(String data) {
		parentMode.input(this, data);
	}
	
	// eventually called by the parent transport mode
	// this method shuts down all currently running sensor processes
	abstract public void kill();
	
	// needed for the state to properly ID the sensor
	abstract public SensorTypes getType();
	
	// a further subdivision after the type
	abstract public String getLabel();
	

}
