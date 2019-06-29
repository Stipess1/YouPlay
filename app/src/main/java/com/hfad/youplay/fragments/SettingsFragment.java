package com.hfad.youplay.fragments;


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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hfad.youplay.AudioService;
import com.hfad.youplay.BuildConfig;
import com.hfad.youplay.Ilisteners.OnThemeChanged;
import com.hfad.youplay.MainActivity;
import com.hfad.youplay.R;
import com.hfad.youplay.fragments.preference.BasePreferenceFragmentCompat;
import com.hfad.youplay.utils.Constants;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.ThemeManager;
import com.hfad.youplay.utils.Utils;
import com.hfad.youplay.web.YouPlayWeb;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends BasePreferenceFragmentCompat{

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

                    final String[] downloadLink = new String[1];
                    if(MainActivity.noAdApp && !MainActivity.isGooglePlay)
                    {
                        FirebaseApp.initializeApp(getContext());
                        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                        StorageReference reference = firebaseStorage.getReference();
                        reference.child("youplay/YouPlay.apk").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                downloadLink[0] = uri.toString();
                                download(downloadLink[0]);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("YouPlayAndroid", "Fails: " + e.toString());
                                e.printStackTrace();
                            }
                        });
                        dialogInterface.cancel();
                    }
                    else
                    {
                        if(!MainActivity.isGooglePlay)
                        {
                            downloadLink[0] = Constants.DOWNLOAD_LINK;
                            download(downloadLink[0]);
                        }
                        else
                        {
                            String appPackageName = getActivity().getPackageName(); // getPackageName() from Context or Activity object
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        }
                    }
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
//        DownloadTask.Builder task = new DownloadTask.Builder(link, FileManager.getDownloadFolder());
//        DownloadTask downloadTask = task.build();
//        final ProgressDialog downloadDialog = new ProgressDialog(getContext());
//        downloadTask.enqueue(new DownloadListener1() {
//            @Override
//            public void taskStart(@NonNull DownloadTask task, @NonNull Listener1Assist.Listener1Model model) {
//
//                downloadDialog.setMessage(getContext().getString(R.string.downloading));
//                downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                downloadDialog.setProgress(0);
//                downloadDialog.setMax(100);
//                downloadDialog.setCancelable(false);
//                downloadDialog.setProgressNumberFormat(null);
//                downloadDialog.show();
//            }
//
//            @Override
//            public void retry(@NonNull DownloadTask task, @NonNull ResumeFailedCause cause) {
//
//            }
//
//            @Override
//            public void connected(@NonNull DownloadTask task, int blockCount, long currentOffset, long totalLength) {
//
//            }
//
//            @Override
//            public void progress(@NonNull DownloadTask task, long currentOffset, long totalLength) {
//                double divide = (double) currentOffset / totalLength;
//                double math = (double) downloadDialog.getMax() * divide;
//                downloadDialog.setProgress((int) math);
//            }
//
//            @Override
//            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, @NonNull Listener1Assist.Listener1Model model) {
//                downloadDialog.cancel();
//
//                if(cause == EndCause.COMPLETED && !AudioService.getInstance().isDestroyed())
//                {
//                    downloadDialog.cancel();
//                    Uri apkURI = (Build.VERSION.SDK_INT >= 24)
//                            ? FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", FileManager.getDownloadFolder())
//                            : Uri.fromFile(FileManager.getDownloadFolder());
//
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    intent.setDataAndType(apkURI, "application/vnd.android.package-archive");
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                    startActivity(intent);
//                }
//                if(cause == EndCause.ERROR)
//                    realCause.printStackTrace();
//
//
//            }
//        });
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
