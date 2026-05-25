package com.webcinema.mobile;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.webcinema.mobile.config.AppConfig;
import com.webcinema.mobile.data.SessionStore;
import com.webcinema.mobile.model.UserProfile;
import com.webcinema.mobile.ui.ProfileScreen;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private SessionStore sessionStore;

    private LinearLayout content;
    private ScrollView currentScroll;
    private final ArrayList<Movie> movies = new ArrayList<>();
    private final ArrayList<Showtime> showtimes = new ArrayList<>();
    private final ArrayList<TicketType> ticketTypes = new ArrayList<>();
    private final ArrayList<Seat> seats = new ArrayList<>();
    private final ArrayList<Combo> combos = new ArrayList<>();
    private final ArrayList<Order> orders = new ArrayList<>();
    private final Set<String> selectedSeatIds = new HashSet<>();
    private final Map<String, TextView> ticketQuantityViews = new HashMap<>();
    private final Map<String, TextView> comboQuantityViews = new HashMap<>();
    private final Map<String, Button> seatButtons = new HashMap<>();
    private TextView seatSummaryView;
    private TextView totalSummaryView;
    private LinearLayout bookingContainer;
    private final Map<String, Button> showtimeButtons = new HashMap<>();
    private int bookingLoadsPending = 0;
    private Showtime selectedShowtime;
    private Movie selectedMovie;
    private JSONObject selectedCinema;
    private String token;
    private int userId = -1;
    private String username;

    private static final int COLOR_BG = Color.rgb(246, 247, 251);
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_SURFACE_SOFT = Color.rgb(239, 241, 250);
    private static final int COLOR_TEXT = Color.rgb(18, 24, 48);
    private static final int COLOR_MUTED = Color.rgb(98, 105, 125);
    private static final int COLOR_PRIMARY = Color.rgb(118, 44, 168);
    private static final int COLOR_PRIMARY_SOFT = Color.rgb(237, 229, 247);
    private static final int COLOR_ACCENT = Color.rgb(255, 199, 44);
    private static final int COLOR_ERROR = Color.rgb(196, 44, 58);
    private static final int COLOR_POSTER_BG = Color.rgb(232, 234, 244);
    private static final int COLOR_BORDER = Color.rgb(224, 226, 236);
    private static final int TAB_HOME = 0;
    private static final int TAB_BUY = 1;
    private static final int TAB_NEWS = 2;
    private static final int TAB_MEMBER = 3;
    private int activeTab = TAB_HOME;
    private int featuredIndex = 0;
    private final Handler featuredHandler = new Handler(Looper.getMainLooper());
    private Runnable featuredAutoAdvance;
    private boolean featuredAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionStore = new SessionStore(this);
        loadSession();
        showHome();
    }

    private void loadSession() {
        SessionStore.Session session = sessionStore.load();
        token = session.token;
        userId = session.userId;
        username = session.username;
    }

    private void saveSession(String accessToken, int id, String name) {
        token = accessToken;
        userId = id;
        username = name;
        sessionStore.save(token, userId, username);
    }

    private void logout() {
        token = null;
        userId = -1;
        username = null;
        sessionStore.clear();
        showHome();
    }

    private void showHome() {
        activeTab = TAB_HOME;
        if (movies.isEmpty()) {
            page("Cinema", false);
            addLoading("Loading home...");
            loadHomeMovies();
        } else {
            renderHome();
        }
    }

    private void loadHomeMovies() {
        get("/movie/movies", new ApiCallback() {
            @Override
            public void ok(String body) {
                movies.clear();
                try {
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) movies.add(new Movie(arr.getJSONObject(i)));
                    renderHome();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void renderHome() {
        activeTab = TAB_HOME;
        page("Cinema", false);

        LinearLayout hero = card();
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(getResources().getIdentifier("star", "drawable", getPackageName()));
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(210), dp(72));
        logoParams.setMargins(0, 0, 0, dp(10));
        hero.addView(logo, logoParams);
        TextView tagline = text("Be happy. Be a star.", 18, true, COLOR_PRIMARY);
        tagline.setGravity(Gravity.CENTER);
        hero.addView(tagline);
        content.addView(hero);

        LinearLayout promo = card();
        promo.setBackground(tint(COLOR_PRIMARY, dp(8)));
        TextView happy = text("HAPPY HOUR", 24, true, Color.WHITE);
        happy.setGravity(Gravity.CENTER);
        promo.addView(happy);
        TextView price = text("Tickets from 45K", 34, true, COLOR_ACCENT);
        price.setGravity(Gravity.CENTER);
        promo.addView(price);
        TextView window = text("Before 10:00 and after 22:00", 14, true, Color.WHITE);
        window.setGravity(Gravity.CENTER);
        promo.addView(window);
        content.addView(promo);

        addText("Featured Movie", 22, true, COLOR_TEXT);
        if (movies.isEmpty()) {
            addText("No movies found.", 16, false, COLOR_MUTED);
            return;
        }

        if (featuredIndex < 0 || featuredIndex >= movies.size()) featuredIndex = 0;
        LinearLayout featured = card();
        featured.setPadding(dp(10), dp(10), dp(10), dp(10));

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
        poster.setAdjustViewBounds(true);
        poster.setBackgroundColor(COLOR_POSTER_BG);
        featured.addView(poster, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(430)));

        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dotsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32));
        dotsParams.setMargins(0, dp(8), 0, 0);
        featured.addView(dots, dotsParams);

        TextView[] dotViews = new TextView[4];
        for (int i = 0; i < dotViews.length; i++) {
            final int dotIndex = i;
            TextView dot = text("", 1, false, COLOR_SURFACE);
            dot.setBackground(tint(COLOR_BORDER, dp(5)));
            dot.setOnClickListener(v -> {
                int direction = dotIndex >= (featuredIndex % dotViews.length) ? 1 : -1;
                featuredIndex = dotIndex;
                showHomeFeatured(poster, dotViews, direction, true);
                scheduleFeaturedAutoAdvance(poster, dotViews);
            });
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
            dotParams.setMargins(dp(5), 0, dp(5), 0);
            dots.addView(dot, dotParams);
            dotViews[i] = dot;
        }

        attachHomePosterSwipe(poster, dotViews);
        showHomeFeatured(poster, dotViews, 0, false);
        scheduleFeaturedAutoAdvance(poster, dotViews);
        content.addView(featured);
    }

    private void showHomeFeatured(ImageView poster, TextView[] dotViews, int direction, boolean animated) {
        if (movies.isEmpty() || featuredAnimating) return;
        if (featuredIndex < 0 || featuredIndex >= movies.size()) featuredIndex = 0;
        Movie current = movies.get(featuredIndex);
        updateHomeDots(dotViews);
        if (!animated) {
            poster.setTranslationX(0f);
            poster.setAlpha(1f);
            loadImage(current.posterUrl, poster);
            return;
        }
        featuredAnimating = true;
        float exitX = direction >= 0 ? -dp(120) : dp(120);
        float enterX = direction >= 0 ? dp(120) : -dp(120);
        poster.animate().translationX(exitX).alpha(0f).setDuration(260).withEndAction(() -> {
            poster.setTranslationX(enterX);
            poster.setAlpha(0f);
            loadImage(current.posterUrl, poster, () -> poster.animate().translationX(0f).alpha(1f).setDuration(360).withEndAction(() -> featuredAnimating = false).start());
        }).start();
    }

    private void updateHomeDots(TextView[] dotViews) {
        int activeDot = featuredIndex % dotViews.length;
        for (int i = 0; i < dotViews.length; i++) {
            boolean selected = i == activeDot;
            dotViews[i].setBackground(tint(selected ? COLOR_PRIMARY : COLOR_BORDER, dp(selected ? 6 : 5)));
            ViewGroup.LayoutParams params = dotViews[i].getLayoutParams();
            params.width = dp(selected ? 12 : 9);
            params.height = dp(selected ? 12 : 9);
            dotViews[i].setLayoutParams(params);
        }
    }

    private void attachHomePosterSwipe(ImageView poster, TextView[] dotViews) {
        final float[] startX = new float[1];
        final float[] startY = new float[1];
        poster.setOnTouchListener((v, event) -> {
            if (movies.isEmpty() || featuredAnimating) return true;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    featuredHandler.removeCallbacksAndMessages(null);
                    startX[0] = event.getX();
                    startY[0] = event.getY();
                    v.animate().cancel();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - startX[0];
                    if (Math.abs(dx) > Math.abs(event.getY() - startY[0])) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        v.setTranslationX(dx * 0.85f);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float releaseDx = event.getX() - startX[0];
                    float releaseDy = event.getY() - startY[0];
                    if (Math.abs(releaseDx) < dp(10) && Math.abs(releaseDy) < dp(10)) {
                        showMovieDetail(movies.get(featuredIndex).id);
                        return true;
                    }
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    if (Math.abs(releaseDx) > dp(36) && Math.abs(releaseDx) > Math.abs(releaseDy)) {
                        int direction = releaseDx < 0 ? 1 : -1;
                        featuredIndex = (featuredIndex + direction + movies.size()) % movies.size();
                        showHomeFeatured(poster, dotViews, direction, true);
                    } else {
                        v.animate().translationX(0f).alpha(1f).setDuration(220).start();
                    }
                    scheduleFeaturedAutoAdvance(poster, dotViews);
                    return true;
            }
            return true;
        });
    }

    private void scheduleFeaturedAutoAdvance(ImageView poster, TextView[] dotViews) {
        featuredHandler.removeCallbacksAndMessages(null);
        featuredAutoAdvance = () -> {
            if (activeTab != TAB_HOME || movies.isEmpty()) return;
            featuredIndex = (featuredIndex + 1) % movies.size();
            showHomeFeatured(poster, dotViews, 1, true);
            featuredHandler.postDelayed(featuredAutoAdvance, 5500);
        };
        featuredHandler.postDelayed(featuredAutoAdvance, 5500);
    }
    private void showMovies() {
        activeTab = TAB_BUY;
        if (movies.isEmpty()) {
            page("Buy Tickets", false);
            loadMovies();
        } else {
            renderMovies();
        }
    }

    private void loadMovies() {
        activeTab = TAB_BUY;
        addLoading("Loading movies...");
        get("/movie/movies", new ApiCallback() {
            @Override
            public void ok(String body) {
                movies.clear();
                try {
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        movies.add(new Movie(arr.getJSONObject(i)));
                    }
                    renderMovies();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }
    private void renderMovies() {
        activeTab = TAB_BUY;
        page("Buy Tickets", false);
        addText("Now Showing", 26, true, COLOR_TEXT);
        addText("Choose a movie, showtime, tickets and seats.", 14, false, COLOR_MUTED);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = button("Refresh");
        refresh.setOnClickListener(v -> loadMovies());
        actions.addView(refresh, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        content.addView(actions);
        if (movies.isEmpty()) {
            addText("No movies found.", 16, false, COLOR_MUTED);
            return;
        }

        for (Movie movie : movies) {
            LinearLayout card = card();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageView poster = new ImageView(this);
            LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(92), dp(138));
            poster.setLayoutParams(posterParams);
            poster.setBackgroundColor(COLOR_POSTER_BG);
            poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
            row.addView(poster);
            loadImage(movie.posterUrl, poster);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12), 0, 0, 0);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            info.addView(text(movie.title, 19, true, COLOR_TEXT));
            info.addView(text(movie.genre + " | " + movie.country, 13, false, COLOR_MUTED));
            info.addView(text(movie.duration + " min | " + movie.ageRating, 13, false, COLOR_MUTED));
            Button detail = button("Detail / book");
            detail.setOnClickListener(v -> showMovieDetail(movie.id));
            info.addView(detail);
            row.addView(info);

            card.addView(row);
            content.addView(card);
        }
    }

    private void showMovieDetail(int movieId) {
        activeTab = TAB_BUY;
        page("Movie detail", true);
        addLoading("Loading detail...");
        selectedMovie = null;
        selectedShowtime = null;
        selectedCinema = null;
        selectedSeatIds.clear();
        ticketTypes.clear();
        seats.clear();
        combos.clear();

        get("/movie/movies/" + movieId, new ApiCallback() {
            @Override
            public void ok(String body) {
                try {
                    selectedMovie = new Movie(new JSONObject(body));
                    loadShowtimes(movieId);
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void loadShowtimes(int movieId) {
        get("/showtime/movies/" + movieId + "/showtimes", new ApiCallback() {
            @Override
            public void ok(String body) {
                try {
                    showtimes.clear();
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        showtimes.add(new Showtime(arr.getJSONObject(i)));
                    }
                    renderDetail();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void renderDetail() {
        renderDetail(-1);
    }

    private void renderDetail(int restoreScrollY) {
        activeTab = TAB_BUY;
        page("Movie detail", true, restoreScrollY);
        if (selectedMovie == null) return;

        LinearLayout header = card();
        ImageView poster = new ImageView(this);
        poster.setBackgroundColor(COLOR_POSTER_BG);
        poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260));
        posterParams.setMargins(0, 0, 0, dp(10));
        header.addView(poster, posterParams);
        loadImage(selectedMovie.posterUrl, poster);
        TextView title = text(selectedMovie.title, 24, true, COLOR_TEXT);
        header.addView(title);
        header.addView(text(selectedMovie.genre + " | " + selectedMovie.country + " | " + selectedMovie.duration + " min", 14, false, COLOR_MUTED));
        header.addView(text("Language: " + selectedMovie.language + " | Age: " + selectedMovie.ageRating, 14, false, COLOR_MUTED));
        header.addView(text(selectedMovie.description, 14, false, COLOR_TEXT));
        content.addView(header);

        showtimeButtons.clear();
        addText("Showtimes", 22, true, COLOR_TEXT);
        if (showtimes.isEmpty()) {
            addText("There are currently no showtimes.", 15, false, COLOR_MUTED);
            return;
        }
        for (Showtime s : showtimes) {
            Button b = button(s.startTime + "  " + s.room);
            if (selectedShowtime != null && selectedShowtime.id.equals(s.id)) {
                b.setTextColor(COLOR_TEXT);
                b.setBackground(tint(COLOR_ACCENT, dp(8)));
            }
            b.setOnClickListener(v -> selectShowtime(s));
            showtimeButtons.put(s.id, b);
            LinearLayout.LayoutParams showtimeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
            showtimeParams.setMargins(0, dp(6), 0, dp(8));
            content.addView(b, showtimeParams);
        }

        bookingContainer = new LinearLayout(this);
        bookingContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(bookingContainer);
        if (selectedShowtime != null) {
            renderBookingSections();
        }
    }

    private void selectShowtime(Showtime showtime) {
        selectedShowtime = showtime;
        selectedCinema = null;
        selectedSeatIds.clear();
        ticketTypes.clear();
        seats.clear();
        combos.clear();
        updateShowtimeButtons();
        bookingLoadsPending = 4;
        renderBookingSections();
        loadCinema();
        loadTicketTypes();
        loadSeats();
        loadCombos();
    }

    private void updateShowtimeButtons() {
        for (Showtime s : showtimes) {
            Button button = showtimeButtons.get(s.id);
            if (button == null) continue;
            boolean selected = selectedShowtime != null && selectedShowtime.id.equals(s.id);
            button.setTextColor(selected ? COLOR_TEXT : Color.WHITE);
            button.setBackground(tint(selected ? COLOR_ACCENT : COLOR_PRIMARY, dp(8)));
        }
    }

    private void finishBookingLoad() {
        bookingLoadsPending = Math.max(0, bookingLoadsPending - 1);
        if (bookingLoadsPending == 0) {
            renderBookingSections();
        }
    }

    private void loadCinema() {
        get("/showtime/" + selectedShowtime.id + "/cinema", new ApiCallback() {
            @Override
            public void ok(String body) {
                try {
                    selectedCinema = new JSONObject(body);
                    finishBookingLoad();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void loadTicketTypes() {
        get("/ticket/" + selectedShowtime.id + "/ticket-types", new ApiCallback() {
            @Override
            public void ok(String body) {
                try {
                    ticketTypes.clear();
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        ticketTypes.add(new TicketType(arr.getJSONObject(i)));
                    }
                    finishBookingLoad();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void loadSeats() {
        get("/seat/" + selectedShowtime.id + "/seats", new ApiCallback() {
            @Override
            public void ok(String body) {
                try {
                    seats.clear();
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        seats.add(new Seat(arr.getJSONObject(i)));
                    }
                    finishBookingLoad();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void loadCombos() {
        get("/combo/api/", new ApiCallback() {
            @Override
            public void ok(String body) {
                try {
                    combos.clear();
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        combos.add(new Combo(arr.getJSONObject(i)));
                    }
                    finishBookingLoad();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void renderBookingSections() {
        LinearLayout target = bookingContainer == null ? content : bookingContainer;
        target.removeAllViews();
        ticketQuantityViews.clear();
        comboQuantityViews.clear();
        seatButtons.clear();
        seatSummaryView = null;
        totalSummaryView = null;

        if (bookingLoadsPending > 0) {
            LinearLayout loading = card();
            loading.addView(text("Loading booking options...", 16, false, COLOR_MUTED));
            target.addView(loading);
            return;
        }

        if (selectedCinema != null) {
            LinearLayout cinemaCard = card();
            cinemaCard.addView(text("Cinema", 20, true, COLOR_TEXT));
            cinemaCard.addView(text(selectedCinema.optString("name"), 16, true, COLOR_PRIMARY));
            cinemaCard.addView(text(selectedCinema.optString("address"), 14, false, COLOR_TEXT));
            cinemaCard.addView(text(selectedCinema.optString("phone"), 14, false, COLOR_MUTED));
            target.addView(cinemaCard);
        }

        LinearLayout ticketCard = card();
        ticketCard.addView(text("Tickets", 22, true, COLOR_TEXT));
        for (TicketType type : ticketTypes) {
            TextView[] countRef = new TextView[1];
            LinearLayout row = qtyRow(type.name + " - " + money(type.price), type.quantity,
                    () -> {
                        if (type.quantity > 0) type.quantity--;
                        trimSeatsToTicketCount();
                        updateBookingUi();
                    },
                    () -> {
                        type.quantity++;
                        updateBookingUi();
                    }, countRef);
            ticketQuantityViews.put(type.id, countRef[0]);
            ticketCard.addView(row);
        }
        target.addView(ticketCard);

        LinearLayout seatCard = card();
        seatCard.addView(text("Seats", 22, true, COLOR_TEXT));
        seatSummaryView = text(selectedSeatIds.size() + "/" + totalTicketCount() + " selected", 14, false, COLOR_MUTED);
        seatCard.addView(seatSummaryView);

        TextView screen = text("SCREEN", 12, true, COLOR_TEXT);
        screen.setGravity(Gravity.CENTER);
        screen.setBackground(tint(COLOR_ACCENT, dp(4)));
        LinearLayout.LayoutParams screenParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(26));
        screenParams.setMargins(0, dp(12), 0, dp(14));
        seatCard.addView(screen, screenParams);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(false);
        grid.setPadding(0, dp(2), 0, dp(8));
        for (Seat seat : seats) {
            Button b = button(seat.number);
            b.setTextSize(11);
            b.setMinHeight(0);
            b.setMinWidth(0);
            b.setMinimumHeight(0);
            b.setMinimumWidth(0);
            b.setPadding(0, 0, 0, 0);
            b.setOnClickListener(v -> toggleSeat(seat));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(62);
            params.height = dp(38);
            params.setMargins(dp(3), dp(4), dp(3), dp(4));
            grid.addView(b, params);
            seatButtons.put(seat.id, b);
        }
        seatCard.addView(grid);
        target.addView(seatCard);

        LinearLayout comboCard = card();
        comboCard.addView(text("Combos", 22, true, COLOR_TEXT));
        for (Combo combo : combos) {
            TextView[] countRef = new TextView[1];
            LinearLayout row = qtyRow(combo.name + " - " + money(combo.price), combo.quantity,
                    () -> {
                        if (combo.quantity > 0) combo.quantity--;
                        updateBookingUi();
                    },
                    () -> {
                        if (totalComboCount() < totalTicketCount()) combo.quantity++;
                        updateBookingUi();
                    }, countRef);
            comboQuantityViews.put(combo.id, countRef[0]);
            comboCard.addView(row);
        }
        target.addView(comboCard);

        LinearLayout checkout = card();
        totalSummaryView = text("Total: " + money(totalPrice()), 22, true, COLOR_PRIMARY);
        checkout.addView(totalSummaryView);
        Button pay = button("Checkout");
        pay.setOnClickListener(v -> showCheckout());
        LinearLayout.LayoutParams payParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        payParams.setMargins(0, dp(12), 0, 0);
        checkout.addView(pay, payParams);
        target.addView(checkout);
        updateBookingUi();
    }

    private void updateBookingUi() {
        for (TicketType type : ticketTypes) {
            TextView count = ticketQuantityViews.get(type.id);
            if (count != null) count.setText(String.valueOf(type.quantity));
        }
        for (Combo combo : combos) {
            TextView count = comboQuantityViews.get(combo.id);
            if (count != null) count.setText(String.valueOf(combo.quantity));
        }
        if (seatSummaryView != null) {
            seatSummaryView.setText(selectedSeatIds.size() + "/" + totalTicketCount() + " selected");
        }
        for (Seat seat : seats) {
            Button button = seatButtons.get(seat.id);
            if (button == null) continue;
            boolean selected = selectedSeatIds.contains(seat.id);
            boolean booked = "booked".equalsIgnoreCase(seat.type);
            button.setEnabled(!booked);
            button.setBackground(tint(selected ? COLOR_ACCENT : seatColor(seat), dp(6)));
            button.setTextColor(selected ? COLOR_TEXT : seatTextColor(seat));
        }
        if (totalSummaryView != null) {
            totalSummaryView.setText("Total: " + money(totalPrice()));
        }
    }

    private void toggleSeat(Seat seat) {
        if ("booked".equalsIgnoreCase(seat.type)) return;
        if (selectedSeatIds.contains(seat.id)) {
            selectedSeatIds.remove(seat.id);
        } else if (selectedSeatIds.size() < totalTicketCount()) {
            selectedSeatIds.add(seat.id);
        } else {
            toast("Select ticket quantity before choosing more seats.");
        }
        updateBookingUi();
    }

    private void showCheckout() {
        int ticketCount = totalTicketCount();
        if (selectedShowtime == null || ticketCount == 0 || selectedSeatIds.size() != ticketCount) {
            toast("Choose showtime, tickets and enough seats first.");
            return;
        }
        if (token == null) {
            showLogin();
            return;
        }

        page("Checkout", true);
        LinearLayout summary = card();
        summary.addView(text(selectedMovie.title, 24, true, COLOR_PRIMARY));
        summary.addView(text(selectedCinema == null ? "" : selectedCinema.optString("name"), 16, false, COLOR_TEXT));
        summary.addView(text(selectedShowtime.startTime + " | " + selectedShowtime.room, 15, false, COLOR_TEXT));
        summary.addView(text("Tickets: " + ticketCount, 15, false, COLOR_TEXT));
        summary.addView(text("Seats: " + selectedSeatLabels(), 15, false, COLOR_TEXT));
        summary.addView(text("Combos: " + selectedComboLabels(), 15, false, COLOR_TEXT));
        summary.addView(text("Total: " + money(totalPrice()), 24, true, COLOR_PRIMARY));
        content.addView(summary);

        Button pay = button("Pay now");
        pay.setOnClickListener(v -> createPaymentAndTickets());
        addActionButton(pay, dp(14), dp(8));
    }

    private void createPaymentAndTickets() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("user_id", userId);
            payload.put("amount", totalPrice());
            payload.put("payment_method", "Cash");
            post("/payment", payload, true, new ApiCallback() {
                @Override
                public void ok(String body) {
                    try {
                        int paymentId = new JSONObject(body).getJSONObject("payment").getInt("id");
                        createTickets(paymentId);
                    } catch (Exception e) {
                        showError(e.getMessage());
                    }
                }

                @Override
                public void fail(String message) {
                    showError(message);
                }
            });
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void createTickets(int paymentId) throws Exception {
        ArrayList<String> ticketTypeIds = expandedTicketTypeIds();
        ArrayList<String> snackIds = expandedComboIds();
        ArrayList<Seat> selectedSeats = selectedSeats();
        JSONArray tickets = new JSONArray();
        for (int i = 0; i < selectedSeats.size(); i++) {
            JSONObject ticket = new JSONObject();
            ticket.put("seat_id", selectedSeats.get(i).id);
            ticket.put("ticket_type_id", ticketTypeIds.get(i));
            if (i < snackIds.size()) ticket.put("snack_id", snackIds.get(i));
            tickets.put(ticket);
        }

        JSONObject payload = new JSONObject();
        payload.put("payment_id", paymentId);
        payload.put("user_id", userId);
        payload.put("showtime_id", selectedShowtime.id);
        payload.put("tickets", tickets);

        post("/ticket", payload, true, new ApiCallback() {
            @Override
            public void ok(String body) {
                toast("Payment successful. Tickets created.");
                showHome();
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void showHistory() {
        activeTab = TAB_MEMBER;
        if (token == null || userId < 1) {
            toast("Please sign in to view purchase history.");
            showLogin();
            return;
        }
        page("History", true);
        addLoading("Loading purchase history...");
        get("/order/user/" + userId, new ApiCallback() {
            @Override
            public void ok(String body) {
                orders.clear();
                try {
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        orders.add(new Order(arr.getJSONObject(i)));
                    }
                    renderHistory();
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void renderHistory() {
        page("History", true);
        addText("Purchase History", 25, true, COLOR_TEXT);
        if (orders.isEmpty()) {
            addText("No purchases yet.", 16, false, COLOR_MUTED);
            Button browse = button("Browse movies");
            browse.setOnClickListener(v -> showHome());
            addActionButton(browse, dp(18), dp(8));
            return;
        }
        for (Order order : orders) {
            LinearLayout card = card();
            card.addView(text(order.movie, 20, true, COLOR_PRIMARY));
            card.addView(text(order.cinema, 14, false, COLOR_TEXT));
            card.addView(text(order.showtime + " | " + order.room, 14, false, COLOR_MUTED));
            card.addView(text("Seats: " + order.seats, 14, false, COLOR_TEXT));
            card.addView(text("Paid: " + money(order.amount), 18, true, COLOR_TEXT));
            card.addView(text("Created: " + order.createdAt, 12, false, COLOR_MUTED));
            content.addView(card);
        }
    }

    private void showProfile() {
        activeTab = TAB_MEMBER;
        if (token == null || userId < 1) {
            toast("Please sign in to view your profile.");
            showLogin();
            return;
        }
        page("Profile", true);
        addLoading("Loading profile...");
        get("/user/" + userId + "/profile", new ApiCallback() {
            @Override
            public void ok(String body) {
                try {
                    UserProfile profile = new UserProfile(new JSONObject(body));
                    renderProfile(profile);
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }

            @Override
            public void fail(String message) {
                showError(message);
            }
        });
    }

    private void renderProfile(UserProfile profile) {
        page("Profile", true);
        ProfileScreen screen = new ProfileScreen(this);
        content.addView(screen.render(
                profile,
                v -> showHistory(),
                v -> showHome(),
                v -> logout()
        ));
    }
    private void showLogin() {
        page("Sign in", true);
        EditText email = input("Email", false);
        EditText pass = input("Password", true);
        content.addView(email);
        content.addView(pass);

        Button login = button("Sign in");
        login.setOnClickListener(v -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", email.getText().toString().trim());
                payload.put("password", pass.getText().toString());
                post("/auth/login", payload, false, new ApiCallback() {
                    @Override
                    public void ok(String body) {
                        try {
                            JSONObject data = new JSONObject(body);
                            JSONObject user = data.getJSONObject("user");
                            saveSession(data.getString("access_token"), user.getInt("id"), user.getString("username"));
                            toast("Signed in as " + username);
                            showHome();
                        } catch (Exception e) {
                            showError(e.getMessage());
                        }
                    }

                    @Override
                    public void fail(String message) {
                        showError(message);
                    }
                });
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
        addActionButton(login, dp(18), dp(10));

        Button signup = button("Create account");
        signup.setOnClickListener(v -> showSignup());
        addActionButton(signup, dp(10), dp(18));
    }

    private void showSignup() {
        page("Sign up", true);
        EditText name = input("Name", false);
        EditText birthday = input("Birthday YYYY-MM-DD", false);
        EditText email = input("Email", false);
        EditText usernameInput = input("Username", false);
        EditText pass = input("Password", true);
        EditText confirm = input("Confirm password", true);
        content.addView(name);
        content.addView(birthday);
        content.addView(email);
        content.addView(usernameInput);
        content.addView(pass);
        content.addView(confirm);

        Button create = button("Create account");
        create.setOnClickListener(v -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("name", name.getText().toString().trim());
                payload.put("birthday", birthday.getText().toString().trim());
                payload.put("email", email.getText().toString().trim());
                payload.put("username", usernameInput.getText().toString().trim());
                payload.put("password", pass.getText().toString());
                payload.put("confirm_password", confirm.getText().toString());
                post("/auth/signup", payload, false, new ApiCallback() {
                    @Override
                    public void ok(String body) {
                        toast("Account created. Please sign in.");
                        showLogin();
                    }

                    @Override
                    public void fail(String message) {
                        showError(message);
                    }
                });
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
        addActionButton(create, dp(18), dp(18));
    }


    private void showNews() {
        activeTab = TAB_NEWS;
        page("News & Offers", false);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button news = button("News");
        Button offers = button("Offers");
        news.setEnabled(false);
        offers.setTextColor(COLOR_TEXT);
        offers.setBackground(tint(COLOR_ACCENT, dp(8)));
        tabs.addView(news, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams offerParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        offerParams.setMargins(dp(8), 0, 0, 0);
        tabs.addView(offers, offerParams);
        content.addView(tabs);

        addOfferCard("Happy Hour", "Tickets from 45K", "Special prices before 10:00 and after 22:00.", COLOR_PRIMARY);
        addOfferCard("C'School", "Student tickets from 45K", "Weekly student and teacher discount with valid ID.", Color.rgb(139, 91, 190));
        addOfferCard("Member Day", "Earn points on every order", "Use your member account to save purchase history and rewards.", Color.rgb(82, 99, 184));
    }

    private void addOfferCard(String title, String headline, String body, int color) {
        LinearLayout card = card();
        card.setBackground(tint(color, dp(8)));
        TextView banner = text(title.toUpperCase(Locale.US), 24, true, Color.WHITE);
        banner.setGravity(Gravity.CENTER);
        card.addView(banner);
        TextView price = text(headline, 24, true, COLOR_ACCENT);
        card.addView(price);
        card.addView(text(body, 15, false, Color.WHITE));
        content.addView(card);
    }
    private void page(String title, boolean back) {
        page(title, back, -1);
    }

    private void page(String title, boolean back, int restoreScrollY) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(12), dp(12), dp(8));
        bar.setBackgroundColor(COLOR_SURFACE);

        if (back) {
            Button backButton = button("<");
            backButton.setOnClickListener(v -> {
                if (activeTab == TAB_BUY) showMovies();
                else showHome();
            });
            LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(54), dp(44));
            backParams.setMargins(0, 0, dp(18), 0);
            bar.addView(backButton, backParams);
        }

        TextView titleView = text(title, 21, true, COLOR_TEXT);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.setMargins(0, 0, dp(14), 0);
        titleView.setPadding(back ? dp(2) : 0, 0, 0, 0);
        titleView.setLayoutParams(titleParams);
        bar.addView(titleView);

        Button auth = button(token == null ? "Sign in" : username);
        auth.setOnClickListener(v -> {
            if (token == null) showLogin();
            else showProfile();
        });
        bar.addView(auth, new LinearLayout.LayoutParams(dp(94), dp(44)));
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        currentScroll = scroll;
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(32));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(bottomNav());
        setContentView(root);
        if (restoreScrollY >= 0) {
            currentScroll.post(() -> currentScroll.scrollTo(0, restoreScrollY));
        }
    }


    private LinearLayout bottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(5), dp(10), dp(7));
        nav.setBackgroundColor(COLOR_SURFACE);
        nav.addView(navIconButton("Home", R.drawable.home, TAB_HOME, v -> showHome()), navItemParams());
        nav.addView(navIconButton("Buy", R.drawable.tape_measure, TAB_BUY, v -> showMovies()), navItemParams());
        nav.addView(navIconButton("News", R.drawable.image, TAB_NEWS, v -> showNews()), navItemParams());
        nav.addView(navIconButton("Me", R.drawable.user, TAB_MEMBER, v -> showProfile()), navItemParams());
        return nav;
    }

    private LinearLayout.LayoutParams navItemParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
        params.setMargins(dp(8), 0, dp(8), 0);
        return params;
    }

    private LinearLayout navIconButton(String label, int iconRes, int tab, View.OnClickListener listener) {
        boolean selected = activeTab == tab;
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setPadding(dp(6), dp(5), dp(6), dp(5));
        item.setBackground(tint(selected ? COLOR_PRIMARY : COLOR_SURFACE, dp(18)));
        item.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(selected ? Color.WHITE : COLOR_MUTED);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        iconParams.setMargins(0, 0, 0, dp(2));
        item.addView(icon, iconParams);

        if (selected) {
            item.setScaleX(1f);
            item.setScaleY(1f);
            item.post(() -> item.animate().scaleX(1.06f).scaleY(1.06f).setDuration(180).start());
        }
        return item;
    }

    private void refreshDetailInPlace() {
        int y = currentScroll == null ? -1 : currentScroll.getScrollY();
        renderDetail(y);
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackground(tint(COLOR_SURFACE, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, dp(18));
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout qtyRow(String label, int qty, Runnable minus, Runnable plus, TextView[] countRef) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        TextView name = text(label, 15, true, COLOR_TEXT);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(name);
        Button m = button("-");
        m.setOnClickListener(v -> minus.run());
        row.addView(m, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView count = text(String.valueOf(qty), 18, true, COLOR_TEXT);
        count.setGravity(Gravity.CENTER);
        if (countRef != null && countRef.length > 0) countRef[0] = count;
        row.addView(count, new LinearLayout.LayoutParams(dp(46), dp(44)));
        Button p = button("+");
        p.setOnClickListener(v -> plus.run());
        row.addView(p, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return row;
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(value == null || value.equals("null") ? "" : value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setPadding(0, dp(5), 0, dp(5));
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private void addText(String value, int sp, boolean bold, int color) {
        content.addView(text(value, sp, bold, color));
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(tint(COLOR_PRIMARY, dp(8)));
        return b;
    }

    private void addButton(String label, View.OnClickListener listener) {
        Button b = button(label);
        b.setOnClickListener(listener);
        addActionButton(b, dp(12), dp(12));
    }

    private void addActionButton(Button button, int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        params.setMargins(0, top, 0, bottom);
        content.addView(button, params);
    }

    private EditText input(String hint, boolean password) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setHintTextColor(COLOR_MUTED);
        edit.setTextColor(COLOR_TEXT);
        edit.setSingleLine(true);
        edit.setPadding(dp(12), 0, dp(12), 0);
        edit.setBackground(tint(COLOR_SURFACE_SOFT, dp(8)));
        if (password) {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        params.setMargins(0, dp(8), 0, dp(8));
        edit.setLayoutParams(params);
        return edit;
    }

    private GradientDrawable tint(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (color == COLOR_SURFACE || color == COLOR_SURFACE_SOFT) {
            drawable.setStroke(dp(1), COLOR_BORDER);
        }
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void addLoading(String message) {
        content.removeAllViews();
        addText(message, 16, false, COLOR_MUTED);
    }

    private void showError(String message) {
        toast(message);
        addText(message, 14, false, COLOR_ERROR);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int seatColor(Seat seat) {
        if ("booked".equalsIgnoreCase(seat.type)) return Color.rgb(248, 199, 205);
        if ("vip".equalsIgnoreCase(seat.type)) return Color.rgb(139, 91, 190);
        return Color.rgb(225, 229, 239);
    }

    private int seatTextColor(Seat seat) {
        if ("booked".equalsIgnoreCase(seat.type)) return Color.rgb(142, 35, 52);
        if ("vip".equalsIgnoreCase(seat.type)) return Color.WHITE;
        return COLOR_TEXT;
    }

    private int totalTicketCount() {
        int count = 0;
        for (TicketType t : ticketTypes) count += t.quantity;
        return count;
    }

    private int totalComboCount() {
        int count = 0;
        for (Combo c : combos) count += c.quantity;
        return count;
    }

    private double totalPrice() {
        double total = 0;
        for (TicketType t : ticketTypes) total += t.price * t.quantity;
        for (Combo c : combos) total += c.price * c.quantity;
        return total;
    }

    private String money(double value) {
        return String.format(Locale.US, "%,.0f VND", value);
    }

    private void trimSeatsToTicketCount() {
        int limit = totalTicketCount();
        if (selectedSeatIds.size() <= limit) return;
        ArrayList<String> ids = new ArrayList<>(selectedSeatIds);
        selectedSeatIds.clear();
        for (int i = 0; i < limit && i < ids.size(); i++) selectedSeatIds.add(ids.get(i));
    }

    private ArrayList<Seat> selectedSeats() {
        ArrayList<Seat> result = new ArrayList<>();
        for (Seat seat : seats) {
            if (selectedSeatIds.contains(seat.id)) result.add(seat);
        }
        return result;
    }

    private String selectedSeatLabels() {
        ArrayList<String> labels = new ArrayList<>();
        for (Seat seat : selectedSeats()) labels.add(seat.number);
        return labels.isEmpty() ? "-" : join(labels);
    }

    private String selectedComboLabels() {
        ArrayList<String> labels = new ArrayList<>();
        for (Combo combo : combos) {
            if (combo.quantity > 0) labels.add(combo.name + " x " + combo.quantity);
        }
        return labels.isEmpty() ? "-" : join(labels);
    }

    private ArrayList<String> expandedTicketTypeIds() {
        ArrayList<String> ids = new ArrayList<>();
        for (TicketType type : ticketTypes) {
            for (int i = 0; i < type.quantity; i++) ids.add(type.id);
        }
        return ids;
    }

    private ArrayList<String> expandedComboIds() {
        ArrayList<String> ids = new ArrayList<>();
        for (Combo combo : combos) {
            for (int i = 0; i < combo.quantity; i++) ids.add(combo.id);
        }
        return ids;
    }

    private String join(ArrayList<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private void get(String path, ApiCallback callback) {
        request("GET", path, null, false, callback);
    }

    private void post(String path, JSONObject body, boolean auth, ApiCallback callback) {
        request("POST", path, body, auth, callback);
    }

    private void request(String method, String path, JSONObject body, boolean auth, ApiCallback callback) {
        new AsyncTask<Void, Void, ApiResult>() {
            @Override
            protected ApiResult doInBackground(Void... voids) {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(AppConfig.API_BASE + path);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestMethod(method);
                    connection.setRequestProperty("Accept", "application/json");
                    if (auth && token != null) {
                        connection.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    if (body != null) {
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "application/json");
                        byte[] bytes = body.toString().getBytes("UTF-8");
                        OutputStream os = connection.getOutputStream();
                        os.write(bytes);
                        os.close();
                    }
                    int status = connection.getResponseCode();
                    InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                    String response = read(stream);
                    return new ApiResult(status, response, null);
                } catch (Exception e) {
                    return new ApiResult(0, "", e.getMessage());
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }

            @Override
            protected void onPostExecute(ApiResult result) {
                if (result.error != null) {
                    callback.fail(result.error);
                } else if (result.status >= 200 && result.status < 300) {
                    callback.ok(result.body);
                } else {
                    callback.fail("HTTP " + result.status + ": " + result.body);
                }
            }
        }.execute();
    }

    private String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private void loadImage(String url, ImageView imageView) {
        loadImage(url, imageView, null);
    }

    private void loadImage(String url, ImageView imageView, Runnable afterSet) {
        if (url == null || url.trim().isEmpty()) return;
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... urls) {
                try {
                    InputStream stream = new URL(urls[0]).openStream();
                    return BitmapFactory.decodeStream(stream);
                } catch (Exception ignored) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) imageView.setImageBitmap(bitmap);
                if (afterSet != null) afterSet.run();
            }
        }.execute(url);
    }

    interface ApiCallback {
        void ok(String body);
        void fail(String message);
    }

    static class ApiResult {
        final int status;
        final String body;
        final String error;

        ApiResult(int status, String body, String error) {
            this.status = status;
            this.body = body;
            this.error = error;
        }
    }

    static class Movie {
        final int id;
        final String title;
        final String description;
        final String genre;
        final String country;
        final String posterUrl;
        final String ageRating;
        final String language;
        final int duration;

        Movie(JSONObject json) {
            id = json.optInt("id");
            title = json.optString("title");
            description = json.optString("description");
            genre = json.optString("genre");
            country = json.optString("country");
            posterUrl = json.optString("poster_url");
            ageRating = json.optString("age_rating");
            language = json.optString("language");
            duration = json.optInt("duration_minutes");
        }
    }

    static class Showtime {
        final String id;
        final String room;
        final String startTime;

        Showtime(JSONObject json) {
            id = json.optString("id");
            room = json.optString("room");
            startTime = json.optString("start_time");
        }
    }

    static class TicketType {
        final String id;
        final String name;
        final double price;
        int quantity = 0;

        TicketType(JSONObject json) {
            id = json.optString("id");
            name = json.optString("name");
            price = json.optDouble("base_price");
        }
    }

    static class Seat {
        final String id;
        final String number;
        final String type;

        Seat(JSONObject json) {
            id = json.optString("id");
            number = json.optString("seat_number");
            type = json.optString("seat_type");
        }
    }

    static class Order {
        final int id;
        final String movie;
        final String cinema;
        final String room;
        final String showtime;
        final String seats;
        final double amount;
        final String createdAt;

        Order(JSONObject json) {
            id = json.optInt("id");
            movie = json.optString("movie");
            cinema = json.optString("cinema");
            room = json.optString("room");
            showtime = json.optString("showtime");
            seats = json.optString("seats");
            amount = json.optDouble("amount");
            createdAt = json.optString("created_at");
        }
    }

    static class Combo {
        final String id;
        final String name;
        final double price;
        int quantity = 0;

        Combo(JSONObject json) {
            id = json.optString("id");
            name = json.optString("name");
            price = json.optDouble("price");
        }
    }
}

