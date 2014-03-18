package com.example.publictransportation.sensors;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.publictransportation.modes.AbstractMode;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

public class ActivitySensor extends AbstractSensor implements ConnectionCallbacks, OnConnectionFailedListener  {

	// Constants that define the activity detection interval
	private int confidenceCutoff;
	private int delay;

	/*
	 * Store the PendingIntent used to send activity recognition events
	 * back to the app
	 */
	public PendingIntent mActivityRecognitionPendingIntent;

	// Store the current activity recognition client
	private ActivityRecognitionClient mActivityRecognitionClient;

	// Flag that indicates if a request is underway.
	private boolean mInProgress;

	Context mContext;

	// output codes
	final public static String IN_VEHICLE = "IN_VEHICLE";
	final public static String ON_BICYCLE = "ON_BICYCLE";
	final public static String ON_FOOT = "ON_FOOT";
	final public static String STILL = "STILL";
	final public static String TILTING = "TILTING";
	final public static String UNKNOWN = "UNKNOWN";

	public ActivitySensor(AbstractMode parentMode, int delay, int confidenceCutoff) {
		super(parentMode);
		Log.i("AS", "activity sensor constructor");
		mContext = parentMode.getContext();
		mInProgress = false;
		this.delay = delay;
		this.confidenceCutoff = confidenceCutoff;

		if(servicesConnected()) {

			/*
			 * Instantiate a new activity recognition client. Since the
			 * parent Activity implements the connection listener and
			 * connection failure listener, the constructor uses "this"
			 * to specify the values of those parameters.
			 */
			mActivityRecognitionClient = new ActivityRecognitionClient(mContext, this, this);


			// Register the receiver of the intents sent from the IntentService.
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,  new IntentFilter("ACTIVITY_RECOGNITION_DATA"));

			startUpdates();

			/*
			 * Create the PendingIntent that Location Services uses
			 * to send activity recognition updates back to this app.
			 */
			Intent intent = new Intent(mContext, ActivitySensorIntentService.class);

			/*
			 * Return a PendingIntent that starts the IntentService.
			 */ 
			mActivityRecognitionPendingIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		}
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			// Extract data included in the Intent
			int activity = intent.getIntExtra(ActivitySensorIntentService.ACTIVITY, 0);
			int confidence = intent.getIntExtra(ActivitySensorIntentService.CONFIDENCE, 0);

			if (confidence > confidenceCutoff) {
				output(getOutput(activity));
				
			}

			Log.d("AS receiver", "Got Activity: " + getOutput(activity) + " "+confidence);
		}
	};

	private String getOutput(int activity) {
		switch (activity) {
		case DetectedActivity.IN_VEHICLE : return IN_VEHICLE;
		case DetectedActivity.ON_BICYCLE : return ON_BICYCLE;
		case DetectedActivity.ON_FOOT : return ON_FOOT;
		case DetectedActivity.STILL : return STILL;
		case DetectedActivity.TILTING : return TILTING;
		default : return UNKNOWN;
		}
	}

	/**
	 * Request activity recognition updates based on the current
	 * detection interval.
	 *
	 */
	public void startUpdates() {

		// If a request is not already underway
		// Indicate that a request is in progress
		// Request a connection to Location Services
		if (!mInProgress) {
			mActivityRecognitionClient.connect();
			mInProgress = true;
			Log.i("AS", "mInProgress = true");
		}
	}

	private boolean servicesConnected() {

		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("AS", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			Log.i("AS", "Services connected - Error");
		}

		Log.i("AS", "Services connected - Error 2");
		return false;
	}

	@Override
	public void kill() {
		mInProgress = false;
		this.onDisconnected();
		mActivityRecognitionPendingIntent.cancel();
	}

	@Override
	public SensorTypes getType() {
		return SensorTypes.ACTIVITY;
	}

	@Override
	public String getLabel() {
		return ""; // not used
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// Turn off the request flag
		mInProgress = false;

		/*
		 * If the error has a resolution, start a Google Play services
		 * activity to resolve it.
		 */
		if (connectionResult.hasResolution()) {
			Log.i("AS", "onConnectionFailed: has resolution -> download play services");
		} else {
			// Get the error code
			int errorCode = connectionResult.getErrorCode();
			Log.i("AS", "onConnectionFailed: Error code"+errorCode);
		}
	}

	@Override
	public void onConnected(Bundle dataBundle) {

		Log.i("AS","Connection Activity Recognigion: Connected!");
		/*
		 * Request activity recognition updates using the preset
		 * detection interval and PendingIntent. This call is
		 * synchronous.
		 */
		mActivityRecognitionClient.requestActivityUpdates(delay, mActivityRecognitionPendingIntent);

		Log.i("AS","onConnected: Activity Recognition request for update sent");
		/*
		 * Since the preceding call is synchronous, turn off the
		 * in progress flag and disconnect the client
		 */
		mInProgress = true;
		mActivityRecognitionClient.disconnect();
	}

	@Override
	public void onDisconnected() {

		// Turn off the request flag
		mInProgress = false;

		// Delete the client
		mActivityRecognitionClient.disconnect();
		mActivityRecognitionClient.unregisterConnectionCallbacks(this);
		mActivityRecognitionClient.unregisterConnectionFailedListener(this);

		LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
		Log.i("AS","onDisconnected: DONE!");
	}
}