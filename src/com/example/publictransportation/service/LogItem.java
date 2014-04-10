package com.example.publictransportation.service;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogItem {
	private LogTypes type;
	private String data;
	private Date date;
	
	public LogItem(LogTypes type, String data) {
		this.type = type;
		this.data = data;
		this.date = new Date();
	}
	
	@Override
	public String toString() {
		Format formatter = new SimpleDateFormat("dd/MM hh:mm:ss");
		String formattedDate = formatter.format(date);
		return formattedDate + ", " + String.valueOf(type) + ", " + data;
	}
}
