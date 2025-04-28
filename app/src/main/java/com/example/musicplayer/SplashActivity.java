package com.example.musicplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Trouver le VideoView
        VideoView videoView = findViewById(R.id.musicvd);

        // Chemin de la vidéo (dans res/raw/)
        Uri videoPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.musicvd);

        // Charger la vidéo
        videoView.setVideoURI(videoPath);

        // Lancer la vidéo
        videoView.start();

        // Détection de la fin de la vidéo pour passer à MainActivity
        videoView.setOnCompletionListener(mp -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish(); // Fermer la SplashActivity
        });
    }
}