package com.example.musicplayer;

public class itempl {
    private String playlist,description;
    private int coverImg;

    public itempl(String title, String artist, int coverImg) {

        this.playlist = artist;
        this.description = title;
        this.coverImg = coverImg;
        ;
    }

    public String getTitle() {
        return playlist;
    }

    public void setTitle(String title) {
        this.playlist = title;
    }

    public String getArtist() {
        return description;
    }

    public void setArtist(String artist) {
        this.description = artist;
    }

    public int getCoverImg() {
        return coverImg;
    }

    public void setCoverImg(int coverImg) {
        this.coverImg = coverImg;
    }
}
