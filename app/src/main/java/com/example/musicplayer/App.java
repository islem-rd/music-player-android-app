package com.example.musicplayer;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class App  extends Application {
    @Override
    public void onCreate(){
        super.onCreate();
        // Initialize Firebase with your app's context
        FirebaseApp.initializeApp(this);
    }
}
