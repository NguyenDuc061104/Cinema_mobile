package com.webcinema.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionStore {
    private static final String NAME = "cinema";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences prefs;

    public SessionStore(Context context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public Session load() {
        return new Session(
                prefs.getString(KEY_TOKEN, null),
                prefs.getInt(KEY_USER_ID, -1),
                prefs.getString(KEY_USERNAME, null)
        );
    }

    public void save(String token, int userId, String username) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public static class Session {
        public final String token;
        public final int userId;
        public final String username;

        Session(String token, int userId, String username) {
            this.token = token;
            this.userId = userId;
            this.username = username;
        }
    }
}
