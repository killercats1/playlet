package com.killercats.playletmobile;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Talks to an Invidious server (the open-source YouTube front end) for feeds, search and video streams, so the
 * app shows no ads and needs no Google account. Public servers come and go: a request that fails is retried on
 * the next known server, and the one that worked is remembered. Call only from a background thread.
 */
public class Invidious {
    /** Used until the public list (api.invidious.io) has been fetched once. */
    static final String[] DEFAULT_INSTANCES = {
        "https://inv.nadeko.net",
        "https://yewtu.be",
        "https://invidious.nerdvpn.de",
        "https://invidious.jing.rocks",
        "https://invidious.privacyredirect.com",
    };

    private static final String PREFS = "invidious";
    private static final String KEY_INSTANCE = "instance";
    private static final String KEY_AUTO = "auto";
    private static final String KEY_KNOWN = "known";
    private static final String KEY_QUALITY = "quality";

    private final SharedPreferences prefs;

    public Invidious(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- settings ----------

    public String currentInstance() {
        return prefs.getString(KEY_INSTANCE, DEFAULT_INSTANCES[0]);
    }

    /** Automatic: on failure, move on to other servers (on by default). Off: always use the chosen one. */
    public boolean isAuto() {
        return prefs.getBoolean(KEY_AUTO, true);
    }

    public void choose(String instance, boolean auto) {
        prefs.edit().putString(KEY_INSTANCE, Parser.trimSlash(instance.trim())).putBoolean(KEY_AUTO, auto).apply();
    }

    public int maxQuality() {
        return prefs.getInt(KEY_QUALITY, 720);
    }

    public void setMaxQuality(int height) {
        prefs.edit().putInt(KEY_QUALITY, height).apply();
    }

    public List<String> knownInstances() {
        LinkedHashSet<String> all = new LinkedHashSet<String>();
        all.add(currentInstance());
        String known = prefs.getString(KEY_KNOWN, "");
        for (String s : known.split("\n")) if (!s.trim().isEmpty()) all.add(s.trim());
        for (String s : DEFAULT_INSTANCES) all.add(s);
        return new ArrayList<String>(all);
    }

    /** Refreshes the server list from api.invidious.io. */
    public List<String> refreshInstances() throws Exception {
        List<String> list = Parser.instances(get("https://api.invidious.io/instances.json?sort_by=health"));
        if (!list.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String s : list) sb.append(s).append('\n');
            prefs.edit().putString(KEY_KNOWN, sb.toString()).apply();
        }
        return knownInstances();
    }

    // ---------- API ----------

    /** type: "" (now), "music", "gaming" or "movies". */
    public List<Video> trending(String type) throws Exception {
        String path = "/api/v1/trending?region=" + region() + (type.isEmpty() ? "" : "&type=" + type);
        return Parser.videos(api(path).body);
    }

    public List<Video> search(String query, int page) throws Exception {
        String path = "/api/v1/search?type=video&page=" + page + "&q=" + URLEncoder.encode(query, "UTF-8");
        return Parser.videos(api(path).body);
    }

    /** local=true: stream URLs go through the Invidious server (direct Google links refuse other IPs). */
    public VideoDetails video(String videoId) throws Exception {
        Response r = api("/api/v1/videos/" + URLEncoder.encode(videoId, "UTF-8") + "?local=true");
        return Parser.details(r.body, r.instance);
    }

    static class Response {
        String body;
        String instance;
    }

    private Response api(String path) throws Exception {
        List<String> candidates = new ArrayList<String>();
        candidates.add(currentInstance());
        if (isAuto()) {
            for (String s : knownInstances()) if (!candidates.contains(s)) candidates.add(s);
        }
        Exception last = null;
        int tries = 0;
        for (String instance : candidates) {
            if (tries++ >= 6) break;
            try {
                Response r = new Response();
                r.body = get(instance + path);
                r.instance = instance;
                if (!instance.equals(currentInstance())) {
                    prefs.edit().putString(KEY_INSTANCE, instance).apply();
                }
                return r;
            } catch (Exception e) {
                last = e;
            }
        }
        throw new Exception("No Invidious server answered" + (last != null ? " (" + last.getMessage() + ")" : ""));
    }

    static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "PlayletMobile/1.0 (Android)");
        c.setRequestProperty("Accept", "application/json");
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            String body = out.toString("UTF-8");
            String trimmed = body.trim();
            if (!(trimmed.startsWith("[") || trimmed.startsWith("{"))) throw new Exception("Not an API response");
            return body;
        } finally {
            c.disconnect();
        }
    }

    private static String region() {
        String country = java.util.Locale.getDefault().getCountry();
        return country == null || country.length() != 2 ? "US" : country;
    }
}
