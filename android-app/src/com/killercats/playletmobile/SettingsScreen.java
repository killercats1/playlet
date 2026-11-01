package com.killercats.playletmobile;

import android.content.Context;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

/** Settings: which Invidious server to use, automatic fallback, and the highest video quality. */
public class SettingsScreen extends ScrollView {
    private final Invidious api;
    private final LinearLayout body;
    private final RadioGroup servers;
    private final TextView status;

    public SettingsScreen(Context context, Invidious api) {
        super(context);
        this.api = api;
        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(context, 16);
        body.setPadding(pad, Ui.dp(context, 4), pad, pad * 2);
        addView(body);

        heading("Video server");
        note("Videos and lists come from Invidious, an open-source YouTube front end, so there are no ads and no "
            + "Google account. Public servers sometimes go down; with Automatic on, the app moves to another one.");

        Switch auto = new Switch(context);
        auto.setText("Automatic (switch servers when one fails)");
        auto.setTextColor(Ui.TEXT);
        auto.setChecked(api.isAuto());
        auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                SettingsScreen.this.api.choose(SettingsScreen.this.api.currentInstance(), checked);
            }
        });
        body.addView(auto);

        servers = new RadioGroup(context);
        servers.setPadding(0, Ui.dp(context, 8), 0, Ui.dp(context, 4));
        body.addView(servers);

        status = Ui.text(context, 13, Ui.MUTED, false);
        body.addView(status);

        TextView refresh = Ui.button(context, "Refresh server list");
        refresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshList();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(context, 10);
        body.addView(refresh, lp);

        note("Or enter a server address:");
        final EditText custom = new EditText(context);
        custom.setHint("https://invidious.example.com");
        custom.setSingleLine(true);
        custom.setTextColor(Ui.TEXT);
        custom.setHintTextColor(Ui.MUTED);
        body.addView(custom);
        TextView use = Ui.button(context, "Use this server");
        use.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = custom.getText().toString().trim();
                if (!url.startsWith("https://") && !url.startsWith("http://")) {
                    status.setText("Enter a full address starting with https://");
                    return;
                }
                SettingsScreen.this.api.choose(url, SettingsScreen.this.api.isAuto());
                status.setText("Using " + url);
                fillServers(SettingsScreen.this.api.knownInstances());
            }
        });
        body.addView(use, new LinearLayout.LayoutParams(lp));

        heading("Video quality");
        note("Highest quality to play. Higher qualities aren't available on every server.");
        RadioGroup quality = new RadioGroup(context);
        final int[] heights = {360, 720};
        for (final int h : heights) {
            RadioButton rb = new RadioButton(context);
            rb.setText(h + "p");
            rb.setTextColor(Ui.TEXT);
            rb.setChecked(api.maxQuality() == h);
            rb.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsScreen.this.api.setMaxQuality(h);
                }
            });
            quality.addView(rb);
        }
        body.addView(quality);

        heading("About");
        note("Playlet Mobile 1.0.0 · no ads, no tracking, no account.");

        fillServers(api.knownInstances());
    }

    private void heading(String text) {
        TextView t = Ui.text(getContext(), 17, Ui.TEXT, true);
        t.setText(text);
        t.setPadding(0, Ui.dp(getContext(), 18), 0, Ui.dp(getContext(), 4));
        body.addView(t);
    }

    private void note(String text) {
        TextView t = Ui.text(getContext(), 13, Ui.MUTED, false);
        t.setText(text);
        t.setPadding(0, Ui.dp(getContext(), 4), 0, Ui.dp(getContext(), 6));
        body.addView(t);
    }

    private void fillServers(List<String> list) {
        servers.removeAllViews();
        String current = api.currentInstance();
        for (final String s : list) {
            RadioButton rb = new RadioButton(getContext());
            rb.setText(s.replace("https://", ""));
            rb.setTextColor(Ui.TEXT);
            rb.setId(View.generateViewId());
            servers.addView(rb);
            if (s.equals(current)) rb.setChecked(true);
            rb.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    api.choose(s, api.isAuto());
                    status.setText("Using " + s);
                }
            });
        }
    }

    private void refreshList() {
        status.setText("Refreshing…");
        Async.run(new Async.Task<List<String>>() {
            @Override
            public List<String> run() throws Exception {
                return api.refreshInstances();
            }
        }, new Async.Done<List<String>>() {
            @Override
            public void ok(List<String> list) {
                fillServers(list);
                status.setText(list.size() + " servers");
            }

            @Override
            public void error(Exception e) {
                status.setText("Couldn't refresh the list: " + e.getMessage());
            }
        });
    }
}
