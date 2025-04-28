package com.example.musicplayer;

public class suggestion {
    public static final int TYPE_SONG = 1;
    public static final int TYPE_ARTIST = 2;
    public static final int TYPE_PLAYLIST = 3;

    private String text;
    private int type;


    private item song;  // 🔥 Optional full song if it's a TYPE_SONG


    public suggestion(item song, int type) {
        this.text = song.getSong();
        this.song = song;
        this.type = type;
    }


    public item getSong() {
        return song;
    }
    public suggestion(String text, int type) {
        this.text = text;
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public int getType() {
        return type;
    }

    public String getTypeString() {
        switch (type) {
            case TYPE_SONG:
                return "Song";
            case TYPE_ARTIST:
                return "Artist";
            case TYPE_PLAYLIST:
                return "Playlist";
            default:
                return "";
        }
    }
}
