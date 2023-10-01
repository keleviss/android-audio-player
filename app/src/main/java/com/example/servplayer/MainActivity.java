package com.example.servplayer;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    RecyclerView recyclerView;
    TextView noMusicTextView;
    TextView songTitleTextView;
    SeekBar seekBar;
    ArrayList<Song> songsList = new ArrayList<>();
    ImageButton playPauseBtn, nextBtn, prevBtn;
    MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    //boolean Loaded = false;

    // Intent actions
    String SERVICE_PLAY_SONG = "service_play_song";
    String SERVICE_RESUME_SONG = "service_resume_song";
    String SERVICE_PAUSE_SONG = "service_pause_song";
    String SERVICE_NEXT_SONG = "service_next_song";
    String SERVICE_PREV_SONG = "service_prev_song";
    String SERVICE_SEEKBAR_SONG = "service_seekbar_song";
    //String SERVICE_GET_SONGS = "service_get_songs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ShowMessage("Activity onCreate");

        noMusicTextView = findViewById(R.id.no_music_available);
        songTitleTextView = findViewById(R.id.currentSongTitle);
        seekBar = findViewById(R.id.seekbar);
        seekBar.setProgress(0);
        recyclerView = findViewById(R.id.recycler_view);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        nextBtn = findViewById(R.id.next_btn);
        prevBtn = findViewById(R.id.prev_btn);
        playPauseBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        prevBtn.setOnClickListener(this);

        // Check for storage permission and request it if needed
        checkExternalStoragePermission();

        // Touch and update the application UI
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    // Constantly update the seekBar when the mediaPlayer is playing
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());

                    // Check which song is playing and update the song title text view
                    if (songTitleTextView.getText() != songsList.get(MyMediaPlayer.currentIndex).getTitle()) {
                        songTitleTextView.setSelected(true);
                        songTitleTextView.setText(songsList.get(MyMediaPlayer.currentIndex).getTitle());
                    }

                    // Set seekbar max based on the duration of the current song
                    if (seekBar.getMax() != mediaPlayer.getDuration()) {
                        seekBar.setMax(mediaPlayer.getDuration());
                    }

                    // Set the play/pause button to the pause image view
                    playPauseBtn.setImageResource(R.drawable.baseline_pause_45);
                } else if (MyMediaPlayer.isStopped || MyMediaPlayer.isPaused) {
                    // Set the play/pause button to the play image view
                    playPauseBtn.setImageResource(R.drawable.baseline_play_arrow_50);
                    songTitleTextView.setSelected(false);
                }
                new Handler().postDelayed(this, 50);
            }
        });

        // Listener for the changes that the user does on the seekbar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    Intent seekBarIntent = new Intent(MainActivity.this, MediaPlayerService.class);
                    seekBarIntent.setAction(SERVICE_SEEKBAR_SONG);
                    seekBarIntent.putExtra("current position", progress);
                    seekBarIntent.putExtra("current song", songsList.get(MyMediaPlayer.currentIndex));
                    startService(seekBarIntent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer = null;
        ShowMessage("Activity onDestroy");
    }

    @Override
    public void onClick(View view) {
        if (view.equals(playPauseBtn)) {
            if (MyMediaPlayer.isStopped) {
                //ShowMessage("Pressed Play Button");
                playAudio();
            } else if (MyMediaPlayer.isPaused) {
                //ShowMessage("Pressed Resume Button");
                resumeAudio();
            } else {
                //ShowMessage("Pressed Pause Button");
                pauseAudio();
            }
        } else if (view.equals(prevBtn)) {
            if (!MyMediaPlayer.isStopped) {
                //ShowMessage("Pressed Skip Button");
                prevSong();
            }
        } else if (view.equals(nextBtn)) {
            if (!MyMediaPlayer.isStopped) {
                //ShowMessage("Pressed Skip Button");
                nextSong();
            }
        }
    }

    void playAudio() {
        MyMediaPlayer.isPaused = false;
        MyMediaPlayer.isStopped = false;

        Intent playInt = new Intent(this, MediaPlayerService.class);
        playInt.setAction(SERVICE_PLAY_SONG);
        playInt.putExtra("media", songsList.get(MyMediaPlayer.currentIndex));
        startService(playInt);
    }

    void resumeAudio() {
        MyMediaPlayer.isPaused = false;

        Intent resumeInt = new Intent(this, MediaPlayerService.class);
        resumeInt.setAction(SERVICE_RESUME_SONG);
        startService(resumeInt);
    }

    void pauseAudio() {
        MyMediaPlayer.isPaused = true;

        Intent stopInt = new Intent(this, MediaPlayerService.class);
        stopInt.setAction(SERVICE_PAUSE_SONG);
        startService(stopInt);
    }

    void prevSong() {
        MyMediaPlayer.isPaused = false;
        MyMediaPlayer.currentIndex--;

        Intent prevInt = new Intent(this, MediaPlayerService.class);
        prevInt.setAction(SERVICE_PREV_SONG);
        prevInt.putExtra("media", songsList.get(MyMediaPlayer.currentIndex));
        startService(prevInt);
    }

    void nextSong() {
        MyMediaPlayer.isPaused = false;
        MyMediaPlayer.currentIndex++;

        Intent nextInt = new Intent(this, MediaPlayerService.class);
        nextInt.setAction(SERVICE_NEXT_SONG);
        nextInt.putExtra("media", songsList.get(MyMediaPlayer.currentIndex));
        startService(nextInt);
    }

    // Load audio files from the devices external storage using query with the cursor class
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

            /*if (!Loaded) {
                Intent loadIntent = new Intent(this, MediaPlayerService.class);
                loadIntent.setAction(SERVICE_GET_SONGS);
                loadIntent.putExtra("songsList", songsList);
                startService(loadIntent);
                Loaded = true;
            }*/

        }
    }

    // Check external storage permission and request if it needed
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

    // Toast message creation method
    private void ShowMessage (String Mess)
    {
        Toast Tst = Toast.makeText (getApplicationContext (), "Service: " + Mess, Toast.LENGTH_LONG);
        Tst.show ();
    }
}