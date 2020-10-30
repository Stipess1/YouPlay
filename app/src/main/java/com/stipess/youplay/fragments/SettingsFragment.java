package com.stipess.youplay.fragments;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.stipess.youplay.AudioService;
import com.stipess.youplay.BuildConfig;
import com.stipess.youplay.Ilisteners.OnThemeChanged;
import com.stipess.youplay.R;
import com.stipess.youplay.fragments.preference.BasePreferenceFragmentCompat;
import com.stipess.youplay.utils.Constants;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.ThemeManager;
import com.stipess.youplay.utils.Utils;
import com.stipess.youplay.web.YouPlayWeb;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;


/**
 * Created by Stjepan Stjepanovic
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * SettingsFragment.java is part of YouPlay.
 * <p>
 * YouPlay is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * YouPlay is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with YouPlay.  If not, see <http://www.gnu.org/licenses/>.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private OnThemeChanged onThemeChanged;

    public static final String KEY = "Theme";

    public SettingsFragment() {
        // Required empty public constructor
    }

    public void setListener(OnThemeChanged onThemeChanged)
    {
        this.onThemeChanged = onThemeChanged;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean pre = pref.getBoolean(SettingsFragment.KEY, false);
        Window window = getActivity().getWindow();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(pre) {
                window.setStatusBarColor(getResources().getColor(R.color.toolbar_color));
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else{
                window.setStatusBarColor(getResources().getColor(R.color.adapter_color));
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }


        PreferenceManager.setDefaultValues(getActivity(), R.xml.preference, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        PreferenceScreen screen = (PreferenceScreen) findPreference(getResources().getString(R.string.current_version));
        screen.setSummary(BuildConfig.VERSION_NAME);

        PreferenceScreen versionCheck = (PreferenceScreen) findPreference(Constants.VERSION_CHECK);
        versionCheck.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                YouPlayWeb web = new YouPlayWeb();
                web.setListener(new YouPlayWeb.Listener() {
                    @Override
                    public void onConnected(String version) {
                        if (version != null && !AudioService.getInstance().isDestroyed())
                            buildAlertDialog(Utils.needsUpdate(version), version);
                    }

                    @Override
                    public void onError(Exception e) {
                        if(!AudioService.getInstance().isDestroyed())
                            Toast.makeText(getContext(), getResources().getString(R.string.download_error), Toast.LENGTH_SHORT).show();
                    }
                });

                web.execute();
                return false;
            }
        });

        setDownload();

        PreferenceScreen sendMail = (PreferenceScreen) findPreference(Constants.SEND_EMAIL);
        sendMail.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "stipess@youplayandroid.com", null));
                startActivity(Intent.createChooser(emailIntent, SettingsFragment.this.getResources().getString(R.string.send_email)));
                return false;
            }
        });

        PreferenceScreen website = (PreferenceScreen) findPreference(Constants.WEBSITE);
        website.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent website1 = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.YOUPLAY_WEBSITE));
                startActivity(website1);
                return true;
            }
        });

        PreferenceScreen contribute = (PreferenceScreen) findPreference(Constants.CONTRIBUTE);
        contribute.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent website = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.YOUPLAY_WEBSITE+"/contribute"));
                startActivity(website);
                return true;
            }
        });

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.equals(SettingsFragment.this.getString(R.string.switch_key))) {
                    SwitchPreference switchPreference = (SwitchPreference) SettingsFragment.this.findPreference(SettingsFragment.this.getString(R.string.switch_key));

                    if (switchPreference.isChecked())
                        ThemeManager.setTheme(ThemeManager.Theme.DARK_THEME);
                    else
                        ThemeManager.setTheme(ThemeManager.Theme.LIGHT_THEME);

                    Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            onThemeChanged.onThemeChanged();
                        }
                    };
                    handler.postDelayed(runnable, 250);
                }
            }
        };
    }

    private void setDownload() {
        CheckBoxPreference preference = (CheckBoxPreference) findPreference(Constants.CACHE_MODE);
        if(!preference.isEnabled()) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent website1 = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.YOUPLAY_WEBSITE+"#download"));
                    startActivity(website1);
                    return true;
                }
            });
        }
    }

    private void buildAlertDialog(boolean needsUpdate, String version)
    {
        if(needsUpdate)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getResources().getString(R.string.version_update))
                    .setMessage(getResources().getString(R.string.version_update_summary, version));
            builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    download(Constants.DOWNLOAD_LINK);
                }
            });
            builder.setNegativeButton(R.string.rationale_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.create().show();
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getResources().getString(R.string.version_newest))
                    .setMessage(getResources().getString(R.string.version_newest_summary));
            builder.setPositiveButton(R.string.rationale_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.create().show();
        }
    }

    private void download(String link)
    {
        FileManager.getDownloadFolder().delete();
        BaseDownloadTask task = FileDownloader.getImpl().create(link).setPath(FileManager.getDownloadFolder().getPath());

        final ProgressDialog downloadDialog = new ProgressDialog(getContext());
        FileDownloadQueueSet queueSet = new FileDownloadQueueSet(new FileDownloadListener() {
            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {

            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                double divide = (double) soFarBytes / totalBytes;
                double math = (double) downloadDialog.getMax() * divide;
                downloadDialog.setProgress((int) math);
            }

            @Override
            protected void started(BaseDownloadTask task) {
                downloadDialog.setMessage(getContext().getString(R.string.downloading));
                downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                downloadDialog.setProgress(0);
                downloadDialog.setMax(100);
                downloadDialog.setCancelable(false);
                downloadDialog.setProgressNumberFormat(null);
                downloadDialog.dismiss();
                downloadDialog.show();
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                downloadDialog.dismiss();
                if(!AudioService.getInstance().isDestroyed()) {
                    downloadDialog.dismiss();
                    Uri apkURI = (Build.VERSION.SDK_INT >= 24)
                            ? FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", FileManager.getDownloadFolder())
                            : Uri.fromFile(FileManager.getDownloadFolder());

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(apkURI, "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(intent);
                }
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {

            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {

            }

            @Override
            protected void warn(BaseDownloadTask task) {

            }
        });
        queueSet.downloadSequentially(task);
        queueSet.start();
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        super.setPreferenceScreen(preferenceScreen);
        if (preferenceScreen != null) {
            int count = preferenceScreen.getPreferenceCount();
            for (int i = 0; i < count; i++)
                preferenceScreen.getPreference(i).setIconSpaceReserved(false);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
//        setPreferencesFromResource(R.xml.preference, rootKey);
    }


    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

}
