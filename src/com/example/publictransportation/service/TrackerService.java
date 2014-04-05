package com.example.publictransportation.service;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.example.publictransportation.MainActivity;
import com.example.publictransportation.R;
import com.example.publictransportation.WidgetProvider;
import com.example.publictransportation.modes.AbstractMode;
import com.example.publictransportation.modes.BusMode;
import com.example.publictransportation.modes.DefaultMode;
import com.example.publictransportation.modes.ForcedMode;
import com.example.publictransportation.modes.MetroMode;
import com.example.publictransportation.modes.ModeTypes;
import com.example.publictransportation.modes.MovingMode;
import com.example.publictransportation.modes.STrainMode;
import com.example.publictransportation.modes.WaitingMode;
import com.example.publictransportation.profiles.AbstractProfile;
import com.example.publictransportation.profiles.DefaultProfile;

public class TrackerService extends Service implements IModeManager {

	AbstractMode mode;
	AbstractProfile profile;
	ModeChooserReceiver modeChooserReceiver;
	Handler handler;

	// System services
	NotificationManager notificationManager;
	Vibrator vibrator;

	public static final String FORCE_TRANSPORTATION_MODE = "TrackerService.CHANGE_TRANSPORTATION_MODE";

	@Override
	public void onCreate() {
		super.onCreate();

		profile = new DefaultProfile();

		handler = new Handler();
		
		notificationManager =  (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		modeChooserReceiver = new ModeChooserReceiver();
		registerReceiver(modeChooserReceiver, new IntentFilter(FORCE_TRANSPORTATION_MODE));
	}

	private void killMode() {
		// Kill the previous mode first
		if (mode != null) {
			mode.kill();
			mode = null;
		}
	}

	// called from outside through binder
	// ForcedMode identifies itself as newMode specified in paramter
	public void forceMode(ModeTypes newMode) {
		// update widgets on homescreen
		updateWidgets(newMode);

		// Kill the previous before creating a new one
		killMode();

		// MetroMode already includes exit logic similar to ForcedMode
		// this also prevents double notifications when exiting ForcedMode in metro tunnel
		if (newMode == ModeTypes.METRO) {
			mode = new MetroMode(profile, this);
		}
		// this gives the user the option to always switch to DefaultMode without notifications
		else if (newMode == ModeTypes.DEFAULT) {
			mode = new DefaultMode(profile, this);
		}
		// for busses and s-trains we enter this "fake" mode
		else {
			mode = new ForcedMode(profile, this, newMode);
		}
	}

	@Override
	public void changeMode(ModeTypes newMode, String latestMacAddress) {

		// the actual reminder code
		ModeTypes oldMode = mode.getType();
		if (newMode != ModeTypes.OFF && (oldMode == ModeTypes.BUS || oldMode == ModeTypes.S_TRAIN || oldMode == ModeTypes.METRO)) {
			showNotification(oldMode);
		}

		// update widgets on homescreen
		updateWidgets(newMode);

		// Kill the previous before creating a new one
		killMode();

		// then create and reference the new mode
		if (newMode == ModeTypes.DEFAULT) {
			mode = new DefaultMode(profile, this);
		}
		else if (newMode == ModeTypes.BUS) {
			mode = new BusMode(profile, this, latestMacAddress);
		}
		else if (newMode == ModeTypes.S_TRAIN) {
			mode = new STrainMode(profile, this, latestMacAddress);
		}
		else if (newMode == ModeTypes.METRO) {
			mode = new MetroMode(profile, this);
		}
		else if (newMode == ModeTypes.MOVING) {
			mode = new MovingMode(profile, this);
		}
		else if (newMode == ModeTypes.WAITING) {
			mode = new WaitingMode(profile, this);
		}
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("trackerservice","TrackerService.onStartCommand()");
		// debugging with this mode at the moment
		mode = new DefaultMode(profile, this);
		updateWidgets(ModeTypes.DEFAULT);

		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(modeChooserReceiver);
		updateWidgets(ModeTypes.OFF);
		killMode();

		// Change the widget to DefaultMode when the service is stopped - 
		// or else it will "hang" in another mode if the service is stopped in a mode which is not Default Mode
		Log.i("trackerservice","TrackerService.onDestroy()");
		super.onDestroy();
	}

	@Override
	public void abortOnMissingWifi() {
		// ignores abort calls from more than one source
		Log.i("trackerservice", "abortOnMissingWifi()");
		updateWidgets(ModeTypes.OFF);
		killMode();
		stopSelf();
	}

	public void updateWidgets(ModeTypes newMode) {
		// Get an instance of the AppWidgetManager
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

		// Extract the widgetIds of all the active widgets on the home screens
		ComponentName widgetType = new ComponentName(getApplicationContext(), WidgetProvider.class);
		int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetType);

		// Get an instance of the RemoteViews with the layout "widget_layout"
		RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout);

		// set up onClick action for forcing modes
		Intent toggleIntent = new Intent();
		toggleIntent.setAction(FORCE_TRANSPORTATION_MODE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);

		remoteViews.setTextViewText(R.id.notCorrect, "Not correct? Please click here.");
		remoteViews.setTextViewText(R.id.title, "You are currently travelling by...");

		if (newMode == ModeTypes.DEFAULT) {
			remoteViews.setTextViewText(R.id.title, "Your location is");
			remoteViews.setTextViewTextSize(R.id.modeOfTransport, TypedValue.COMPLEX_UNIT_SP, 42);
			remoteViews.setTextViewText(R.id.modeOfTransport, "Unknown");
			remoteViews.setImageViewResource(R.id.transportIcon, R.drawable.unknown);
		}
		else if (newMode == ModeTypes.OFF) {
			remoteViews.setTextViewText(R.id.title, "Service not running");
			remoteViews.setTextViewText(R.id.modeOfTransport, "Off");
			remoteViews.setImageViewResource(R.id.transportIcon, R.drawable.unknown);
		}
		else if (newMode == ModeTypes.S_TRAIN) {
			remoteViews.setTextViewText(R.id.modeOfTransport, getText(R.string.sTrain));
			remoteViews.setImageViewResource(R.id.transportIcon, R.drawable.stog);
		}
		else if (newMode == ModeTypes.BUS) {
			remoteViews.setTextViewText(R.id.modeOfTransport, getText(R.string.bus));
			remoteViews.setImageViewResource(R.id.transportIcon, R.drawable.bus);
		}
		else if (newMode == ModeTypes.METRO){
			remoteViews.setTextViewText(R.id.modeOfTransport, getText(R.string.metro));
			remoteViews.setImageViewResource(R.id.transportIcon, R.drawable.metro);
		}
		else if (newMode == ModeTypes.MOVING) {
			remoteViews.setTextViewText(R.id.modeOfTransport, "Foot");
			remoteViews.setImageViewResource(R.id.transportIcon, R.drawable.walking);
		}
		else if (newMode == ModeTypes.WAITING) {
			remoteViews.setTextViewText(R.id.title, "Phone is");
			remoteViews.setTextViewText(R.id.modeOfTransport, "Sleeping");
			remoteViews.setImageViewResource(R.id.transportIcon, R.drawable.na);
		}

		// Important: Update all the widgets with the newly changed text and images!
		for (int widgetId : widgetIds) {
			appWidgetManager.updateAppWidget(widgetId, remoteViews); // Update all the widgets!
		}
	}

	private void showNotification(ModeTypes oldMode) {

		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

		int oldModeIcon = R.drawable.na;
		String oldModeName = "???";

		if (oldMode == ModeTypes.BUS) {
			oldModeIcon = R.drawable.bus;
			oldModeName = (String) getText(R.string.bus);
		}
		else if (oldMode == ModeTypes.S_TRAIN) {
			oldModeIcon = R.drawable.stog;
			oldModeName = (String) getText(R.string.sTrain);
		}
		else if (oldMode == ModeTypes.METRO) {
			oldModeIcon = R.drawable.metro;
			oldModeName = (String) getText(R.string.metro);
		}

		// Simple notification
		Notification notification = new Notification.Builder(getApplicationContext())
		.setContentTitle(getText(R.string.gotOff) + oldModeName)
		.setContentText(getText(R.string.rememberToCheckOut))
		.setSmallIcon(oldModeIcon)
		.setContentIntent(pendingIntent).getNotification();

		// Hide the notification after its selected
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(0, notification);

		// TTS and notification sounds at the same time crashes the app!!
		if (profile.isSpeechOn() == false) {
			// play a sound
			try {
				Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
				ringtone.play();
			} catch (Exception e) {}

			// also vibrate for extra effect (gets annoying fast)
			vibrator.vibrate(2000);
		}
	}

	// this inner class deals with scanning results
	private class ModeChooserReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {

			handler.post(new Runnable() {

				@Override
				public void run() {
					showForceModeChooser();
				}
			});
		}
	}

	public void showForceModeChooser() {
		final CharSequence[] items = {
				"Bus", "S-train", "Metro", "None of the above"
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose correct mode");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				if (item == 0) {
					forceMode(ModeTypes.BUS);
				}
				else if (item == 1) {
					forceMode(ModeTypes.S_TRAIN);
				}
				else if (item == 2) {
					forceMode(ModeTypes.METRO);
				}
				else if (item == 3) {
					forceMode(ModeTypes.DEFAULT);
				}
			}
		});
		AlertDialog alert = builder.create();
		// THANK YOU TO THIS GUY:
		// http://tofu0913.blogspot.dk/2013/07/popup-alertdialog-in-android-service.html
		// Otherwise showing alertdialogs from services will crash the app
		alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		alert.show();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
