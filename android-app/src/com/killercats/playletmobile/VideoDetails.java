package com.killercats.playletmobile;

import java.util.ArrayList;
import java.util.List;

/** Everything the player screen shows for one video. */
public class VideoDetails {
    public Video video = new Video();
    public String description = "";
    public long likeCount = -1;
    public String subCountText = "";
    public final List<Stream> streams = new ArrayList<Stream>();
    public final List<Video> related = new ArrayList<Video>();

    /** A muxed (audio + video) stream the built-in player can play directly. */
    public static class Stream {
        public String url = "";
        public String quality = "";
        public int height;
    }
}
