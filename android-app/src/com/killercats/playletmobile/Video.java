package com.killercats.playletmobile;

/** A video as shown in lists (home, search, related). */
public class Video {
    public String videoId = "";
    public String title = "";
    public String author = "";
    public String authorId = "";
    public long lengthSeconds;
    public long viewCount = -1;
    public String publishedText = "";
    public boolean live;

    /** 16:9-friendly thumbnail from YouTube's image server, cropped to fit by the card. */
    public String thumbnailUrl() {
        return "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
    }
}
