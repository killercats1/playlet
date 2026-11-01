package com.killercats.playletmobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.List;

/**
 * Watch screen: the video on top (full screen when the phone is turned sideways), then title, channel,
 * stats and description, then related videos. When a video ends, the first related one plays.
 */
public class PlayerActivity extends Activity {
    private static final String EXTRA_ID = "videoId";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_AUTHOR = "author";

    private Invidious api;
    private LinearLayout root;
    private FrameLayout videoBox;
    private VideoView videoView;
    private ProgressBar spinner;
    private TextView error;
    private ListView list;
    private VideoAdapter related;
    private TextView titleView;
    private TextView statsView;
    private TextView channelView;
    private TextView descriptionView;

    private VideoDetails details;
    private final List<VideoDetails.Stream> tried = new ArrayList<VideoDetails.Stream>();
    private int loadId = 0;
    private int resumeAt = 0;

    public static void open(Context context, Video video) {
        Intent i = new Intent(context, PlayerActivity.class);
        i.putExtra(EXTRA_ID, video.videoId);
        i.putExtra(EXTRA_TITLE, video.title);
        i.putExtra(EXTRA_AUTHOR, video.author);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF000000);
        getWindow().setNavigationBarColor(Ui.BG);
        api = new Invidious(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        videoBox = new FrameLayout(this) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                if (isLandscape()) {
                    super.onMeasure(widthSpec, heightSpec);
                    return;
                }
                int width = MeasureSpec.getSize(widthSpec);
                int height = width * 9 / 16;
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
        };
        videoBox.setBackgroundColor(0xFF000000);
        videoView = new VideoView(this);
        videoBox.addView(videoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        spinner = new ProgressBar(this);
        videoBox.addView(spinner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        error = Ui.text(this, 14, Ui.TEXT, false);
        error.setGravity(Gravity.CENTER);
        int pad = Ui.dp(this, 24);
        error.setPadding(pad, pad, pad, pad);
        error.setVisibility(View.GONE);
        videoBox.addView(error, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        root.addView(videoBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoBox);
        videoView.setMediaController(controller);
        videoView.setKeepScreenOn(true);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                spinner.setVisibility(View.GONE);
                if (resumeAt > 0) {
                    videoView.seekTo(resumeAt);
                    resumeAt = 0;
                }
                videoView.start();
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                playNextStream();
                return true;
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (related.getCount() > 0) load(related.getItem(0));
            }
        });

        list = new ListView(this);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        list.addHeaderView(buildHeader(), null, false);
        related = new VideoAdapter(this);
        list.setAdapter(related);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item instanceof Video) load((Video) item);
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
        applyOrientation();

        Video v = new Video();
        v.videoId = getIntent().getStringExtra(EXTRA_ID);
        v.title = valueOrEmpty(getIntent().getStringExtra(EXTRA_TITLE));
        v.author = valueOrEmpty(getIntent().getStringExtra(EXTRA_AUTHOR));
        if (v.videoId == null) {
            finish();
            return;
        }
        load(v);
    }

    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 14);
        h.setPadding(pad, pad, pad, Ui.dp(this, 6));

        titleView = Ui.text(this, 18, Ui.TEXT, true);
        h.addView(titleView);
        statsView = Ui.text(this, 13, Ui.MUTED, false);
        statsView.setPadding(0, Ui.dp(this, 4), 0, 0);
        h.addView(statsView);
        channelView = Ui.text(this, 15, Ui.TEXT, true);
        channelView.setPadding(0, Ui.dp(this, 14), 0, 0);
        h.addView(channelView);

        descriptionView = Ui.text(this, 14, Ui.TEXT, false);
        descriptionView.setBackground(roundedSurface());
        descriptionView.setPadding(pad, Ui.dp(this, 10), pad, Ui.dp(this, 10));
        descriptionView.setMaxLines(3);
        descriptionView.setEllipsize(TextUtils.TruncateAt.END);
        descriptionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean collapsed = descriptionView.getMaxLines() == 3;
                descriptionView.setMaxLines(collapsed ? Integer.MAX_VALUE : 3);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 12);
        h.addView(descriptionView, lp);

        TextView upNext = Ui.text(this, 16, Ui.TEXT, true);
        upNext.setText("Up next");
        upNext.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 8));
        h.addView(upNext);
        return h;
    }

    private android.graphics.drawable.GradientDrawable roundedSurface() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(Ui.SURFACE);
        d.setCornerRadius(Ui.dp(this, 12));
        return d;
    }

    /** Loads a video (also from Up next) into this screen. */
    private void load(final Video video) {
        final int id = ++loadId;
        details = null;
        tried.clear();
        videoView.stopPlayback();
        spinner.setVisibility(View.VISIBLE);
        error.setVisibility(View.GONE);
        titleView.setText(video.title);
        statsView.setText("");
        channelView.setText(video.author);
        descriptionView.setText("");
        descriptionView.setVisibility(View.GONE);
        related.set(new ArrayList<Video>());
        list.setSelection(0);

        Async.run(new Async.Task<VideoDetails>() {
            @Override
            public VideoDetails run() throws Exception {
                return api.video(video.videoId);
            }
        }, new Async.Done<VideoDetails>() {
            @Override
            public void ok(VideoDetails d) {
                if (id != loadId) return;
                details = d;
                showDetails(d);
                playNextStream();
            }

            @Override
            public void error(Exception e) {
                if (id != loadId) return;
                showError("Couldn't load this video.\n" + e.getMessage() + "\n\nTry another server in Settings.");
            }
        });
    }

    private void showDetails(VideoDetails d) {
        Video v = d.video;
        titleView.setText(v.title);
        StringBuilder stats = new StringBuilder(Format.views(v.viewCount));
        if (!v.publishedText.isEmpty()) stats.append(stats.length() > 0 ? " · " : "").append(v.publishedText);
        if (d.likeCount > 0) stats.append(stats.length() > 0 ? " · " : "").append(Format.count(d.likeCount)).append(" likes");
        statsView.setText(stats);
        channelView.setText(d.subCountText.isEmpty() ? v.author : v.author + "  ·  " + d.subCountText + " subscribers");
        descriptionView.setText(d.description.trim());
        descriptionView.setVisibility(d.description.trim().isEmpty() ? View.GONE : View.VISIBLE);
        related.set(d.related);
    }

    /** Plays the best stream not tried yet (within the quality setting), falling back to the others. */
    private void playNextStream() {
        if (details == null) return;
        List<VideoDetails.Stream> left = new ArrayList<VideoDetails.Stream>();
        for (VideoDetails.Stream s : details.streams) if (!tried.contains(s)) left.add(s);
        if (left.isEmpty()) {
            showError(details.streams.isEmpty()
                ? "This server didn't return a playable stream for this video.\nTry another server in Settings."
                : "Couldn't play this video.\nTry another server in Settings.");
            return;
        }
        VideoDetails.Stream s = Parser.pickStream(left, api.maxQuality());
        tried.add(s);
        spinner.setVisibility(View.VISIBLE);
        error.setVisibility(View.GONE);
        videoView.setVideoURI(Uri.parse(s.url));
    }

    private void showError(String text) {
        spinner.setVisibility(View.GONE);
        error.setText(text);
        error.setVisibility(View.VISIBLE);
    }

    // ---------- full screen when sideways ----------

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyOrientation();
    }

    private void applyOrientation() {
        boolean landscape = isLandscape();
        list.setVisibility(landscape ? View.GONE : View.VISIBLE);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) videoBox.getLayoutParams();
        lp.height = landscape ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.weight = landscape ? 1 : 0;
        videoBox.setLayoutParams(lp);
        View decor = getWindow().getDecorView();
        if (landscape) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    // ---------- lifecycle ----------

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            resumeAt = videoView.getCurrentPosition();
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resumeAt > 0 && details != null) {
            videoView.seekTo(resumeAt);
            videoView.start();
            resumeAt = 0;
        }
    }

    @Override
    protected void onDestroy() {
        loadId++;
        videoView.stopPlayback();
        super.onDestroy();
    }

    private static String valueOrEmpty(String s) {
        return s == null ? "" : s;
    }
}
