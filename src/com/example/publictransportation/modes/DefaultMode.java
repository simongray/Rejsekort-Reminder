package com.example.publictransportation.modes;

import java.util.List;

import android.content.res.Resources;

import com.example.publictransportation.R;
import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.sensors.AbstractSensor;
import com.example.publictransportation.sensors.ActivitySensor;
import com.example.publictransportation.sensors.CellSensor;
import com.example.publictransportation.sensors.SensorTypes;
import com.example.publictransportation.sensors.TimeSensor;
import com.example.publictransportation.sensors.WifiGroup;
import com.example.publictransportation.sensors.WifiSensor;
import com.example.publictransportation.service.IModeManager;

/*
 * This class is used as a beginning or interim transportation mode.
 * It launches all available sensors and attempts to find out what mode is appropriate.
 */

public class DefaultMode extends AbstractMode {

	// the labels we use for different switching logic
	final String BUS = "BUS";
	final String TRAIN = "TRAIN";
	final String STATION = "STATION";
	final String METRO_TUNNEL = "METRO_TUNNEL";
	final String TRAIN_TUNNEL = "TRAIN_TUNNEL";
	final String UNKNOWN = "UNKNOWN";

	final int BUS_COUNT_LIMIT = 16; // 1 minute 20 seconds
	final int S_TRAIN_COUNT_LIMIT = 21; // 1 minute 45 seconds
	final int S_TRAIN_TUNNEL_COUNT_LIMIT = 8; // 40 seconds
	final int TIMESENSOR_DELAY = 5000; // 5 seconds per tick

	String currentLabel;
	int ticks; // to count occurrences of some label being present

	String latestWifiSensorResult; // to hold latest data from WifiSensor

	Boolean enoughActivityResults = false;
	
	final int MIN_RESULTS = 2;
	final int MAX_RESULTS = 18;
	private ActivityResults activityResults;



	public DefaultMode(AbstractProfile profile, IModeManager manager) {
		super(profile, manager, "");

		activityResults = new ActivityResults(MAX_RESULTS);

		currentLabel = UNKNOWN;
		ticks = 0;
		latestWifiSensorResult = "";

		// Accessing the external resources to get list of cells and ssids
		Resources r = manager.getApplicationContext().getResources();

		String[] busSsids = r.getStringArray(R.array.bus_ssids);
		String[] trainSsids = r.getStringArray(R.array.train_ssids);
		String[] trainStationSsids = r.getStringArray(R.array.train_station_ssids);
		int[] metroUndergroundCells = r.getIntArray(R.array.metro_underground_cells);
		int[] metroUndergroundCellsIgnore = r.getIntArray(R.array.metro_underground_cells_ignore);
		int[] trainUndergroundCells = r.getIntArray(R.array.train_underground_cells);
		int[] trainUndergrundCellsIgnore = r.getIntArray(R.array.train_underground_cells_ignore);

		WifiGroup busGroup = new WifiGroup(BUS, busSsids);
		WifiGroup trainGroup = new WifiGroup(TRAIN,trainSsids);
		WifiGroup trainStationGroup = new WifiGroup(STATION, trainStationSsids);
		WifiGroup[] wifiGroups = {busGroup, trainGroup, trainStationGroup};

		AbstractSensor wifiSensor = new WifiSensor(this, wifiGroups, profile.getWifiSensorDelay(), "");
		AbstractSensor activitySensor = new ActivitySensor(this, profile.getActivitySensorDelay(), profile.getDefaultActivitySensorCutoff());
		AbstractSensor metroCellSensor = new CellSensor(this, metroUndergroundCells, metroUndergroundCellsIgnore, METRO_TUNNEL);
		AbstractSensor trainCellSensor = new CellSensor(this, trainUndergroundCells, trainUndergrundCellsIgnore, TRAIN_TUNNEL);
		AbstractSensor timeSensor = new TimeSensor(this, TIMESENSOR_DELAY);

		addSensor(wifiSensor);
		addSensor(activitySensor);
		addSensor(metroCellSensor);
		addSensor(trainCellSensor);
		addSensor(timeSensor);
	}

	@Override
	public void input(AbstractSensor sensor, String data) {
		// metro cells are clear indicator we are underground
		if (sensor.getType() == SensorTypes.CELL && !data.equals(CellSensor.NOTHING_FOUND)) {
			currentLabel = sensor.getLabel();
		}
		// otherwise, wifi connections set the label
		else if (sensor.getType() == SensorTypes.WIFI) {
			if (data.startsWith(WifiSensor.SIGNAL_FOUND)) {
				currentLabel = sensor.getLabel();
				latestWifiSensorResult = data;
			}
			else if (data.equals(WifiSensor.SIGNAL_LOST)) {
				currentLabel = UNKNOWN;
			}
		}
		// activity results will eventually be evaluated to avoid switching to wifi per default
		else if (sensor.getType() == SensorTypes.ACTIVITY) {
			activityResults.add(data);

			// used to allow evaluation of activity results
			if (activityResults.size() >= MIN_RESULTS) {
				enoughActivityResults = true;
			}
		}
		else if (sensor.getType() == SensorTypes.TIME) {
			if (!currentLabel.equals(UNKNOWN)) { // otherwise will count when there is nothing to count
				ticks += 1;
			}
			else {
				ticks = 0;
			}
		}

		// Always end input with evaluate
		evaluate();
	}

	@Override
	protected void evaluate() {
		// metro takes precedence over everything!!
		if (currentLabel.equals(METRO_TUNNEL)) {
			changeMode(ModeTypes.METRO, "");
		}
		// quickest way to get into s-train mode (works on Noerreport St)
		// the tick limit is there to prevent false positives when passing through
		if (currentLabel.equals(TRAIN_TUNNEL) && ticks >= S_TRAIN_TUNNEL_COUNT_LIMIT) {
			changeMode(ModeTypes.S_TRAIN, "");
		}
		// then normal way with s-train
		else if (currentLabel.equals(TRAIN) && ticks >= S_TRAIN_COUNT_LIMIT) {
			changeMode(ModeTypes.S_TRAIN, WifiSensor.getMacAddress(latestWifiSensorResult));
		}
		// then bus wifi connections are checked
		else if (currentLabel.equals(BUS) && ticks >= BUS_COUNT_LIMIT) {
			changeMode(ModeTypes.BUS, WifiSensor.getMacAddress(latestWifiSensorResult));
		}
		// only starts to evaluate after MIN_RESULTS has been reached!
		else if (enoughActivityResults) {
			String evaluatedActivity = evaluateActivity();

			if (evaluatedActivity.equals(ActivitySensor.ON_FOOT)) {
				changeMode(ModeTypes.MOVING, "");
			}
			// with no wifi connections, then it is possible we are Waiting at an unknown location
			else if (evaluatedActivity.equals(ActivitySensor.STILL)) {
				changeMode(ModeTypes.WAITING, "");
			}
		}
	}

	// cycles through last 15 or so results from activity sensor
	// the constants from activity sensor are returned here to indicate different evaluations
	private String evaluateActivity() {

		List<String> results = activityResults.getLatestResults();
		int size = results.size();


		if (enoughActivityResults) {

			// if the last two results are ON_FOOT, we evaluate as ON_FOOT
			if (results.get(0).equals(ActivitySensor.ON_FOOT) &&
					results.get(1).equals(ActivitySensor.ON_FOOT)) {
				return ActivitySensor.ON_FOOT;
			}

			// if the last 15 results are STILL or TILTING
			// then we are probably STILL
			else if (size == MAX_RESULTS) {
				int tiltingCount = 0;
				Boolean badResult = false;

				for (String result : results) {
					if (result.equals(ActivitySensor.STILL)) {
						// pass!
					}
					else if (results.equals(ActivitySensor.TILTING)) {
						tiltingCount += 1;
					}
					else {
						badResult = true;
						break;
					}
				}

				// if results only include STILL and at most 3 TILTING
				// then the final, evaluated result equals STILL
				if (!badResult && tiltingCount <= 3) {
					return ActivitySensor.STILL;
				}
			}
		}

		// the default result is not used for anything (...at the moment?)
		return ActivitySensor.UNKNOWN;
	}

	@Override
	public ModeTypes getType() {
		return ModeTypes.DEFAULT;
	}
}