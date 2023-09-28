package com.example.servplayer;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    RecyclerView recyclerView;
    TextView noMusicTextView;
    TextView songTitleTextView;
    ArrayList<Song> songsList = new ArrayList<>();
    ImageButton playPauseBtn, nextBtn, prevBtn;
    MediaPlayerService MusicServ;
    int currentSong;
    boolean Connected;

    String SERVICE_PLAY_SONG = "service_play_song";
    String SERVICE_RESUME_SONG = "service_resume_song";
    String SERVICE_PAUSE_SONG = "service_pause_song";
    String SERVICE_NEXT_SONG = "service_next_song";
    String SERVICE_PREV_SONG = "service_prev_song";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noMusicTextView = findViewById(R.id.no_music_available);
        songTitleTextView = findViewById(R.id.currentSongTitle);
        recyclerView = findViewById(R.id.recycler_view);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        nextBtn = findViewById(R.id.next_btn);
        prevBtn = findViewById(R.id.prev_btn);
        playPauseBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        prevBtn.setOnClickListener(this);

        checkExternalStoragePermission();

        Connected = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShowMessage("Activity On Destroy");
        if (Connected)
            unbindService(ServCon);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(playPauseBtn)) {
            if (MyMediaPlayer.isStopped) {
                playAudio();
                playPauseBtn.setImageResource(R.drawable.baseline_pause_45);
            } else if (MyMediaPlayer.isPaused) {
                resumeAudio();
                playPauseBtn.setImageResource(R.drawable.baseline_pause_45);
            } else {
                pauseAudio();
                playPauseBtn.setImageResource(R.drawable.baseline_play_arrow_50);
            }
        } else if (view.equals(prevBtn)) {
            if (!MyMediaPlayer.isStopped) {
                prevSong();
                playPauseBtn.setImageResource(R.drawable.baseline_pause_45);
            }
        } else if (view.equals(nextBtn)) {
            if (!MyMediaPlayer.isStopped) {
                nextSong();
                playPauseBtn.setImageResource(R.drawable.baseline_pause_45);
            }
        }
        //bindService(serviceInt, ServCon, Context.BIND_AUTO_CREATE);
    }

    void playAudio() {
        Intent playInt = new Intent(this, MediaPlayerService.class);
        playInt.setAction(SERVICE_PLAY_SONG);
        playInt.putExtra("media", songsList.get(currentSong));
        startService(playInt);
        MyMediaPlayer.isPaused = false;
        MyMediaPlayer.isStopped = false;
    }

    void resumeAudio() {
        Intent resumeInt = new Intent(this, MediaPlayerService.class);
        resumeInt.setAction(SERVICE_RESUME_SONG);
        startService(resumeInt);
        MyMediaPlayer.isPaused = false;
    }

    void pauseAudio() {
        Intent stopInt = new Intent(this, MediaPlayerService.class);
        stopInt.setAction(SERVICE_PAUSE_SONG);
        startService(stopInt);
        MyMediaPlayer.isPaused = true;
    }

    void prevSong() {
        currentSong = --MyMediaPlayer.currentIndex;

        Intent prevInt = new Intent(this, MediaPlayerService.class);
        prevInt.setAction(SERVICE_PREV_SONG);
        prevInt.putExtra("media", songsList.get(currentSong));
        startService(prevInt);

        MyMediaPlayer.isPaused = false;
    }

    void nextSong() {
        currentSong = ++MyMediaPlayer.currentIndex;

        Intent nextInt = new Intent(this, MediaPlayerService.class);
        nextInt.setAction(SERVICE_NEXT_SONG);
        nextInt.putExtra("media", songsList.get(currentSong));
        startService(nextInt);

        MyMediaPlayer.isPaused = false;
    }

    void loadAudioFiles() {
        songsList = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION};
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder);
        while (cursor != null && cursor.moveToNext()) {
            // Create new Song object and add it to the ArrayList
            Song songData = new Song(cursor.getString(1),cursor.getString(0), cursor.getString(2));
            if (new File(songData.getPath()).exists())
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
            /*SongListAdapter songListAdapter = new SongListAdapter(songsList, this);
            songListAdapter.setOnItemClickListener(new SongListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    playPauseBtn.setImageResource(R.drawable.baseline_pause_45);
                }
            });*/
        }
    }

    void checkExternalStoragePermission() {
        int selfPermission;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            selfPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_AUDIO);
        else
            selfPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);

        // Check if permission is granted
        if(selfPermission == PackageManager.PERMISSION_GRANTED)
        {
            // Permission is already granted, you can proceed with reading storage files
            loadAudioFiles();
        } else {
            // Permission is not granted, request it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 123);
            else
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
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
                ShowMessage("Permission denied...");
            }
        }
    }

    private final ServiceConnection ServCon = new ServiceConnection ()
    {

        @Override
        public void onServiceConnected (ComponentName className, IBinder service)
        {
            System.out.println ("***3");
            ShowMessage ("Connected to Service");
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            MusicServ = binder.getService();
            Connected = true;
        }

        @Override
        public void onServiceDisconnected (ComponentName CompNam)
        {
            System.out.println ("***4");
            ShowMessage ("Disconnected from Service");
            Connected = false;
        }
    };

    private void ShowMessage (String Mess)
    {
        Toast Tst = Toast.makeText (getApplicationContext (), "Service: " + Mess, Toast.LENGTH_LONG);
        Tst.show ();
    }
}