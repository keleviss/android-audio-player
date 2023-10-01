package com.example.servplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
    private Song currentSong;
    private AudioManager audioManager;
    //private ArrayList<Song> songsList;

    // Intent actions
    String SERVICE_PLAY_SONG = "service_play_song";
    String SERVICE_PAUSE_SONG = "service_pause_song";
    String SERVICE_NEXT_SONG = "service_next_song";
    String SERVICE_PREV_SONG = "service_prev_song";
    String SERVICE_SELECT_SONG = "service_select_song";
    String SERVICE_RESUME_SONG = "service_resume_song";
    String SERVICE_SEEKBAR_SONG = "service_seekbar_song";
    //String SERVICE_GET_SONGS = "service_get_songs";

    // Service Lifecycle Methods ===================================================================
    @Override
    public void onCreate() {
        initMediaPlayer();
    }

    // Executed when the user removes the app from the "recent apps" list
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        ShowMessage("Service onTaskRemoved");
        stopForeground(true);
        stopSelf();
        onDestroy();
    }

    @Override
    public void onDestroy() {
        ShowMessage("Service onDestroy");
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        removeAudioFocus();
    }

    // Executed when the startService() method is called

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

        // Specify the audio stream type for the media playback
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }
    // Used to handle incoming intents and start or manage the background tasks of the Service

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!requestAudioFocus())
            stopSelf();
        if (mediaPlayer == null)
            initMediaPlayer();

        if (Objects.equals(intent.getAction(), SERVICE_PLAY_SONG)) {
            prepareMedia("Media Playing", intent, "Media Playing");
        } else if (Objects.equals(intent.getAction(), SERVICE_SELECT_SONG)) {
            prepareMedia("Media Selected", intent, "Media Playing");
        } else if (Objects.equals(intent.getAction(), SERVICE_RESUME_SONG)) {
            ShowMessage("Media Resumed");
            resumeMedia();
        } else if (Objects.equals(intent.getAction(), SERVICE_PREV_SONG)) {
            prepareMedia("Media Skipped", intent, "Media Playing");
        } else if (Objects.equals(intent.getAction(), SERVICE_NEXT_SONG)) {
            prepareMedia("Media Skipped", intent, "Media Playing");
        } else if (Objects.equals(intent.getAction(), SERVICE_PAUSE_SONG)) {
            ShowMessage("Media Paused");
            createNotification(currentSong.getTitle(), "Media Paused");
            pauseMedia();
        } else if (Objects.equals(intent.getAction(), SERVICE_SEEKBAR_SONG)) {
            if (mediaPlayer != null) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    //noinspection DataFlowIssue
                    int currentPosition = (int) extras.get("current position");

                    if (mediaPlayer.isPlaying())
                        mediaPlayer.seekTo(currentPosition);
                    else
                        resumePosition = currentPosition;
                }
            }
        } /*else if (Objects.equals(intent.getAction(), SERVICE_GET_SONGS)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                songsList = new ArrayList<>();
                songsList = (ArrayList<Song>) extras.get("songsList");
                for (int i = 0; i < songsList.size(); i++) {
                    System.out.println("Song [" + i + "] : " + songsList.get(i).getTitle());
                }
            }
        }*/

        return START_STICKY;
    }
    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void resumeMedia() {
        mediaPlayer.seekTo(resumePosition);
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void stopMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            MyMediaPlayer.isStopped = true;
        }
    }

    // Prepare and start playing a media file asynchronously
    public void prepareMedia(String msg, Intent intent, String notificationStatus) {
        ShowMessage(msg);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            currentSong = (Song) extras.get("media");
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepareAsync();
            playMedia();
            createNotification(currentSong.getTitle(), notificationStatus);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    // Audio Playback Methods ======================================================================
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        /*if (mediaPlayer != null) {
            initMediaPlayer();
            ShowMessage("Media Completed");
            MyMediaPlayer.currentIndex++;
            if (MyMediaPlayer.currentIndex < songsList.size()) {
                currentSong = songsList.get(MyMediaPlayer.currentIndex);
                mediaPlayer.reset();
                try {
                    mediaPlayer.setDataSource(currentSong.getPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                mediaPlayer.prepareAsync();
                playMedia();
                createNotification(currentSong.getTitle(), "Media Playing");
            } else {
                MyMediaPlayer.currentIndex = 0;
                currentSong = songsList.get(MyMediaPlayer.currentIndex);
                mediaPlayer.reset();
                try {
                    mediaPlayer.setDataSource(currentSong.getPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                mediaPlayer.prepareAsync();
                playMedia();
                createNotification(currentSong.getTitle(), "Media Playing");
            }
        }*/
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
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        // Invoked when the audio focus of the system is updated.
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                ShowMessage("Audio Focus Gain");
                // start playback
                if (!mediaPlayer.isPlaying() && MyMediaPlayer.isStopped) playMedia();
                // resume playback
                if (!mediaPlayer.isPlaying() && MyMediaPlayer.isPaused) resumeMedia();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                ShowMessage("Audio Focus Loss");
                // Lost focus for an unbounded amount of time
                // stop playback
                stopMedia();
                mediaPlayer.release();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                ShowMessage("Audio Focus Loss Transient");
                // Lost focus for a short amount of time, but we have to pause playback
                // We don't release the media player because playback is likely to resume
                pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                ShowMessage("Audio Focus Loss Transient Can Duck");
                // Lost focus for a short amount of time, but it's ok to keep playing at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        // Return TRUE if focus was granted or FALSE if focus was not granted
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void removeAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }


    // Binding Methods =============================================================================
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

    private void ShowMessage(String Mess) {
        Toast Tst = Toast.makeText(getApplicationContext(), "Service: " + Mess, Toast.LENGTH_LONG);
        Tst.show();
    }
}
