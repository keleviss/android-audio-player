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

    private final MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    private int resumePosition; //Used to pause/resume MediaPlayer
    private boolean Paused = false;
    private Song currentSong;
    private AudioManager audioManager;

    String SERVICE_PLAY_SONG = "service_play_song";
    String SERVICE_PAUSE_SONG = "service_pause_song";
    String SERVICE_NEXT_SONG = "service_next_song";
    String SERVICE_PREV_SONG = "service_prev_song";
    String SERVICE_SELECT_SONG = "service_select_song";

    // Service Lifecycle Methods ===================================================================
    @Override
    public void onCreate() {
        initMediaPlayer();
        ShowMessage("Service onCreate");
    }

    private void initMediaPlayer() {

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
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Paused = false;
        }
    }

    private void resumeMedia() {
        mediaPlayer.seekTo(resumePosition);
        Paused = false;
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            Paused = true;
        }
    }

    private void stopMedia() {
        mediaPlayer.stop();
    }

    public void prepareMedia(String msg, Intent intent, String notificationStatus) {
        ShowMessage(msg);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            currentSong = (Song) extras.get("media");
        }
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepareAsync();
            createNotification(currentSong.getTitle(), notificationStatus);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    // Executed when the startService() method is called
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mediaPlayer == null)
            initMediaPlayer();

        if (Objects.equals(intent.getAction(), SERVICE_PLAY_SONG)) {
            if (Paused)
                resumeMedia();
            else {
                prepareMedia("Pressed Play", intent, "Media Playing");
                playMedia();
            }
        } else if (Objects.equals(intent.getAction(), SERVICE_SELECT_SONG)) {
            prepareMedia("Selected Song", intent, "Media Playing");
            playMedia();
        } else if (Objects.equals(intent.getAction(), SERVICE_PREV_SONG)) {
            prepareMedia("Skip Prev", intent, "Media Playing");
            playMedia();
        } else if (Objects.equals(intent.getAction(), SERVICE_NEXT_SONG)) {
            prepareMedia("Skip Next", intent, "Media Playing");
            playMedia();
        } else if (Objects.equals(intent.getAction(), SERVICE_PAUSE_SONG)) {
            Toast.makeText(this, "Pressed Pause", Toast.LENGTH_SHORT).show();
            createNotification(currentSong.getTitle(), "Media Paused");
            pauseMedia();
        }

        return START_STICKY;
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
        playMedia();
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

    public void createNotification(String songTitle, String status) {
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
                .setContentText(status)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification notification = notificationBuilder.build();

        // Start the service as a foreground service with the notification
        startForeground(NOTIFICATION_ID, notification);
    }
}
