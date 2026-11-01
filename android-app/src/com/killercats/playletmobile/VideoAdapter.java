package com.killercats.playletmobile;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Big thumbnail cards (title, channel, views, age), like the phone app's feed. */
public class VideoAdapter extends BaseAdapter {
    /** Called when the list nears its end, so the next page can load. */
    public interface MoreListener {
        void onNeedMore();
    }

    private final Context context;
    private final List<Video> videos = new ArrayList<Video>();
    private MoreListener moreListener;

    public VideoAdapter(Context context) {
        this.context = context;
    }

    public void setMoreListener(MoreListener l) {
        moreListener = l;
    }

    public void set(List<Video> list) {
        videos.clear();
        videos.addAll(list);
        notifyDataSetChanged();
    }

    public void add(List<Video> list) {
        videos.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return videos.size();
    }

    @Override
    public Video getItem(int position) {
        return videos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class Holder {
        AspectImageView thumb;
        TextView duration;
        TextView title;
        TextView meta;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(0, 0, 0, Ui.dp(context, 18));

            FrameLayout frame = new FrameLayout(context);
            h = new Holder();
            h.thumb = new AspectImageView(context);
            h.thumb.setBackgroundColor(Ui.SURFACE);
            frame.addView(h.thumb, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            h.duration = Ui.text(context, 12, Ui.TEXT, true);
            h.duration.setPadding(Ui.dp(context, 5), Ui.dp(context, 1), Ui.dp(context, 5), Ui.dp(context, 1));
            GradientDrawable badge = new GradientDrawable();
            badge.setColor(0xCC000000);
            badge.setCornerRadius(Ui.dp(context, 4));
            h.duration.setBackground(badge);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
            lp.setMargins(0, 0, Ui.dp(context, 8), Ui.dp(context, 8));
            frame.addView(h.duration, lp);
            card.addView(frame);

            LinearLayout texts = new LinearLayout(context);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(Ui.dp(context, 14), Ui.dp(context, 10), Ui.dp(context, 14), 0);
            h.title = Ui.text(context, 15, Ui.TEXT, true);
            h.title.setMaxLines(2);
            h.title.setEllipsize(TextUtils.TruncateAt.END);
            h.meta = Ui.text(context, 12.5f, Ui.MUTED, false);
            h.meta.setMaxLines(1);
            h.meta.setEllipsize(TextUtils.TruncateAt.END);
            h.meta.setPadding(0, Ui.dp(context, 3), 0, 0);
            texts.addView(h.title);
            texts.addView(h.meta);
            card.addView(texts);

            card.setTag(h);
            convertView = card;
        } else {
            h = (Holder) convertView.getTag();
        }

        Video v = videos.get(position);
        ImageLoader.load(h.thumb, v.thumbnailUrl());
        String duration = v.live ? "LIVE" : Format.duration(v.lengthSeconds);
        h.duration.setText(duration);
        h.duration.setVisibility(duration.isEmpty() ? View.GONE : View.VISIBLE);
        h.title.setText(v.title);
        h.meta.setText(Format.meta(v));

        if (moreListener != null && position >= videos.size() - 4) moreListener.onNeedMore();
        return convertView;
    }
}
