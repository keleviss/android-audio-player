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

import java.util.Objects;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer;
    private String mediaFile;   //path to the audio file
    private int resumePosition; //Used to pause/resume MediaPlayer
    private AudioManager audioManager;

    String SERVICE_START = "service_start";
    String SERVICE_STOP = "service_stop";
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

    // Executed when the startService() method is called
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Objects.equals(intent.getAction(), SERVICE_START)) {
            ShowMessage("Service onStartCommand");
            createNotification("No Song");
        } else if (Objects.equals(intent.getAction(), SERVICE_SELECT_SONG)) {
            Bundle extras = intent.getExtras();
            Song song = (Song) extras.get("songData");
            ShowMessage("On Start: " + song.getTitle());
            createNotification(song.getTitle());
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
