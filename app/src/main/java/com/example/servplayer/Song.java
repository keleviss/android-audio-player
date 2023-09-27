package com.example.servplayer;

import java.io.Serializable;

public class Song implements Serializable {

    String path;
    String title;
    String duration;

    public Song(String path, String title, String duration) {
        this.path = path;
        this.title = title;
        this.duration = duration;
    }

    public String getPath() { return path; }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }
}
