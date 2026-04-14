package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.preference.PreferenceManager;

public final class AkitaTheme {

    public static final String PREF_UI_THEME = "ui_theme";
    public static final String MODE_DARK = "dark";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_NIGHT_RED = "night_red";
    public static final String MODE_NIGHT_GREEN = "night_green";

    private AkitaTheme() {
    }

    public static Palette resolvePalette(Context context) {
        String mode = getThemeMode(context);
        if (MODE_LIGHT.equalsIgnoreCase(mode)) {
            return lightPalette();
        }
        if (MODE_NIGHT_RED.equalsIgnoreCase(mode)) {
            return nightRedPalette();
        }
        if (MODE_NIGHT_GREEN.equalsIgnoreCase(mode)) {
            return nightGreenPalette();
        }
        return darkPalette();
    }

    public static String getThemeMode(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_UI_THEME, MODE_DARK);
    }

    public static boolean isDarkMode(Context context) {
        return MODE_DARK.equalsIgnoreCase(getThemeMode(context));
    }

    public static boolean isNightRedMode(Context context) {
        return MODE_NIGHT_RED.equalsIgnoreCase(getThemeMode(context));
    }

    public static boolean isNightGreenMode(Context context) {
        return MODE_NIGHT_GREEN.equalsIgnoreCase(getThemeMode(context));
    }

    public static void setThemeMode(Context context, String mode) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_UI_THEME, mode)
                .apply();
    }

    public static String nextThemeMode(Context context) {
        return nextThemeMode(getThemeMode(context));
    }

    public static String nextThemeMode(String currentMode) {
        if (MODE_DARK.equalsIgnoreCase(currentMode)) {
            return MODE_LIGHT;
        }
        if (MODE_LIGHT.equalsIgnoreCase(currentMode)) {
            return MODE_NIGHT_RED;
        }
        if (MODE_NIGHT_RED.equalsIgnoreCase(currentMode)) {
            return MODE_NIGHT_GREEN;
        }
        return MODE_DARK;
    }

    public static String getThemeLabel(Context context) {
        String mode = getThemeMode(context);
        if (MODE_LIGHT.equalsIgnoreCase(mode)) {
            return "Light Ops";
        }
        if (MODE_NIGHT_RED.equalsIgnoreCase(mode)) {
            return "Night Red";
        }
        if (MODE_NIGHT_GREEN.equalsIgnoreCase(mode)) {
            return "Night Green";
        }
        return "Dark Ops";
    }

    public static GradientDrawable createPanelDrawable(Context context, Palette palette, boolean elevated) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(palette.monochrome ? palette.background : elevated ? palette.surfaceElevated : palette.surface);
        drawable.setCornerRadius(dp(context, elevated ? 26 : 22));
        drawable.setStroke(dp(context, 1), palette.monochrome ? palette.accent : withAlpha(palette.outline, elevated ? 200 : 155));
        return drawable;
    }

    public static GradientDrawable createAccentPanelDrawable(Context context, Palette palette) {
        GradientDrawable drawable = new GradientDrawable();
        String mode = getThemeMode(context);
        int alpha = MODE_LIGHT.equalsIgnoreCase(mode) ? 34 : MODE_NIGHT_RED.equalsIgnoreCase(mode) ? 220 : 150;
        drawable.setColor(palette.monochrome ? palette.background : withAlpha(palette.navy, alpha));
        drawable.setCornerRadius(dp(context, 26));
        drawable.setStroke(dp(context, 1), palette.monochrome ? palette.accent : withAlpha(palette.accentStrong, 210));
        return drawable;
    }

    public static GradientDrawable createStatTileDrawable(Context context, Palette palette) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(palette.monochrome ? palette.background : withAlpha(palette.surfaceElevated, 245));
        drawable.setCornerRadius(dp(context, 18));
        drawable.setStroke(dp(context, 1), palette.monochrome ? palette.accent : withAlpha(palette.outline, 145));
        return drawable;
    }

    public static GradientDrawable createInputDrawable(Context context, Palette palette) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(palette.monochrome ? palette.background : withAlpha(palette.surfaceElevated, 250));
        drawable.setCornerRadius(dp(context, 18));
        drawable.setStroke(dp(context, 1), palette.monochrome ? palette.accent : withAlpha(palette.outline, 190));
        return drawable;
    }

    public static GradientDrawable createAccentButtonDrawable(Context context, Palette palette) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(palette.monochrome ? palette.background : palette.accent);
        drawable.setCornerRadius(dp(context, 18));
        drawable.setStroke(dp(context, 1), palette.monochrome ? palette.accent : palette.accentStrong);
        return drawable;
    }

    public static GradientDrawable createDangerButtonDrawable(Context context, Palette palette) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(palette.monochrome ? palette.background : palette.danger);
        drawable.setCornerRadius(dp(context, 18));
        drawable.setStroke(dp(context, 1), palette.monochrome ? palette.accent : withAlpha(palette.white, 65));
        return drawable;
    }

    public static GradientDrawable createBadgeDrawable(Context context, Palette palette, boolean accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(context, 999));
        drawable.setColor(palette.monochrome ? palette.background : accent ? withAlpha(palette.accent, 225) : withAlpha(palette.surfaceElevated, 250));
        drawable.setStroke(dp(context, 1), palette.monochrome ? palette.accent : accent ? withAlpha(palette.white, 90) : withAlpha(palette.outline, 165));
        return drawable;
    }

    public static GradientDrawable createDotDrawable(Context context, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(dp(context, 10), dp(context, 10));
        return drawable;
    }

    public static int statusColor(String status, Palette palette) {
        if (palette.monochrome) {
            return palette.accent;
        }
        String lowerStatus = status == null ? "" : status.toLowerCase();
        if (lowerStatus.contains("connected") || lowerStatus.contains("ready") || lowerStatus.contains("good")) {
            return palette.success;
        }
        if (lowerStatus.contains("connecting") || lowerStatus.contains("scanning") || lowerStatus.contains("searching") || lowerStatus.contains("medium")) {
            return palette.warning;
        }
        if (lowerStatus.contains("error") || lowerStatus.contains("failed") || lowerStatus.contains("disconnected") || lowerStatus.contains("low")) {
            return palette.danger;
        }
        return palette.textPrimary;
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static Palette darkPalette() {
        return new Palette(
                Color.parseColor("#09090B"),   // background — near-black
                Color.parseColor("#131316"),   // surface — charcoal
                Color.parseColor("#1C1C21"),   // surfaceElevated — dark graphite
                Color.parseColor("#6E6E76"),   // surfaceAccent — titanium mid
                Color.parseColor("#3F3F46"),   // outline — zinc-700
                Color.parseColor("#FAFAFA"),   // textPrimary — white
                Color.parseColor("#A1A1AA"),   // textSecondary — zinc-400
                Color.parseColor("#71717A"),   // textMuted — zinc-500
                Color.parseColor("#A1A1AA"),   // accent — silver
                Color.parseColor("#D4D4D8"),   // accentStrong — zinc-300
                Color.parseColor("#27272A"),   // navy (secondary fill) — zinc-800
                Color.parseColor("#D4D4D8"),   // silver — zinc-300
                Color.parseColor("#EF4444"),   // danger — red
                Color.parseColor("#EAB308"),   // warning — yellow
                Color.parseColor("#22C55E"),   // success — green
                Color.parseColor("#27272A"),   // grid — zinc-800
                Color.parseColor("#FFFFFF"),   // white
                false);
    }

    public static Palette lightPalette() {
        return new Palette(
                Color.parseColor("#F4F4F5"),   // background — zinc-100
                Color.parseColor("#FFFFFF"),   // surface — white
                Color.parseColor("#E4E4E7"),   // surfaceElevated — zinc-200
                Color.parseColor("#71717A"),   // surfaceAccent — titanium mid
                Color.parseColor("#A1A1AA"),   // outline — zinc-400
                Color.parseColor("#09090B"),   // textPrimary — near-black
                Color.parseColor("#52525B"),   // textSecondary — zinc-600
                Color.parseColor("#71717A"),   // textMuted — zinc-500
                Color.parseColor("#52525B"),   // accent — graphite
                Color.parseColor("#3F3F46"),   // accentStrong — zinc-700
                Color.parseColor("#E4E4E7"),   // navy (secondary fill) — zinc-200
                Color.parseColor("#A1A1AA"),   // silver — zinc-400
                Color.parseColor("#DC2626"),   // danger — red-600
                Color.parseColor("#CA8A04"),   // warning — yellow-600
                Color.parseColor("#16A34A"),   // success — green-600
                Color.parseColor("#D4D4D8"),   // grid — zinc-300
                Color.parseColor("#FFFFFF"),   // white
                false);
    }

    public static Palette nightRedPalette() {
        return new Palette(
                Color.parseColor("#000000"),
                Color.parseColor("#000000"),
                Color.parseColor("#000000"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                Color.parseColor("#FF3B30"),
                true);
    }

    public static Palette nightGreenPalette() {
        return new Palette(
                Color.parseColor("#000000"),
                Color.parseColor("#000000"),
                Color.parseColor("#000000"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                Color.parseColor("#33FF00"),
                true);
    }

    private static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    public static final class Palette {
        public final int background;
        public final int surface;
        public final int surfaceElevated;
        public final int surfaceAccent;
        public final int outline;
        public final int textPrimary;
        public final int textSecondary;
        public final int textMuted;
        public final int accent;
        public final int accentStrong;
        public final int navy;
        public final int silver;
        public final int danger;
        public final int warning;
        public final int success;
        public final int grid;
        public final int white;
        public final boolean monochrome;

        private Palette(int background,
                        int surface,
                        int surfaceElevated,
                        int surfaceAccent,
                        int outline,
                        int textPrimary,
                        int textSecondary,
                        int textMuted,
                        int accent,
                        int accentStrong,
                        int navy,
                        int silver,
                        int danger,
                        int warning,
                        int success,
                        int grid,
                        int white,
                        boolean monochrome) {
            this.background = background;
            this.surface = surface;
            this.surfaceElevated = surfaceElevated;
            this.surfaceAccent = surfaceAccent;
            this.outline = outline;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.textMuted = textMuted;
            this.accent = accent;
            this.accentStrong = accentStrong;
            this.navy = navy;
            this.silver = silver;
            this.danger = danger;
            this.warning = warning;
            this.success = success;
            this.grid = grid;
            this.white = white;
            this.monochrome = monochrome;
        }
    }
}