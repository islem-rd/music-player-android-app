package com.example.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class item implements Parcelable, Serializable {
    private String singer;
    private String song;
    private String path;
    private String duree;
    private String img;
    private boolean hasLoadedImage = false;

    public item(String singer, String song, String path, String duree, String img) {
        this.singer = singer;
        this.song = song;
        this.path = path;
        this.duree = duree;
        this.img = img;
    }

    protected item(Parcel in) {
        singer = in.readString();
        song = in.readString();
        path = in.readString();
        duree = in.readString();
        img = in.readString();
        hasLoadedImage = in.readByte() != 0;
    }

    public static final Creator<item> CREATOR = new Creator<item>() {
        @Override
        public item createFromParcel(Parcel in) {
            return new item(in);
        }

        @Override
        public item[] newArray(int size) {
            return new item[size];
        }
    };

    public String getSinger() {
        return singer;
    }

    public String getSong() {
        return song;
    }

    public String getPath() {
        return path;
    }

    public String getDuree() {
        return duree;
    }

    public String getImg() {
        return img;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDuree(String duree) {
        this.duree = duree;
    }

    public void setImg(String img) {
        this.img = img;
        this.hasLoadedImage = img != null && !img.isEmpty();
    }

    public boolean hasLoadedImage() {
        return hasLoadedImage;
    }

    public void setHasLoadedImage(boolean hasLoadedImage) {
        this.hasLoadedImage = hasLoadedImage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(singer);
        dest.writeString(song);
        dest.writeString(path);
        dest.writeString(duree);
        dest.writeString(img);
        dest.writeByte((byte) (hasLoadedImage ? 1 : 0));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        item other = (item) obj;
        // Compare by path which should be unique
        return path != null && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
}