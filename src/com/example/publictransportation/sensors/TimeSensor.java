package com.example.publictransportation.sensors;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;

import com.example.publictransportation.modes.AbstractMode;

/*
 * This is a "fake" sensor... in that it actually isn't a sensor,
 * but rather a simple timer placed into the existing Mode/Sensor logic.
 * 
 * In this way, stopwatch operations are implemented the same as any other sensor.
 */
public class TimeSensor extends AbstractSensor {

	Timer timer;
	TimerTask timerTask;
	Handler handler;
	final static String TICK = "TICK";

	public TimeSensor(AbstractMode parentMode, int delayInMilliseconds) {
		super(parentMode);
		
		handler = new Handler();

		timer = new Timer();
		timerTask = new TimerTask() {

			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override 
					public void run(){
						output(TICK);
					}});
			}

		};

		timer.scheduleAtFixedRate(timerTask, delayInMilliseconds, delayInMilliseconds);
	}

	@Override
	public void kill() {
		timerTask.cancel();
		timer.cancel();
	}

	@Override
	public SensorTypes getType() {
		return SensorTypes.TIME;
	}

	@Override
	public String getLabel() {
		return "timer";
	}

}
