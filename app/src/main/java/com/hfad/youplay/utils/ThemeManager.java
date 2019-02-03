package com.hfad.youplay.utils;


import com.hfad.youplay.Ilisteners.OnThemeChanged;
import com.hfad.youplay.R;

public class ThemeManager
{
    private static OnThemeChanged themeChanged;

    public static String DARK_THEME = "Dark";
    public static String LIGHT_THEME = "Light";

    public enum Theme
    {
        DARK_THEME,

        LIGHT_THEME
    }

    private static Theme theme;

    private ThemeManager(){}

    public static void setOnThemeChanged(OnThemeChanged onThemeChanged)
    {
        themeChanged = onThemeChanged;
    }

    public static void setTheme(Theme setTheme)
    {
        theme = setTheme;
        if(themeChanged != null)
            themeChanged.onThemeChanged();
    }

    public static String getDebug()
    {
        if(theme == Theme.DARK_THEME)
            return DARK_THEME;
        else
            return LIGHT_THEME;
    }

    public static int getTheme()
    {
        if(theme == Theme.DARK_THEME)
            return R.color.play_fragment_bars;

        return R.color.adapter_color;
    }

    public static int getFontTheme()
    {
        if(theme == Theme.DARK_THEME)
            return R.color.suggestions;

        return R.color.black;
    }

    public static int getSnackbarFont()
    {
        return R.color.white;
    }

    public static int getSelectedTheme()
    {
        if (theme == Theme.DARK_THEME)
            return R.color.play_fragment;

        return R.color.suggestions;
    }

    public static int getUnselectedTheme()
    {
        if (theme == Theme.DARK_THEME)
            return R.color.play_fragment_bars;

        return R.color.white;
    }
    public static int getDividerColor()
    {
        if(theme == Theme.DARK_THEME)
            return R.drawable.divider;

        return R.drawable.divider_light;
    }

}
