package com.hfad.youplay;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.hfad.youplay.Ilisteners.OnThemeChanged;
import com.hfad.youplay.fragments.SettingsFragment;
import com.hfad.youplay.utils.ThemeManager;


public class SettingsActivity extends AppCompatActivity implements OnThemeChanged{

    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(ThemeManager.getDebug().equals(ThemeManager.DARK_THEME))
            setTheme(R.style.PreferenceScreen);
        else
            setTheme(R.style.LightTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
//        RelativeLayout relativeLayout = findViewById(R.id.settings_layout);

        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(R.string.settings);
        SettingsFragment fragment = new SettingsFragment();
        fragment.setListener(this);

        toolbar.setTitleTextColor(getResources().getColor(ThemeManager.getFontTheme()));
//        toolbar.setBackgroundColor(getResources().getColor(ThemeManager.getToolbarTheme()));
//        relativeLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    @Override
    public void onThemeChanged()
    {
        if(ThemeManager.getDebug().equals("Dark"))
            setTheme(R.style.PreferenceScreen);
        else
            setTheme(R.style.LightTheme);

        finish();
        startActivity(getIntent());
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ako korisnik izade iz aplikacije dok je u postavkama, najbolje je zavrsit activity.
        // Jer moze bit da korisnik izade iz aplikacije i unisti aplikaciju preko notifikacije
        // Pa moze ostat ne unisteni activity.
        finish();
    }
}
