package com.example.servplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ViewHolder> {

    ArrayList<Song> songsList;
    Context context;
    String SERVICE_SELECT_SONG = "service_select_song";

    public SongListAdapter(ArrayList<Song> songsList, Context context) {
        this.songsList = songsList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate song row item layout
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song songData = songsList.get(position);
        holder.titleTextView.setText(songData.getTitle());
        String duration = formatDuration(songData.getDuration());
        holder.durationTextView.setText(duration);

        holder.itemView.setOnClickListener(v -> {
            holder.relativeLayout.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6E39CC")));
            Intent songSelectIntent = new Intent(context, MediaPlayerService.class);
            songSelectIntent.putExtra("media", songData);
            songSelectIntent.setAction(SERVICE_SELECT_SONG);
            context.startService(songSelectIntent);
        });
    }

    @Override
    public int getItemCount() {
        return songsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout relativeLayout;
        TextView titleTextView;
        ImageView iconImageView;
        TextView durationTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleView);
            iconImageView = itemView.findViewById(R.id.artworkView);
            durationTextView = itemView.findViewById(R.id.durationView);
            relativeLayout = itemView.findViewById(R.id.song_panel);
            titleTextView.setSelected(true);
        }
    }

    @SuppressLint("DefaultLocale")
    public String formatDuration (String duration) {
        long millis = Long.parseLong(duration);

        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(millis);
        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (minutes > 60) {
            int hours = minutes / 60;
            minutes %= 60;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format("%02d:%02d", minutes, seconds);
    }

}
