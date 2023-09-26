package com.example.servplayer;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView noMusicTextView;
    ArrayList<Song> songsList = new ArrayList<>();
    ImageView songIcon;
    TextView songTitle;
    ImageView playPauseBtn;
    Song currentSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        songIcon = findViewById(R.id.songPlayingIcon);
        songTitle = findViewById(R.id.songPlayingTitle);
        playPauseBtn = findViewById(R.id.playPause);

        checkExternalStoragePermission();

    }

    void checkExternalStoragePermission() {
        int selfPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        // Check if permission is not granted
        if (selfPermission != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
        } else {
            // Permission is already granted, you can proceed with reading storage files
            // Your code to access and read audio files goes here
        }
    }

    // Handle the permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed with reading storage files
                // Your code to access and read audio files goes here
            } else {
                // Permission denied, handle it gracefully (e.g., show a message to the user)
                Toast.makeText(this, "Permission denied...", Toast.LENGTH_LONG).show();
            }
        }
    }

}