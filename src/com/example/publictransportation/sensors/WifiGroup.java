package com.example.publictransportation.sensors;

public class WifiGroup {
	
	String label;
	String[] whitelistedSsids;
	
	public WifiGroup(String label, String[] whitelistedSsids) {
		this.label = label;
		this.whitelistedSsids = whitelistedSsids;
	}
}