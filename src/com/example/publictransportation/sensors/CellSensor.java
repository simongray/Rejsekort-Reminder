package com.example.publictransportation.sensors;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.example.publictransportation.modes.AbstractMode;

/*
 * This mode either outputs the connected cell id or NOTHING_FOUND.
 * 
 */
public class CellSensor extends AbstractSensor {

	TelephonyManager telephonyManager;
	CellListener listener;
	int[] whitelistedCells;
	int[] blacklistedCells;
	String area;

	final int SIGNAL_STRENGTH_LIMIT = -95;
	int cellSignalstrength;

	public static final String NOTHING_FOUND = "NOTHING_FOUND";

	public CellSensor(AbstractMode parentMode, int[] whitelistedCells, int[] blacklistedCells, String area) {
		super(parentMode);

		this.whitelistedCells = whitelistedCells;
		this.blacklistedCells = blacklistedCells;
		this.area = area;

		// registers a new listener for cell location
		telephonyManager = (TelephonyManager) parentMode.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		listener = new CellListener();
		telephonyManager.listen(listener, PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}

	@Override
	public void kill() {
		// deregister the listener
		telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
	}

	// accesses the phone location info to listen for cell location changes
	// upon each change, output() is called
	class CellListener extends PhoneStateListener {

		@Override
		public void onCellLocationChanged(CellLocation location) {

			// use correct subclass (GSM is used in Europe)
			GsmCellLocation gsmLocation = (GsmCellLocation) location;

			if (cellSignalstrength > SIGNAL_STRENGTH_LIMIT) {
				// save the current cell id and check it
				checkCellId(gsmLocation.getCid());
				Log.i("Signal Strength","Cell signal:"+cellSignalstrength);
			}
		}

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			cellSignalstrength = getSignalStrength(signalStrength);
		}
	}

	// Returns 3G or 4G signal strength
	protected int getSignalStrength(SignalStrength signal) {

		String ssignal = signal.toString();
		String[] parts = ssignal.split(" ");

		int dB = -120; // No Signal Measured when returning -120 dB

		// If LTE 
		if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE){

			int ltesignal = Integer.parseInt(parts[9]);

			// check to see if it get's the right signal in dB, a signal below -2
			if(ltesignal < -2) {
				dB = ltesignal;
			}
		}
		// Else 3G
		else {

			if (signal.getGsmSignalStrength() != 99) {

				int strengthInteger = -113 + 2 * signal.getGsmSignalStrength();
				dB = strengthInteger;	
			}
		}

		return dB;
	}

	// check some cell against the list of whitelisted cells
	// and blacklisted cells
	private void checkCellId(int cellId) {

		Boolean foundBlacklistedCell = false;

		// ignore blacklisted cells
		for (int i = 0; i < blacklistedCells.length; i++) {
			if (cellId == blacklistedCells[i]) {
				foundBlacklistedCell = true;
				break;
			}
		}
		
		// only output something if cell was not in blacklist
		if (!foundBlacklistedCell) {
			
			String cell = "";
			
			for (int i = 0; i < whitelistedCells.length; i++) {
				if (cellId == whitelistedCells[i]) {
					cell = cellId+"";
					break;
				}
			}

			if (cell.isEmpty()) {
				output(NOTHING_FOUND);
			}
			else {
				output(cell);
			}
		}
	}

	@Override
	public SensorTypes getType() {
		return SensorTypes.CELL;
	}

	@Override
	public String getLabel() {
		return area;
	}

}
