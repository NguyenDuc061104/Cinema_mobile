package com.webcinema.mobile.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.webcinema.mobile.model.UserProfile;

import java.util.Locale;

public class ProfileScreen {
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_TEXT = Color.rgb(18, 24, 48);
    private static final int COLOR_MUTED = Color.rgb(98, 105, 125);
    private static final int COLOR_PRIMARY = Color.rgb(118, 44, 168);
    private static final int COLOR_BORDER = Color.rgb(224, 226, 236);
    private static final int COLOR_DANGER = Color.rgb(190, 52, 74);
    private final Context context;

    public ProfileScreen(Context context) {
        this.context = context;
    }

    public LinearLayout render(UserProfile profile, View.OnClickListener history, View.OnClickListener movies, View.OnClickListener logout) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout hero = card();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = text(initial(profile), 28, true, Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(oval(COLOR_PRIMARY));
        row.addView(avatar, new LinearLayout.LayoutParams(dp(72), dp(72)));

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(14), 0, 0, 0);
        info.addView(text(empty(profile.name) ? profile.username : profile.name, 23, true, COLOR_TEXT));
        info.addView(text("@" + profile.username, 14, false, COLOR_MUTED));
        info.addView(text(profile.email, 14, false, COLOR_MUTED));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(row);
        root.addView(hero);

        LinearLayout stats = new LinearLayout(context);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(stat("Orders", String.valueOf(profile.purchaseCount)), weightParams(0, 0, dp(6), 0));
        stats.addView(stat("Spent", money(profile.totalSpent)), weightParams(dp(6), 0, dp(6), 0));
        stats.addView(stat("Member", clean(profile.memberSince)), weightParams(dp(6), 0, 0, 0));
        root.addView(stats);

        LinearLayout details = card();
        details.addView(labelValue("Birthday", clean(profile.birthday)));
        details.addView(labelValue("Last purchase", clean(profile.lastPurchase)));
        root.addView(details);

        Button historyButton = button("Purchase history");
        historyButton.setOnClickListener(history);
        root.addView(historyButton, actionParams(dp(8), dp(10)));

        Button moviesButton = button("Browse movies");
        moviesButton.setOnClickListener(movies);
        root.addView(moviesButton, actionParams(0, dp(10)));

        Button logoutButton = button("Sign out");
        logoutButton.setBackground(tint(COLOR_DANGER, dp(8)));
        logoutButton.setOnClickListener(logout);
        root.addView(logoutButton, actionParams(0, dp(18)));
        return root;
    }

    private LinearLayout stat(String label, String value) {
        LinearLayout view = card();
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(12), dp(8), dp(12));
        TextView number = text(value, 13, true, COLOR_PRIMARY);
        number.setGravity(Gravity.CENTER);
        number.setSingleLine(true);
        number.setIncludeFontPadding(false);
        number.setTextScaleX(0.92f);
        TextView caption = text(label, 12, false, COLOR_MUTED);
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        view.addView(number);
        view.addView(caption);
        return view;
    }
    private LinearLayout labelValue(String label, String value) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(text(label, 13, false, COLOR_MUTED));
        row.addView(text(value, 16, true, COLOR_TEXT));
        return row;
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackground(tint(COLOR_SURFACE, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(10), 0, dp(16));
        view.setLayoutParams(params);
        return view;
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView tv = new TextView(context);
        tv.setText(clean(value));
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setPadding(0, dp(4), 0, dp(4));
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private Button button(String label) {
        Button b = new Button(context);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(tint(COLOR_PRIMARY, dp(8)));
        return b;
    }

    private LinearLayout.LayoutParams actionParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        params.setMargins(0, top, 0, bottom);
        return params;
    }

    private LinearLayout.LayoutParams weightParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private GradientDrawable tint(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (color == COLOR_SURFACE) {
            drawable.setStroke(dp(1), COLOR_BORDER);
        }
        return drawable;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = tint(color, dp(36));
        drawable.setShape(GradientDrawable.OVAL);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private String money(double value) {
        return String.format(Locale.US, "%,.0f VND", value);
    }

    private String initial(UserProfile profile) {
        String source = empty(profile.name) ? profile.username : profile.name;
        return empty(source) ? "U" : source.substring(0, 1).toUpperCase(Locale.US);
    }

    private String clean(String value) {
        return empty(value) || "null".equals(value) ? "-" : value;
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
