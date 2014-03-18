package com.example.publictransportation;

import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.publictransportation.modes.AbstractMode;

public class TextToSpeechActuator implements TextToSpeech.OnInitListener {

	TextToSpeech speech;
	public static Boolean speechOn = true;

	public TextToSpeechActuator(AbstractMode parentMode) {
		Context mContext = parentMode.getContext(); // get the context
		speech = new TextToSpeech(mContext, this);  
		String engine = speech.getDefaultEngine();  // get the default speech engine
		speech.setLanguage(Locale.US);				// set the language to US English
		speech = new TextToSpeech(mContext, this, engine);
		
	}
	
	// Call this method to say a message
	public void say(String message) {
		if(speechOn) {
			speech.speak(message, TextToSpeech.QUEUE_ADD, null);
			Log.d("TTS", "Just spoke this msg: "+message);
		}else{
			Log.d("TTS", "Text To Speech is turned off");
		}
	}	

	@Override
	public void onInit(int status) {
		// Could do some error handling her
	}
}
