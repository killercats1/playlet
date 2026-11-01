package com.killercats.playletmobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Turns Invidious API responses into models. Pure (no Android classes), so it is tested on the JVM. */
public final class Parser {
    private Parser() {}

    /** /api/v1/trending, /api/v1/popular, /api/v1/search: an array of items; non-video items are skipped. */
    public static List<Video> videos(String json) throws Exception {
        List<Video> out = new ArrayList<Video>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            String type = item.optString("type", "video");
            if (!"video".equals(type) && !"shortVideo".equals(type)) continue;
            Video v = video(item);
            if (v != null) out.add(v);
        }
        return out;
    }

    public static Video video(JSONObject item) {
        String id = item.optString("videoId", "");
        if (id.isEmpty()) return null;
        Video v = new Video();
        v.videoId = id;
        v.title = item.optString("title", "");
        v.author = item.optString("author", "");
        v.authorId = item.optString("authorId", "");
        v.lengthSeconds = item.optLong("lengthSeconds", 0);
        v.viewCount = item.optLong("viewCount", -1);
        v.publishedText = item.optString("publishedText", "");
        v.live = item.optBoolean("liveNow", false);
        return v;
    }

    /** /api/v1/videos/:id */
    public static VideoDetails details(String json, String instance) throws Exception {
        JSONObject o = new JSONObject(json);
        if (o.has("error")) throw new Exception(o.optString("error"));
        VideoDetails d = new VideoDetails();
        Video v = video(o);
        if (v == null) throw new Exception("No video in response");
        d.video = v;
        d.description = o.optString("description", "");
        d.likeCount = o.optLong("likeCount", -1);
        d.subCountText = o.optString("subCountText", "");

        JSONArray formats = o.optJSONArray("formatStreams");
        if (formats != null) {
            for (int i = 0; i < formats.length(); i++) {
                JSONObject f = formats.optJSONObject(i);
                if (f == null) continue;
                String url = f.optString("url", "");
                if (url.isEmpty()) continue;
                VideoDetails.Stream s = new VideoDetails.Stream();
                s.url = absolute(url, instance);
                s.quality = f.optString("qualityLabel", f.optString("quality", ""));
                // "720p" labels first; "1280x720" resolutions give the height after the x
                s.height = heightOf(s.quality);
                if (s.height == 0) s.height = heightOf(f.optString("resolution", ""));
                d.streams.add(s);
            }
        }

        JSONArray related = o.optJSONArray("recommendedVideos");
        if (related != null) {
            for (int i = 0; i < related.length(); i++) {
                JSONObject r = related.optJSONObject(i);
                if (r == null) continue;
                Video rv = video(r);
                if (rv != null) d.related.add(rv);
            }
        }
        return d;
    }

    /** api.invidious.io/instances.json: [[host, {api, type, uri, ...}], ...] -> https instances with the API on. */
    public static List<String> instances(String json) throws Exception {
        List<String> out = new ArrayList<String>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONArray entry = array.optJSONArray(i);
            if (entry == null || entry.length() < 2) continue;
            JSONObject info = entry.optJSONObject(1);
            if (info == null) continue;
            if (!"https".equals(info.optString("type"))) continue;
            if (!info.optBoolean("api", false)) continue;
            String uri = info.optString("uri", "");
            if (uri.startsWith("https://")) out.add(trimSlash(uri));
        }
        return out;
    }

    /** The playable stream closest to (not above) the wanted height, else the smallest one. */
    public static VideoDetails.Stream pickStream(List<VideoDetails.Stream> streams, int maxHeight) {
        VideoDetails.Stream best = null;
        VideoDetails.Stream smallest = null;
        for (VideoDetails.Stream s : streams) {
            if (smallest == null || s.height < smallest.height) smallest = s;
            if (s.height <= maxHeight && (best == null || s.height > best.height)) best = s;
        }
        return best != null ? best : smallest;
    }

    /** "720p", "720p60" or "1280x720" -> 720; anything else -> 0. */
    static int heightOf(String resolution) {
        if (resolution == null) return 0;
        int x = resolution.indexOf('x');
        if (x >= 0) resolution = resolution.substring(x + 1);
        StringBuilder digits = new StringBuilder();
        for (char c : resolution.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
            else if (digits.length() > 0) break;
        }
        try {
            return digits.length() == 0 ? 0 : Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String absolute(String url, String instance) {
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("/")) return trimSlash(instance) + url;
        return url;
    }

    static String trimSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
