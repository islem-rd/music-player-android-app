package com.example.musicplayer;

public class MusicTrack {
    private long id;
    private String title;
    private String artist;
    private String album;
    private String path;
    private String albumArt;
    private String deezerLink;
    private String spotifyUrl;

    public String getSpotifyUrl() {
        return spotifyUrl;
    }

    public void setSpotifyUrl(String spotifyUrl) {
        this.spotifyUrl = spotifyUrl;
    }
    public MusicTrack() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(String albumArt) {
        this.albumArt = albumArt;
    }
    public String getDeezerLink() {
        return deezerLink;
    }

    public void setDeezerLink(String deezerLink) {
        this.deezerLink = deezerLink;
    }
}