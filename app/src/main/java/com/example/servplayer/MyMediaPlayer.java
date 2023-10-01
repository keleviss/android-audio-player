package com.example.servplayer;

import android.media.MediaPlayer;

public class MyMediaPlayer extends MediaPlayer
{
    static MediaPlayer instance;

    // Create and return a singleton instance of the MediaPlayer
    public static MediaPlayer getInstance() {
        if (instance == null) {
            instance = new MediaPlayer();
        }
        return instance;
    }

    public static int currentIndex = 0;
    public static boolean isPaused = true;
    public static boolean isStopped = true;
}
