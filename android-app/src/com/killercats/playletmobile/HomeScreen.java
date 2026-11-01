package com.killercats.playletmobile;

import android.content.Context;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Home: trending videos with topic chips (Trending, Music, Gaming, Movies). */
public class HomeScreen extends LinearLayout {
    private static final String[] LABELS = {"Trending", "Music", "Gaming", "Movies"};
    private static final String[] TYPES = {"", "music", "gaming", "movies"};

    private final Invidious api;
    private final FeedView feed;
    private final List<TextView> chips = new ArrayList<TextView>();
    private int selected = 0;
    private int loadId = 0;

    public HomeScreen(Context context, Invidious api) {
        super(context);
        this.api = api;
        setOrientation(VERTICAL);

        HorizontalScrollView scroller = new HorizontalScrollView(context);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(context);
        int pad = Ui.dp(context, 12);
        row.setPadding(pad, Ui.dp(context, 4), pad, Ui.dp(context, 10));
        for (int i = 0; i < LABELS.length; i++) {
            final int index = i;
            TextView chip = Ui.chip(context, LABELS[i]);
            chip.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    select(index);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(context, 8));
            row.addView(chip, lp);
            chips.add(chip);
        }
        scroller.addView(row);
        addView(scroller);

        feed = new FeedView(context);
        addView(feed, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        select(0);
    }

    public void select(int index) {
        selected = index;
        for (int i = 0; i < chips.size(); i++) Ui.setChipSelected(getContext(), chips.get(i), i == index);
        load();
    }

    /** Tapping Home again reloads. */
    public void reload() {
        load();
    }

    private void load() {
        final int id = ++loadId;
        final String type = TYPES[selected];
        feed.showLoading();
        Async.run(new Async.Task<List<Video>>() {
            @Override
            public List<Video> run() throws Exception {
                return api.trending(type);
            }
        }, new Async.Done<List<Video>>() {
            @Override
            public void ok(List<Video> videos) {
                if (id != loadId) return;
                feed.adapter.set(videos);
                feed.list.setSelection(0);
                if (videos.isEmpty()) feed.showMessage("Nothing here right now.", retry());
                else feed.showList();
            }

            @Override
            public void error(Exception e) {
                if (id != loadId) return;
                feed.adapter.set(new ArrayList<Video>());
                feed.showMessage("Couldn't load videos.\n" + e.getMessage() + "\n\nYou can pick another server in Settings.", retry());
            }
        });
    }

    private Runnable retry() {
        return new Runnable() {
            @Override
            public void run() {
                load();
            }
        };
    }
}
