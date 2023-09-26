package com.example.servplayer;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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

    void loadAudioFiles() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION};
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder);

        while (cursor != null && cursor.moveToNext()) {
            // Create new Song object and add it to the ArrayList
            Song songData = new Song(cursor.getString(1),cursor.getString(0), cursor.getString(2));
            songsList.add(songData);
        }

        // Close cursor once we are done with it
        if (cursor != null) {
            cursor.close();
        }

        if (songsList.size() == 0) {
            noMusicTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new SongListAdapter(songsList, getApplicationContext()));
        }
    }

    // Check External Storage Permission
    void checkExternalStoragePermission() {
        int selfPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        // Check if permission is not granted
        if (selfPermission != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
        } else {
            // Permission is already granted, you can proceed with reading storage files
            // Your code to access and read audio files goes here
            loadAudioFiles();
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
                loadAudioFiles();
            } else {
                // Permission denied, handle it gracefully (e.g., show a message to the user)
                Toast.makeText(this, "Permission denied...", Toast.LENGTH_LONG).show();
            }
        }
    }

}