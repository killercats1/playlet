import com.killercats.playletmobile.Format;
import com.killercats.playletmobile.Parser;
import com.killercats.playletmobile.Video;
import com.killercats.playletmobile.VideoDetails;

import java.util.List;

/** Runs the app's Invidious parsing and formatting on the JVM (org.json stands in for Android's). See run-tests.sh. */
public class ParserTest {
    static int failures = 0;

    static void check(String name, Object actual, Object expected) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) {
            failures++;
            System.out.println("FAIL " + name + ": got [" + actual + "] expected [" + expected + "]");
        }
    }

    public static void main(String[] args) throws Exception {
        String trending = "[{\"type\":\"video\",\"title\":\"First\",\"videoId\":\"aaaaaaaaaaa\",\"author\":\"Chan\",\"authorId\":\"UC1\","
            + "\"lengthSeconds\":754,\"viewCount\":1234567,\"publishedText\":\"2 days ago\",\"liveNow\":false},"
            + "{\"type\":\"channel\",\"author\":\"Not a video\"},"
            + "{\"type\":\"video\",\"title\":\"Live one\",\"videoId\":\"bbbbbbbbbbb\",\"lengthSeconds\":0,\"liveNow\":true},"
            + "{\"type\":\"video\",\"title\":\"No id\"}]";
        List<Video> videos = Parser.videos(trending);
        check("video count", videos.size(), 2);
        check("title", videos.get(0).title, "First");
        check("meta", Format.meta(videos.get(0)), "Chan · 1.2M views · 2 days ago");
        check("duration", Format.duration(videos.get(0).lengthSeconds), "12:34");
        check("live meta", Format.meta(videos.get(1)), "LIVE");
        check("thumbnail", videos.get(0).thumbnailUrl(), "https://i.ytimg.com/vi/aaaaaaaaaaa/hqdefault.jpg");

        String video = "{\"title\":\"Watch me\",\"videoId\":\"ccccccccccc\",\"author\":\"Chan\",\"viewCount\":999,"
            + "\"likeCount\":12345,\"subCountText\":\"1.5M\",\"description\":\"Line\\nTwo\","
            + "\"formatStreams\":[{\"url\":\"/latest_version?id=ccccccccccc&itag=18&local=true\",\"itag\":\"18\",\"qualityLabel\":\"360p\",\"resolution\":\"640x360\"},"
            + "{\"url\":\"https://x.example/videoplayback?itag=22\",\"itag\":\"22\",\"qualityLabel\":\"720p\",\"resolution\":\"1280x720\"}],"
            + "\"recommendedVideos\":[{\"videoId\":\"ddddddddddd\",\"title\":\"Next\",\"author\":\"Other\",\"lengthSeconds\":61,\"viewCount\":5}]}";
        VideoDetails d = Parser.details(video, "https://inv.example/");
        check("details title", d.video.title, "Watch me");
        check("streams", d.streams.size(), 2);
        check("relative stream url", d.streams.get(0).url, "https://inv.example/latest_version?id=ccccccccccc&itag=18&local=true");
        check("stream height", d.streams.get(1).height, 720);
        check("pick 720", Parser.pickStream(d.streams, 720).quality, "720p");
        check("pick 360", Parser.pickStream(d.streams, 360).quality, "360p");
        check("pick below all", Parser.pickStream(d.streams, 144).quality, "360p");
        check("related", d.related.size(), 1);
        check("likes", Format.count(d.likeCount), "12.3K");
        check("views small", Format.views(999), "999 views");

        try {
            Parser.details("{\"error\":\"This video is unavailable\"}", "https://inv.example");
            check("error throws", "no exception", "exception");
        } catch (Exception e) {
            check("error message", e.getMessage(), "This video is unavailable");
        }

        String instances = "[[\"inv.example\",{\"api\":true,\"type\":\"https\",\"uri\":\"https://inv.example/\"}],"
            + "[\"onion.example\",{\"api\":true,\"type\":\"onion\",\"uri\":\"http://x.onion\"}],"
            + "[\"noapi.example\",{\"api\":false,\"type\":\"https\",\"uri\":\"https://noapi.example\"}],"
            + "[\"null.example\",{\"api\":null,\"type\":\"https\",\"uri\":\"https://null.example\"}]]";
        List<String> list = Parser.instances(instances);
        check("instances", list.size(), 1);
        check("instance uri", list.get(0), "https://inv.example");

        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
