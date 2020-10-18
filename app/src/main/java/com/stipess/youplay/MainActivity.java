package com.stipess.youplay;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.stipess.youplay.Ilisteners.OnItemClicked;
import com.stipess.youplay.Ilisteners.OnThemeChanged;
import com.stipess.youplay.adapter.RadioAdapter;
import com.stipess.youplay.apprater.AppRater;
import com.stipess.youplay.apprater.GoogleMarket;
import com.stipess.youplay.database.YouPlayDatabase;
import com.stipess.youplay.fragments.DefaultPlaylistFragment;
import com.stipess.youplay.fragments.HistoryFragment;
import com.stipess.youplay.fragments.PlayFragment;
import com.stipess.youplay.fragments.SearchFragment;
import com.stipess.youplay.fragments.RadioFragment;
import com.stipess.youplay.fragments.SettingsFragment;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.player.AudioPlayer;
import com.stipess.youplay.utils.Constants;
import com.stipess.youplay.radio.Station;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.ThemeManager;
import com.stipess.youplay.utils.Utils;
import com.stipess.youplay.web.YouPlayWeb;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;



public class MainActivity extends AppCompatActivity implements AudioService.ServiceCallback
        , EasyPermissions.PermissionCallbacks, OnThemeChanged, OnItemClicked {

    private static final String TAG = MainActivity.class.getSimpleName();
    // kada je true daj link od verzije bez reklama, ostalo daj sa reklamom.
    public static final boolean noAdApp = true;
    // Kada je false znaci da ova verzija nebi trebala bit na google playu niti na galaxy store.
    // i mora biti false za ovo za korisnike koji nemaju reklame
    public static final boolean isGooglePlay = false;

    private static final String EMAIL = "stjepstjepanovic@gmail.com";

    private TabLayout tabLayout;
    private HistoryFragment historyFragment;
    private PlayFragment playFragment;
    private SearchFragment searchFragment;
    private DefaultPlaylistFragment defaultPlaylistFragment;
    private RadioFragment radioFragment;
    private AudioService audioService;
    private boolean bound = false;
    private boolean isRunning = false;
    public ViewPager pager;
    private YouPlayDatabase db;
    private InputMethodManager imm;
    private AudioPlayer audioPlayer;
    private static Context context;
    private boolean pre;
    // da nam otvori history prilikom prvom pokretanju
    private boolean firstTime = true;
    /*
    kada je wifi ugasen i korisnik stisne strelicu, visina viewa ce se postavit , tada reklama ce offsetat recylcerview preko kontroli (next,prev,playpause).
     */
    public static int size = -1;
    public static boolean adLoaded;

    private int[] imageId = {
            R.drawable.music,
            R.drawable.history,
            R.drawable.playlist,
            R.drawable.search
    };

    public static final String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private ServiceConnection connection;

    @Override
    protected void onStart() {
        Intent intent = new Intent(getApplication(), AudioService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent);
        else
            startService(intent);

        isRunning = true;
        if(firstTime)
        {
            pager.setCurrentItem(1);
            firstTime = false;
        }
        context = getApplicationContext();
        FileDownloader.setupOnApplicationOnCreate(getApplication());

        super.onStart();
    }


    @Override
    protected void onPause() {
        isRunning = false;
        hideKeyboard();

        super.onPause();
    }

    private void hideKeyboard()
    {
        if(imm != null && getCurrentFocus() != null)
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PreferenceManager.setDefaultValues(this, R.xml.preference, true);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        pre = preferences.getBoolean(SettingsFragment.KEY, false);
        if(pre)
            setTheme(R.style.BlackTheme);
        else
            setTheme(R.style.LightTheme);

        super.onCreate(savedInstanceState);
//        Fabric.with(this, new Crashlytics());
//        Crashlytics.setUserEmail(EMAIL);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);


        if(pre)
            ThemeManager.setTheme(ThemeManager.Theme.DARK_THEME);
        else
            ThemeManager.setTheme(ThemeManager.Theme.LIGHT_THEME);

        ThemeManager.setOnThemeChanged(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        historyFragment         = new HistoryFragment();
        playFragment            = new PlayFragment();
        radioFragment           = new RadioFragment();
        searchFragment          = new SearchFragment();
        defaultPlaylistFragment = new DefaultPlaylistFragment();

        if (savedInstanceState != null)
        {
            historyFragment = (HistoryFragment) getSupportFragmentManager().getFragment(savedInstanceState, Constants.HISTORY_FRAGMENT);
            playFragment = (PlayFragment) getSupportFragmentManager().getFragment(savedInstanceState, Constants.PLAY_FRAGMENT);
            radioFragment = (RadioFragment) getSupportFragmentManager().getFragment(savedInstanceState, Constants.RADIO_FRAGMENT);
        }

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                AudioService.AudioBinder audioBinder = (AudioService.AudioBinder) iBinder;
                audioService = audioBinder.getAudio();
                AudioService.getInstance().setDestroyed(false);
                audioPlayer = audioService.getAudioPlayer();
                audioService.setCallback(MainActivity.this);

                historyFragment.initAudioService();
                playFragment.initAudioService();
                searchFragment.initAudioService();

                bound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                bound = false;
            }
        };

        SectionsPagerAdapter pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        pager = findViewById(R.id.pager);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(5);

        db = new YouPlayDatabase(getApplicationContext());
        checkPermissions(PERMISSIONS);


        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(pager);
        tabLayout.setBackgroundColor(getResources().getColor(R.color.light_black));
        registerListeners();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(historyFragment != null)
        {
            if(historyFragment.isAdded() && playFragment.isAdded() && radioFragment.isAdded())
            {
                getSupportFragmentManager().putFragment(outState, Constants.HISTORY_FRAGMENT, historyFragment);
                getSupportFragmentManager().putFragment(outState, Constants.PLAY_FRAGMENT, playFragment);
                getSupportFragmentManager().putFragment(outState, Constants.RADIO_FRAGMENT, radioFragment);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(bound)
        {
            unbindService(connection);
            bound = false;
        }
        if(audioService != null)
        {
            audioService.setDestroyed(true);
            audioService.setCallback(null);
            audioPlayer.setPlayerState(null);
        }
        size = -1;
        FileDownloader.getImpl().pauseAll();
//        OkDownload.with().downloadDispatcher().cancelAll();
        ThemeManager.setOnThemeChanged(null);
        super.onDestroy();
    }

    public void registerListeners()
    {
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }


            @Override
            public void onPageSelected(final int position) {

                SearchView searchView = findViewById(R.id.search_song);

                if(searchView != null)
                    searchView.clearFocus();

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                pre = preferences.getBoolean(SettingsFragment.KEY, false);

                Window window = getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                switch (position)
                {
                    case 0:
                        boolean nav = preferences.getBoolean("navigation", false);

                        if(nav)
                            tabLayout.setVisibility(View.GONE);
                        else
                        {
                            tabLayout.setBackgroundColor(getResources().getColor(R.color.play_fragment_bars));
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                window.setStatusBarColor(getResources().getColor(R.color.black_b));
                                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                            }

                            tabLayout.getTabAt(position).setIcon(R.drawable.music_pressed);
                            tabLayout.getTabAt(1).setIcon(R.drawable.history);
                            tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                            tabLayout.getTabAt(4).setIcon(R.drawable.search);
                            tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                        }
                        break;
                    case 1:
                        tabLayout.setVisibility(View.VISIBLE);
                        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if(pre)
                                window.setStatusBarColor(getResources().getColor(R.color.toolbar_color));
                            else{
                                window.setStatusBarColor(getResources().getColor(R.color.adapter_color));
                                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                            }
                        }

                        tabLayout.getTabAt(0).setIcon(R.drawable.music);
                        tabLayout.getTabAt(1).setIcon(R.drawable.history_pressed);
                        tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                        tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                        tabLayout.getTabAt(4).setIcon(R.drawable.search);

                        break;
                    case 2:
                        tabLayout.setVisibility(View.VISIBLE);
                        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if(pre)
                                window.setStatusBarColor(getResources().getColor(R.color.toolbar_color));
                            else{
                                window.setStatusBarColor(getResources().getColor(R.color.adapter_color));
                                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                            }
                        }

                        tabLayout.getTabAt(0).setIcon(R.drawable.music);
                        tabLayout.getTabAt(1).setIcon(R.drawable.history);
                        tabLayout.getTabAt(4).setIcon(R.drawable.search);
                        tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                        tabLayout.getTabAt(position).setIcon(R.drawable.playlist_pressed);
                        break;
                    case 3:
                        tabLayout.setVisibility(View.VISIBLE);
                        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if(pre)
                                window.setStatusBarColor(getResources().getColor(R.color.toolbar_color));
                            else{
                                window.setStatusBarColor(getResources().getColor(R.color.adapter_color));
                                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                            }
                        }

                        tabLayout.getTabAt(1).setIcon(R.drawable.history);
                        tabLayout.getTabAt(0).setIcon(R.drawable.music);
                        tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                        tabLayout.getTabAt(4).setIcon(R.drawable.search);
                        tabLayout.getTabAt(position).setIcon(R.drawable.radio_pressed);

                        break;
                    case 4:
                        tabLayout.setVisibility(View.VISIBLE);
                        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if(pre)
                                window.setStatusBarColor(getResources().getColor(R.color.play_fragment_bars));
                            else {
                                window.setStatusBarColor(getResources().getColor(R.color.white));
                                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                            }
                        }

                        tabLayout.getTabAt(1).setIcon(R.drawable.history);
                        tabLayout.getTabAt(0).setIcon(R.drawable.music);
                        tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                        tabLayout.getTabAt(position).setIcon(R.drawable.search_pressed);
                        tabLayout.getTabAt(3).setIcon(R.drawable.radio);

                        searchView.requestFocus();
//
                        if(imm == null)
                            imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.SHOW_IMPLICIT);
                        break;

                }
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        initFiles();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        this.finishAffinity();
    }

    public void checkPermissions(String... permissions)
    {
        Log.d("Permissions", "Permissions: " + EasyPermissions.hasPermissions(this, permissions));
        if(EasyPermissions.hasPermissions(this, permissions))
        {
            initFiles();

            // Pogledaj dali ima nova verzija YouPlay-a
            YouPlayWeb web = new YouPlayWeb();
            web.setListener(new YouPlayWeb.Listener() {
                @Override
                public void onConnected(String version) {
                    if(version != null && isRunning)
                        buildAlertDialog(Utils.needsUpdate(version), version);
                }

                @Override
                public void onError(Exception e) {

                }
            });
            web.execute();

        }
        else
        {
            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this, Constants.PERMISSION_ALL, permissions)
                                                .setRationale(R.string.rationale_ask)
                                                .setPositiveButtonText(R.string.rationale_ok)
                                                .setNegativeButtonText(R.string.rationale_cancel)
                                                .build());
        }
    }

    private void buildAlertDialog(boolean needsUpdate, String version)
    {

        if(needsUpdate)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.version_update))
                    .setMessage(getResources().getString(R.string.version_update_summary, version));
            builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                    final String[] downloadLink = new String[1];
                    if(noAdApp && !isGooglePlay)
                    {
                        FirebaseApp.initializeApp(getApplicationContext());
                        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                        StorageReference reference = firebaseStorage.getReference();
                        reference.child("youplay/YouPlay.apk").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                downloadLink[0] = uri.toString();
                                Log.d(TAG, "URL: " + downloadLink[0]);
                                download(downloadLink[0]);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("YouPlayAndroid", "Fails: " + e.toString());
                                e.printStackTrace();
                            }
                        });

                    }
                    else
                    {
                        downloadLink[0] = Constants.DOWNLOAD_LINK;
                        download(downloadLink[0]);
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
        else{
            if(isGooglePlay) {
                AppRater.setMarket(new GoogleMarket());
                AppRater.setPackageName("com.stipess.youplay");
                AppRater.app_launched(this);
            }
        }
    }

    private void download(String link)
    {
        FileManager.getDownloadFolder().delete();
        BaseDownloadTask task = FileDownloader.getImpl().create(link).setPath(FileManager.getDownloadFolder().getPath());
        final ProgressDialog downloadDialog = new ProgressDialog(MainActivity.this);
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
                downloadDialog.setMessage(getApplicationContext().getString(R.string.downloading));
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
                if(getApplication() != null) {
                    downloadDialog.dismiss();
                    Uri apkURI = (Build.VERSION.SDK_INT >= 24)
                            ? FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", FileManager.getDownloadFolder())
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
//        final ProgressDialog downloadDialog = new ProgressDialog(MainActivity.this);
//        downloadTask.enqueue(new DownloadListener1() {
//            @Override
//            public void taskStart(@NonNull DownloadTask task, @NonNull Listener1Assist.Listener1Model model) {
//
//                downloadDialog.setMessage(getApplicationContext().getString(R.string.downloading));
//                downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                downloadDialog.setProgress(0);
//                downloadDialog.setMax(100);
//                downloadDialog.setCancelable(false);
//                downloadDialog.setProgressNumberFormat(null);
//                downloadDialog.dismiss();
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
//                downloadDialog.dismiss();
//                if(cause == EndCause.COMPLETED && getApplication() != null)
//                {
//                    downloadDialog.dismiss();
//                    Uri apkURI = (Build.VERSION.SDK_INT >= 24)
//                            ? FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", FileManager.getDownloadFolder())
//                            : Uri.fromFile(FileManager.getDownloadFolder());
//
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    intent.setDataAndType(apkURI, "application/vnd.android.package-archive");
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                    startActivity(intent);
//                }
//
//
//            }
//        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public void initFiles() {

        File file = FileManager.getRootPath();
        if(!file.exists())
            file.mkdirs();

        File images = FileManager.getPictureFolder();
        if(!images.exists())
            images.mkdirs();

        File databaseFile = FileManager.getDatabaseFolder();
        if(!databaseFile.exists())
            databaseFile.mkdirs();

        try {
            db.createPlaylistDatabase();
        } catch (Exception e) {
            Log.d("Exception", e.getMessage());
            Toast.makeText(this, getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
            // Zna se desit kada se uklone permisije u postavkama da nezeli uci u app
            // sve dok se ne "force stop" aplikacija pa ovdje radimo rucno
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
            finishAffinity();
        }

    }

    @Override
    public void onMusicClick(Music pjesma) {
        historyFragment.onClick(pjesma, null);
    }

    @Override
    public void onMusicClick(Music pjesma, ArrayList<Music> pjesme, String table, boolean shuffled) {
        ArrayList<Music> temp = new ArrayList<>();
        if(pjesme != null)
            temp.addAll(pjesme);

        audioPlayer.setStream(false);
        if(shuffled) {
            audioPlayer.setMusicList(temp);
            Collections.shuffle(temp);
            pjesma = temp.get(0);
            playFragment.setShuffled();
        } else {
            playFragment.setUnshuffled();
        }

        playFragment.setMusic(pjesma, temp);
    }

    @Override
    public void refreshSearchList(Music pjesma) {
        searchFragment.refreshMusicList(pjesma);
    }

    @Override
    public void setStation(Station station) {
        Intent intent = new Intent(this, AudioService.class);
        audioPlayer.setStream(true);
        intent.putExtra(AudioService.SONG, station);
        intent.putExtra(AudioService.ACTION, Constants.PLAY_SONG);
        startService(intent);
    }

    @Override
    public void refreshSuggestions(ArrayList<Music> data, boolean queue) {
        playFragment.refreshList(data, queue);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(pager != null && pager.getAdapter() != null) {
            pager.getAdapter().notifyDataSetChanged();
        }

        putCurrentIcon();
    }

    private boolean internetConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private void putCurrentIcon() {
        for(int i = 0; i < imageId.length; i++) {
            tabLayout.getTabAt(i).setIcon(imageId[i]);
        }
        int current = pager.getCurrentItem();
        switch (current) {
            case 0:
                tabLayout.getTabAt(current).setIcon(R.drawable.music_pressed);
                tabLayout.getTabAt(1).setIcon(R.drawable.history);
                tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                tabLayout.getTabAt(4).setIcon(R.drawable.search);
                break;
            case 1:
                tabLayout.getTabAt(0).setIcon(R.drawable.music);
                tabLayout.getTabAt(current).setIcon(R.drawable.history_pressed);
                tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                tabLayout.getTabAt(4).setIcon(R.drawable.search);
                break;
            case 3:
                tabLayout.getTabAt(0).setIcon(R.drawable.music);
                tabLayout.getTabAt(1).setIcon(R.drawable.history);
                tabLayout.getTabAt(current).setIcon(R.drawable.radio_pressed);
                tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                tabLayout.getTabAt(4).setIcon(R.drawable.search);
                break;
            case 2:
                tabLayout.getTabAt(0).setIcon(R.drawable.music);
                tabLayout.getTabAt(1).setIcon(R.drawable.history);
                tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                tabLayout.getTabAt(4).setIcon(R.drawable.search);
                tabLayout.getTabAt(current).setIcon(R.drawable.playlist_pressed);
                break;
            case 4:
                tabLayout.getTabAt(current).setIcon(R.drawable.search_pressed);
                tabLayout.getTabAt(0).setIcon(R.drawable.music);
                tabLayout.getTabAt(1).setIcon(R.drawable.history);
                tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        Log.d(TAG, "Edit pager: " + pager.getCurrentItem());
        switch (pager.getCurrentItem())
        {
            case 1:
                if(historyFragment.adapter != null && historyFragment.adapter.getState())
                {
                    if(historyFragment.adapter.getMoved())
                        refreshSuggestions(historyFragment.musicList, true);

                    historyFragment.resetAdapter();
                    fm.popBackStack();
                }
                break;
            case 2:
                    if(defaultPlaylistFragment.playlistFragment != null)
                    {

                        if(defaultPlaylistFragment.playlistFragment.playlistAdapter.getEdit()) {
                            fm.popBackStack();
                            defaultPlaylistFragment.playlistFragment.playlistAdapter.setEdit(false);
                        }else if(defaultPlaylistFragment.playlistFragment.playlistTableFragment != null) {
                            if(defaultPlaylistFragment.playlistFragment.playlistTableFragment.videoAdapter != null)
                            {
                                if(defaultPlaylistFragment.playlistFragment.playlistTableFragment.videoAdapter.getState())
                                    defaultPlaylistFragment.playlistFragment.playlistTableFragment.resetAdapter();
                                fm.popBackStack();
                            }
                        } else {
                            pager.setCurrentItem(1, true);
                        }
                        Log.d(TAG, "Edit: " + defaultPlaylistFragment.playlistFragment.playlistAdapter.getEdit());

                    }
                break;
            case 3:
                if(radioFragment.radioAdapter != null &&
                        radioFragment.radioAdapter.getFirstList() == RadioAdapter.List.STATIONS)
                {
                    radioFragment.radioAdapter.setListCountry();
                    fm.popBackStack();
                }
                else if(radioFragment.radioAdapter != null &&
                        radioFragment.radioAdapter.getFirstList() == RadioAdapter.List.COUNTRIES)
                {
                    radioFragment.radioAdapter.setHistoryList();
                    radioFragment.searchCountry.setVisibility(View.VISIBLE);
                    radioFragment.offsetRecyclerView();
                    fm.popBackStack();
                }
                else
                {
                    pager.setCurrentItem(1, true);
                }
                break;
                default:
                    pager.setCurrentItem(1, true);
                    break;
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {

        FragmentManager fm = getSupportFragmentManager();
        int current = pager.getCurrentItem();
        Log.d("PlayFragKey", " ON KEY DOWN Current: " + current);

        if(playFragment.isSlided() && keyCode == KeyEvent.KEYCODE_BACK && current == 0)
        {
            playFragment.slide();
            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_BACK && current == 1 && !historyFragment.adapter.getState())
        {
            if(historyFragment.getSearchView().isActionViewExpanded())
            {
                historyFragment.getSearchView().collapseActionView();
                return true;
            }
            moveTaskToBack(true);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && fm.getBackStackEntryCount() != 0)
        {
            Log.d("PlayFragKey", "OnbackPressed");
            onBackPressed();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            pager.setCurrentItem(1, true);
            return true;

        }
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            playFragment.setVolume(keyCode);
            return true;
        }
        return false;
    }

    @Override
    public void stream(Station station, ArrayList<Station> stations)
    {
        audioPlayer.setStream(true);
        playFragment.refreshList(stations);
        playFragment.setStream(station, stations);
    }


    public void refresh(Music pjesma)
    {
        historyFragment.addToList(pjesma);
    }

    @Override
    public void refreshPlaylist()
    {
        if(defaultPlaylistFragment.playlistFragment != null
                && defaultPlaylistFragment.playlistFragment.playlistTableFragment != null)
            defaultPlaylistFragment.playlistFragment.playlistTableFragment.refreshAdapter();
    }

    // koristiti Music#getPath
    @Override
    public void setMusic(Music pjesma) {
        Intent intent = new Intent(this, AudioService.class);
        audioPlayer.setStream(false);
        intent.putExtra(AudioService.SONG, pjesma);
        intent.putExtra(AudioService.ACTION, 6);
        startService(intent);
    }

    // funkcija callback se poziva klikom na notifikaciju.
    @Override
    public void callback(String callback) {
        Log.d(TAG, callback);
        switch (callback)
        {
            case Constants.NEXT:
                playFragment.nextSong();
                break;
            case Constants.PREVIOUS:
                playFragment.previousSong();
                break;
            case Constants.PLAY_PAUSE:
                playFragment.playPauseSong(false);
                break;
            case Constants.AD:
                searchFragment.ifInternetConnection();
                break;
            case Constants.EXIT:
                audioService.stopForeground(true);
                this.finishAffinity();
                break;
        }
    }

    // Kada se napravi lista u playlist fragmenut ova se funkcija zove.

    @Override
    public void onThemeChanged()
    {
        Resources.Theme theme = super.getTheme();
        if(ThemeManager.getDebug().equals(ThemeManager.DARK_THEME))
            theme.applyStyle(R.style.BlackTheme, true);
        else
            theme.applyStyle(R.style.LightTheme, true);

        if(historyFragment != null)
        {
            historyFragment.setupActionBar();
            historyFragment.refreshFragment();
            defaultPlaylistFragment.setupActionBar();
            defaultPlaylistFragment.refreshFragment();
            searchFragment.refreshFragment();
            radioFragment.setupActionBar();
            radioFragment.refreshFragment();
            tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));
        }

    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
                case 0:
                    return playFragment;
                case 1:
                    return historyFragment;
                case 2:
                    return defaultPlaylistFragment;
                case 3:
                    return radioFragment;
                case 4:
                    return searchFragment;
            }
            return null;
        }


        @Override
        public int getCount()
        {
            return 5;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }

}
