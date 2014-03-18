package com.example.publictransportation.modes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityResults {
	private List<String> results;
	final int MAX_RESULTS;
	
	public ActivityResults(int max) {
		results = new ArrayList<String>();
		MAX_RESULTS = max;
	}
	
	public void add(String result) {
		
		// limit to max size
		if (results.size() == MAX_RESULTS) {
			results.remove(0);
		}
		
		results.add(result);
	}
	
	// results presented in reverse
	public List<String> getLatestResults() {
		
		// present results in LIFO order
		List<String> copy = new ArrayList<String>(results); 
		Collections.reverse(copy);
		
		return copy;
	}
	
	public int size() {
		return results.size();
	}
}
