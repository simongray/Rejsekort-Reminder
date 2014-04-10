package com.example.publictransportation.service;

public class LogItem {
	private LogTypes type;
	private String data;
	
	public LogItem(LogTypes type, String data) {
		this.type = type;
		this.data = data;
	}
	
	@Override
	public String toString() {
		return String.valueOf(type) + ": " + data;
	}
}
