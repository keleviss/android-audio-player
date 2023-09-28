package com.example.servplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.Objects;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer;
    private int resumePosition; //Used to pause/resume MediaPlayer
    private boolean Paused = false;
    private AudioManager audioManager;

    String SERVICE_PLAY_SONG = "service_play_song";
    String SERVICE_STOP_SONG = "service_stop_song";
    String SERVICE_NEXT_SONG = "service_next_song";
    String SERVICE_PREV_SONG = "service_prev_song";
    String SERVICE_SELECT_SONG = "service_select_song";

    // Service Lifecycle Methods ===================================================================
    @Override
    public void onCreate() {
        ShowMessage("Service onCreate");
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();

        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);

        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private void playMedia() {
        if (mediaPlayer != null) {
            if (!mediaPlayer.isPlaying() && !Paused) {
                mediaPlayer.start();
            } else if (!mediaPlayer.isPlaying() && Paused) {
                mediaPlayer.seekTo(resumePosition);
                mediaPlayer.start();
                Paused = false;
            }
        }
    }

    private void stopMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            Paused = false;
        }
    }

    private void pauseMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            Paused = true;
        }
    }

    public void prepareMedia(String msg, Intent intent) {
        ShowMessage(msg);
        Bundle extras = intent.getExtras();
        Song song = (Song) extras.get("media");
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(song.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
        createNotification(song.getTitle());
    }

    // Executed when the startService() method is called
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mediaPlayer == null)
            initMediaPlayer();

        if (intent.getAction() == SERVICE_PLAY_SONG) {
            prepareMedia("Pressed Play", intent);
            playMedia();
        } else if (intent.getAction() == SERVICE_SELECT_SONG) {
            stopMedia();
            initMediaPlayer();
            prepareMedia("Song Selected", intent);
            playMedia();
        } else if (intent.getAction() == SERVICE_PREV_SONG) {
            stopMedia();
            initMediaPlayer();
            prepareMedia("Pressed Prev", intent);
            playMedia();
        } else if (intent.getAction() == SERVICE_NEXT_SONG) {
            stopMedia();
            initMediaPlayer();
            prepareMedia("Pressed Next", intent);
            playMedia();
        } else if (intent.getAction() == SERVICE_STOP_SONG) {
            pauseMedia();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    // Executed when the user removes the app from the "recent apps" list
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        ShowMessage("Service onTaskRemoved");
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        ShowMessage("Service onDestroy");
    }

    // Audio Playback Methods ======================================================================
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onAudioFocusChange(int i) {

    }

    private void ShowMessage (String Mess)
    {
        Toast Tst = Toast.makeText (getApplicationContext (), "Service: " + Mess, Toast.LENGTH_LONG);
        Tst.show ();
    }

    // Binding Section =============================================================================

    private final IBinder iBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        ShowMessage("Binded...");
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        ShowMessage ("Unbinded...");
        return false;              //Allow Rebind? For started services
    }

    // Create Foreground Notification ==============================================================

    private static final String CHANNEL_ID = "Serv Player";
    private static final int NOTIFICATION_ID = 1;

    public void createNotification(String songTitle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Your Channel Name",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Create a notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.rectangle_icon_nobg)
                .setContentTitle(songTitle)
                .setContentText("Ready for playback")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification notification = notificationBuilder.build();

        // Start the service as a foreground service with the notification
        startForeground(NOTIFICATION_ID, notification);
    }
}
