package com.example.publictransportation.sensors;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Service that receives ActivityRecognition updates. It receives updates
 * in the background, even if the main Activity is not visible.
 */
public class ActivitySensorIntentService extends IntentService { 

	int cutoff;

	final public static String CONFIDENCE = "CONFIDENCE";
	final public static String ACTIVITY = "ACTIVITY";
	final public static String ACTIVITY_RECOGNITION_DATA = "ACTIVITY_RECOGNITION_DATA";

	public ActivitySensorIntentService() {
		// Set the label for the service's background thread
		super("ActivitySensorIntentService");
		Log.i("AS Service", "got to the intent service!");
	}

	/**
	 * Called when a new activity detection update is available.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i("AS Service", "onHandleIntent: Got here!");
		if (ActivityRecognitionResult.hasResult(intent)) {
			// Get the update
			ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

			DetectedActivity mostProbableActivity = result.getMostProbableActivity();

			// Get the confidence % (probability)
			int confidence = mostProbableActivity.getConfidence();

			// Get the type
			int activityType = mostProbableActivity.getType();

			// process 
			Intent broadcastIntent = new Intent(ACTIVITY_RECOGNITION_DATA);
			broadcastIntent.putExtra(ACTIVITY, activityType);
			broadcastIntent.putExtra(CONFIDENCE, confidence);
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

			Log.i("AS Service","Sent a local broardcast with the activity data.");

		}
		Log.i("AS", "onHandleIntent called");
	}
}