package com.example.publictransportation.modes;

import android.content.res.Resources;

import com.example.publictransportation.R;
import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.ActivitySensor;
import com.example.publictransportation.sensors.CellSensor;
import com.example.publictransportation.sensors.SensorTypes;
import com.example.publictransportation.service.IModeManager;

	
public class MetroMode extends AbstractMode {
	
	Boolean underground;
	Boolean leavingMetro;
	
	final static String METRO_TUNNEL = "METRO_TUNNEL";
	
	public MetroMode(AbstractProfile profile, IModeManager manager) {
		super(profile, manager, "");
		
		// assumed per default when starting this mode
		underground = true;
		leavingMetro = false;
		
		// accessing the external resources to get list of cells
		Resources r = manager.getApplicationContext().getResources();
		int[] metroUndergroundCells = r.getIntArray(R.array.metro_underground_cells);
		int[] metroUndergroundCellsIgnore = r.getIntArray(R.array.metro_underground_cells_ignore);
		
		AbstractSensor metroCellSensor = new CellSensor(this, metroUndergroundCells, metroUndergroundCellsIgnore, METRO_TUNNEL);
		AbstractSensor activitySensor = new ActivitySensor(this, profile.getActivitySensorDelay(), profile.getDefaultActivitySensorCutoff());
		
		addSensor(metroCellSensor);
		addSensor(activitySensor);
	}

	@Override
	public void input(AbstractSensor sensor, String data) {
		if (sensor.getType() == SensorTypes.CELL) {
			underground = data.equals(CellSensor.NOTHING_FOUND)? false : true;
		}
		else if (sensor.getType() == SensorTypes.ACTIVITY && data.equals(ActivitySensor.ON_FOOT)) {
			if (!underground) {
				leavingMetro = true;
			}
		}
		
		// input() should always end with a call to evaluate()
		evaluate();
	}

	@Override
	protected void evaluate() {
		if (leavingMetro) {
			changeMode(ModeTypes.MOVING, "");
		}
	}

	@Override
	public ModeTypes getType() {
		return ModeTypes.METRO;
	}

}
