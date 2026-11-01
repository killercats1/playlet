package com.killercats.playletmobile;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/** A list of video cards with loading and error states; tapping a card opens the player. */
public class FeedView extends FrameLayout {
    public final ListView list;
    public final VideoAdapter adapter;
    private final ProgressBar spinner;
    private final TextView message;
    private Runnable retry;

    public FeedView(final Context context) {
        super(context);
        adapter = new VideoAdapter(context);
        list = new ListView(context);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item instanceof Video) PlayerActivity.open(context, (Video) item);
            }
        });
        addView(list, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        spinner = new ProgressBar(context);
        addView(spinner, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        message = Ui.text(context, 15, Ui.MUTED, false);
        message.setGravity(Gravity.CENTER);
        int pad = Ui.dp(context, 32);
        message.setPadding(pad, pad, pad, pad);
        message.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (retry != null) retry.run();
            }
        });
        addView(message, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        showMessage("", null);
    }

    public void showLoading() {
        spinner.setVisibility(VISIBLE);
        message.setVisibility(GONE);
    }

    public void showList() {
        spinner.setVisibility(GONE);
        message.setVisibility(GONE);
    }

    /** A message in place of the list; with a retry action, tapping it retries. */
    public void showMessage(String text, Runnable retryAction) {
        spinner.setVisibility(GONE);
        retry = retryAction;
        message.setText(retryAction == null ? text : text + "\n\nTap to try again");
        message.setVisibility(text.isEmpty() ? GONE : VISIBLE);
    }

}
