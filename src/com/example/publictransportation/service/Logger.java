package com.example.publictransportation.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

public class Logger {

	String filename;
	ArrayList<LogItem> items;

	int BUFFER_LIMIT = 50;


	public Logger() {
		Log.i("Logger", "constructor");
		filename = Environment.getExternalStorageDirectory() + File.separator + "rejsereminder.log";
		Log.i("logger", "writing to: " + filename);
		items = new ArrayList<LogItem>();
	}

	public void log(LogTypes type, String data) {
		Log.i("Logger", "log()");
		LogItem item = new LogItem(type, data);
		items.add(item);

		if (items.size() == BUFFER_LIMIT) {
			writeToFile();
		}
	}

	private void writeToFile() {
		Log.i("Logger", "writeToFile()");
		File logFile = new File(filename);
		
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			// BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
			for (LogItem item : items) {
				buf.append(item.toString());
				buf.newLine();
			}
			buf.close();
			Log.i("logger", "successfully wrote to log file");
		}
		catch (IOException e) {
			e.printStackTrace();
			Log.e("logger", "couldn't write to log file!!");
		}
	}

	public void kill() {
		Log.i("Logger", "Kill()");
		writeToFile();
	}
}
