package com.killercats.playletmobile;

import android.content.Context;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Search with endless results (the next page loads near the bottom). */
public class SearchScreen extends LinearLayout {
    private final Invidious api;
    private final EditText input;
    private final FeedView feed;
    private String query = "";
    private int page = 1;
    private boolean loading;
    private boolean noMore;
    private int searchId = 0;

    public SearchScreen(Context context, Invidious api) {
        super(context);
        this.api = api;
        setOrientation(VERTICAL);

        input = new EditText(context);
        input.setHint("Search");
        input.setSingleLine(true);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.MUTED);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setBackground(Ui.pill(context, Ui.SURFACE_2));
        int pad = Ui.dp(context, 14);
        input.setPadding(pad, Ui.dp(context, 10), pad, Ui.dp(context, 10));
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                startSearch(v.getText().toString());
                return true;
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        int m = Ui.dp(context, 12);
        lp.setMargins(m, Ui.dp(context, 4), m, Ui.dp(context, 10));
        addView(input, lp);

        feed = new FeedView(context);
        feed.adapter.setMoreListener(new VideoAdapter.MoreListener() {
            @Override
            public void onNeedMore() {
                loadMore();
            }
        });
        addView(feed, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        feed.showMessage("Search for videos.", null);
    }

    public void focusInput() {
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    private void startSearch(String text) {
        query = text.trim();
        if (query.isEmpty()) return;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        page = 1;
        noMore = false;
        loading = false;
        searchId++;
        feed.adapter.set(new ArrayList<Video>());
        feed.showLoading();
        fetch();
    }

    private void loadMore() {
        if (loading || noMore || query.isEmpty() || feed.adapter.getCount() == 0) return;
        page++;
        fetch();
    }

    private void fetch() {
        loading = true;
        final int id = searchId;
        final String q = query;
        final int p = page;
        Async.run(new Async.Task<List<Video>>() {
            @Override
            public List<Video> run() throws Exception {
                return api.search(q, p);
            }
        }, new Async.Done<List<Video>>() {
            @Override
            public void ok(List<Video> videos) {
                if (id != searchId) return;
                loading = false;
                if (videos.isEmpty()) noMore = true;
                feed.adapter.add(videos);
                if (feed.adapter.getCount() == 0) feed.showMessage("No results for “" + q + "”.", null);
                else feed.showList();
            }

            @Override
            public void error(Exception e) {
                if (id != searchId) return;
                loading = false;
                if (feed.adapter.getCount() == 0) {
                    feed.showMessage("Search failed.\n" + e.getMessage(), new Runnable() {
                        @Override
                        public void run() {
                            startSearch(q);
                        }
                    });
                } else {
                    noMore = true;
                }
            }
        });
    }
}
