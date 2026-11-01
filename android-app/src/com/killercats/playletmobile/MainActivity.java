package com.killercats.playletmobile;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The app's main screen: a header, the current tab, and the bottom tab bar (Home, Search, Settings). */
public class MainActivity extends Activity {
    private FrameLayout content;
    private HomeScreen home;
    private SearchScreen search;
    private SettingsScreen settings;
    private final TextView[] tabs = new TextView[3];
    private int current = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Ui.BG);
        w.setNavigationBarColor(Ui.BG);

        Invidious api = new Invidious(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        TextView title = Ui.text(this, 21, Ui.TEXT, true);
        title.setText("Playlet");
        title.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 10));
        root.addView(title);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        View line = new View(this);
        line.setBackgroundColor(Ui.SURFACE);
        root.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"Home", "Search", "Settings"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView tab = Ui.text(this, 13, Ui.MUTED, true);
            tab.setText(labels[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    show(index);
                }
            });
            bar.addView(tab, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tabs[i] = tab;
        }
        root.addView(bar);

        home = new HomeScreen(this, api);
        search = new SearchScreen(this, api);
        settings = new SettingsScreen(this, api);
        content.addView(home);
        content.addView(search);
        content.addView(settings);

        setContentView(root);
        show(0);
    }

    private void show(int index) {
        if (index == current && index == 0) home.reload();
        if (index == current && index == 1) search.focusInput();
        current = index;
        home.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        search.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        settings.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < tabs.length; i++) tabs[i].setTextColor(i == index ? Ui.TEXT : Ui.MUTED);
    }

    @Override
    public void onBackPressed() {
        if (current != 0) show(0);
        else super.onBackPressed();
    }
}
