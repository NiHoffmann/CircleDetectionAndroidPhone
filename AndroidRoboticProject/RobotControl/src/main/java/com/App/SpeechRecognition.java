package com.App;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechRecognition {
    private SpeechRecognizer speech;
    private Intent recognizerIntent;
    private RobotBehaviour robotBehaviour;
    private int speed;

    public void startSpeechRecognition() {
        speech.startListening(recognizerIntent);
    }

    public SpeechRecognition(Context context, RobotBehaviour robotBehaviour, int speed){
        this.robotBehaviour = robotBehaviour;
        this.speed = speed;

        RecognitionListener rec = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.i("speech","ready for speech");
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                Log.i("speech","end of speech");
            }

            @Override
            public void onError(int error) {
                Log.i("speech","error"+error);
            }

            @Override
            public void onResults(Bundle results) {
                Log.i("speech", "onResults");
                ArrayList<String> matches = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = "";
                for (String result : matches)
                    text += result;
                Log.i("speech",text);
                doSomethingWithSpeech(text);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                Log.i("speech","partial result");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        };
        speech = SpeechRecognizer.createSpeechRecognizer(context);
        Log.i("speech", "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(context));

        speech.setRecognitionListener(rec);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "de");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    public void doSomethingWithSpeech(String text){

        speech.stopListening();

        text = text.toLowerCase();
        if(text.contains("links")){
            robotBehaviour.updateMotor(-speed,speed);
            return;
        }
        if(text.contains("rechts")){
            robotBehaviour.updateMotor(speed,-speed);
            return;
        }
        if(text.contains("stop")){
            robotBehaviour.stop();
            return;
        }
        if(text.contains("vorwärts")){
            robotBehaviour.updateMotor(speed,speed);
            return;
        }
        if(text.contains("rückwärts")){
            robotBehaviour.updateMotor(-speed,-speed);
            return;
        }
    }

}
