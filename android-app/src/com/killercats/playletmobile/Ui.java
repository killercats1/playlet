package com.killercats.playletmobile;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/** Colors and small view builders shared by the screens (the UI is built in code). */
public final class Ui {
    public static final int BG = 0xFF0F0F0F;
    public static final int SURFACE = 0xFF212121;
    public static final int SURFACE_2 = 0xFF303030;
    public static final int TEXT = 0xFFF1F1F1;
    public static final int MUTED = 0xFFAAAAAA;
    public static final int ACCENT = 0xFF3EA6FF;

    private Ui() {}

    public static int dp(Context c, float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, c.getResources().getDisplayMetrics());
    }

    public static TextView text(Context c, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    /** A rounded pill (chips, buttons). */
    public static GradientDrawable pill(Context c, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, 18));
        return d;
    }

    public static TextView chip(Context c, String label) {
        TextView t = text(c, 14, TEXT, true);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 14), dp(c, 7), dp(c, 14), dp(c, 7));
        t.setBackground(pill(c, SURFACE_2));
        return t;
    }

    public static void setChipSelected(Context c, TextView chip, boolean selected) {
        chip.setBackground(pill(c, selected ? TEXT : SURFACE_2));
        chip.setTextColor(selected ? BG : TEXT);
    }

    public static TextView button(Context c, String label) {
        TextView t = chip(c, label);
        t.setBackground(pill(c, SURFACE_2));
        return t;
    }
}
