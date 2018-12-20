package com.hfad.youplay;

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
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hfad.youplay.Ilisteners.OnItemClicked;
import com.hfad.youplay.Ilisteners.OnThemeChanged;
import com.hfad.youplay.adapter.RadioAdapter;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.fragments.DefaultPlaylistFragment;
import com.hfad.youplay.fragments.HistoryFragment;
import com.hfad.youplay.fragments.PlayFragment;
import com.hfad.youplay.fragments.SearchFragment;
import com.hfad.youplay.fragments.RadioFragment;
import com.hfad.youplay.fragments.SettingsFragment;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.utils.Constants;
import com.hfad.youplay.radio.Station;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.ThemeManager;
import com.hfad.youplay.utils.Utils;
import com.hfad.youplay.web.YouPlayWeb;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener1;
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;



public class MainActivity extends AppCompatActivity implements AudioService.ServiceCallback
        , EasyPermissions.PermissionCallbacks, OnThemeChanged, OnItemClicked {

    private static final String TAG = MainActivity.class.getSimpleName();
    // kada je true daj link od verzije bez reklama, ostalo daj sa reklamom.
    public static final boolean noAdApp = true;

    private static final String EMAIL = "stjepstjepanovic@gmail.com";

    private TabLayout tabLayout;
    private HistoryFragment historyFragment;
    private PlayFragment playFragment;
    private SearchFragment searchFragment;
    private DefaultPlaylistFragment defaultPlaylistFragment;
    private RadioFragment radioFragment;
    private AudioService audioService;
    private Intent intent;
    private boolean bound = false;
    public ViewPager pager;
    private AdView adView;
    private YouPlayDatabase db;
    private InputMethodManager imm;
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
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE};

    private ServiceConnection connection;

    @Override
    protected void onStart() {
        intent = new Intent(getApplication(), AudioService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startService(intent);
        if(firstTime)
        {
            pager.setCurrentItem(1);
            firstTime = false;
        }

//        if(internetConnection())
//            initAds();

        super.onStart();
    }

    @Override
    protected void onPause() {
        if(imm != null && getCurrentFocus() != null)
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PreferenceManager.setDefaultValues(this, R.xml.preference, true);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean pre = preferences.getBoolean(SettingsFragment.KEY, false);
        if(pre)
            setTheme(R.style.BlackTheme);
        else
            setTheme(R.style.LightTheme);

        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        Crashlytics.setUserEmail(EMAIL);
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
                audioService.setCallback(MainActivity.this);

                Log.d(TAG, "On Service connected");
                historyFragment.initAudioService();
                playFragment.initAudioService();
                playFragment.setListeners();
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
        pager.setOffscreenPageLimit(4);

        db = new YouPlayDatabase(getApplicationContext());
        checkPermissions(PERMISSIONS);

        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(pager);
        tabLayout.setBackgroundColor(getResources().getColor(R.color.light_black));
        onThemeChanged();
        registerListeners();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(historyFragment.isAdded() && playFragment.isAdded() && radioFragment.isAdded())
        {
            getSupportFragmentManager().putFragment(outState, Constants.HISTORY_FRAGMENT, historyFragment);
            getSupportFragmentManager().putFragment(outState, Constants.PLAY_FRAGMENT, playFragment);
            getSupportFragmentManager().putFragment(outState, Constants.RADIO_FRAGMENT, radioFragment);
        }
    }

    @Override
    protected void onDestroy() {
        if(bound)
        {
            unbindService(connection);
            bound = false;
        }
//        if(intent != null)
//            stopService(intent);
        if(audioService != null)
        {
            audioService.setDestroyed(true);
            audioService.setCallback(null);
        }
        size = -1;
        OkDownload.with().downloadDispatcher().cancelAll();
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
                switch (position)
                {
                    case 0:
                        tabLayout.setBackgroundColor(getResources().getColor(R.color.play_fragment_bars));

                        tabLayout.getTabAt(position).setIcon(R.drawable.music_pressed);
                        tabLayout.getTabAt(1).setIcon(R.drawable.history);
                        tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                        tabLayout.getTabAt(4).setIcon(R.drawable.search);
                        tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                        break;
                    case 1:
                        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

                        tabLayout.getTabAt(0).setIcon(R.drawable.music);
                        tabLayout.getTabAt(1).setIcon(R.drawable.history_pressed);
                        tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                        tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                        tabLayout.getTabAt(4).setIcon(R.drawable.search);

                        break;
                    case 2:
                        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

                        tabLayout.getTabAt(0).setIcon(R.drawable.music);
                        tabLayout.getTabAt(1).setIcon(R.drawable.history);
                        tabLayout.getTabAt(4).setIcon(R.drawable.search);
                        tabLayout.getTabAt(3).setIcon(R.drawable.radio);
                        tabLayout.getTabAt(position).setIcon(R.drawable.playlist_pressed);
                        break;
                    case 3:
                        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

                        tabLayout.getTabAt(1).setIcon(R.drawable.history);
                        tabLayout.getTabAt(0).setIcon(R.drawable.music);
                        tabLayout.getTabAt(2).setIcon(R.drawable.playlist);
                        tabLayout.getTabAt(4).setIcon(R.drawable.search);
                        tabLayout.getTabAt(position).setIcon(R.drawable.radio_pressed);

                        break;
                    case 4:
                        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

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
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms)
    {
        initFiles();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        this.finishAffinity();
    }

    public void checkPermissions(String... permissions)
    {
        if(EasyPermissions.hasPermissions(this, permissions))
        {
            initFiles();

            File images = FileManager.getPictureFolder();
            if(!images.exists())
                images.mkdirs();

            File databaseFile = FileManager.getDatabaseFolder();
            if(!databaseFile.exists())
                databaseFile.mkdirs();

            // Pogledaj dali ima nova verzija YouPlay-a
            YouPlayWeb web = new YouPlayWeb();
            web.setListener(new YouPlayWeb.Listener() {
                @Override
                public void onConnected(String version) {
                    if(version != null)
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this, ThemeManager.getDialogTheme());
            builder.setTitle(getResources().getString(R.string.version_update))
                    .setMessage(getResources().getString(R.string.version_update_summary, version));
            builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                    final String[] downloadLink = new String[1];
                    if(noAdApp)
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
    }

    private void download(String link)
    {
        DownloadTask.Builder task = new DownloadTask.Builder(link, FileManager.getDownloadFolder());
        DownloadTask downloadTask = task.build();
        ProgressDialog downloadDialog = new ProgressDialog(MainActivity.this);
        downloadTask.enqueue(new DownloadListener1() {
            @Override
            public void taskStart(@NonNull DownloadTask task, @NonNull Listener1Assist.Listener1Model model) {

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
            public void retry(@NonNull DownloadTask task, @NonNull ResumeFailedCause cause) {

            }

            @Override
            public void connected(@NonNull DownloadTask task, int blockCount, long currentOffset, long totalLength) {

            }

            @Override
            public void progress(@NonNull DownloadTask task, long currentOffset, long totalLength) {
                double divide = (double) currentOffset / totalLength;
                double math = (double) downloadDialog.getMax() * divide;
                downloadDialog.setProgress((int) math);
            }

            @Override
            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, @NonNull Listener1Assist.Listener1Model model) {
                downloadDialog.dismiss();
                if(cause == EndCause.COMPLETED)
                {
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
                if(cause == EndCause.ERROR)
                    realCause.printStackTrace();


            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public void initFiles()
    {
        if(Utils.freeSpace(true) < 20)
        {
            Log.d(TAG, "NOt enough Space " + Utils.freeSpace(true));
            Toast.makeText(this, getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
            finishAffinity();
        }

        File file = FileManager.getRootPath();
        if(!file.exists())
            file.mkdirs();

        File images = FileManager.getPictureFolder();
        if(!images.exists())
            images.mkdirs();

        File databaseFile = FileManager.getDatabaseFolder();
        if(!databaseFile.exists())
            databaseFile.mkdirs();

        try
        {
            db.createPlaylistDatabase();
        }
        catch (Exception e)
        {
            Toast.makeText(this, getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
            finishAffinity();
        }

    }

    @Override
    public void onMusicClick(Music pjesma) {
        historyFragment.onClick(pjesma, null);
    }

    @Override
    public void onMusicClick(Music pjesma, List<Music> pjesme)
    {
        playFragment.setUserClick(true);
        if(playFragment.getShuffled())
            playFragment.checkShuffle();

        playFragment.setMusic(pjesma, pjesme);

    }

    @Override
    public void refreshSearchList(Music pjesma) {
        searchFragment.refreshMusicList(pjesma);
    }

    @Override
    public void setStation(Station station)
    {
        Intent intent = new Intent(this, AudioService.class);
        AudioService.getInstance().isStream(true);
        intent.putExtra(AudioService.SONG, station);
        intent.putExtra(AudioService.ACTION, Constants.PLAY_SONG);
        startService(intent);
    }

    @Override
    public void refreshSuggestions(List<Music> data, boolean queue)
    {
        playFragment.refreshList(data, queue);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(pager != null && pager.getAdapter() != null)
        {
            pager.getAdapter().notifyDataSetChanged();
        }
        putCurrentIcon();
    }

    private void initAds()
    {
        MobileAds.initialize(this, "ca-app-pub-8163593086331416~1012493107");
        adView = findViewById(R.id.ad);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener(){
            @Override
            public void onAdLoaded() {
                adView.setVisibility(View.VISIBLE);
                size = AdSize.SMART_BANNER.getHeightInPixels(getApplicationContext());

                if(playFragment.isSlided())
                {
                    ConstraintLayout layout = findViewById(R.id.play_list_layout);

                    layout.getLayoutParams().height -= size;

                    layout.setLayoutParams(layout.getLayoutParams());
                    adLoaded = true;
                }
            }

        });

    }

    private boolean internetConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private void putCurrentIcon()
    {
        for(int i = 0; i < imageId.length; i++)
        {
            tabLayout.getTabAt(i).setIcon(imageId[i]);
        }
        int current = pager.getCurrentItem();
        switch (current)
        {
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

                    if(defaultPlaylistFragment.playlistFragment != null
                            && defaultPlaylistFragment.playlistFragment.playlistTableFragment != null)
                    {
                        if(defaultPlaylistFragment.playlistFragment.playlistTableFragment.videoAdapter != null)
                        {
                            if(defaultPlaylistFragment.playlistFragment.playlistTableFragment.videoAdapter.getState())
                                defaultPlaylistFragment.playlistFragment.playlistTableFragment.resetAdapter();
                            fm.popBackStack();
                        }
                    }
                    else
                        pager.setCurrentItem(1, true);

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

        if(playFragment.isSlided())
            playFragment.slide();

        if(keyCode == KeyEvent.KEYCODE_BACK && pager.getCurrentItem() == 1 && !historyFragment.adapter.getState())
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
            onBackPressed();
            return true;
        }
        else
        {
            if(keyCode == KeyEvent.KEYCODE_BACK)
            {
                pager.setCurrentItem(1, true);
                return true;
            }
            return false;
        }

    }

    @Override
    public void stream(Station station, ArrayList<Station> stations)
    {
        audioService.isStream(true);
        playFragment.setUserClick(true);
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
        AudioService.getInstance().isStream(false);
        intent.putExtra(AudioService.SONG, pjesma);
        intent.putExtra(AudioService.ACTION, 6);
        intent.putExtra(AudioService.LIST, playFragment.getTempList());
        startService(intent);
    }

    // funkcija callback se poziva klikom na notifikaciju.
    @Override
    public void callback(String callback) {
        Log.d(TAG, callback);
        switch (callback)
        {
            case Constants.NEXT:
                playFragment.setUserClick(false);
                playFragment.nextSong();
                break;
            case Constants.PREVIOUS:
                playFragment.setUserClick(false);
                playFragment.previousSong();
                break;
            case Constants.PLAY_PAUSE:
                playFragment.playPauseSong(false);
                break;
            case Constants.AD:
//                initAds();
                searchFragment.ifInternetConnection();
                break;
            case Constants.EXIT:
                audioService.stopForeground(true);
                this.finishAffinity();
                break;
        }
    }

    // Kada otvaramo pjesmu u youtube pogledaj da li neka pjesma svira, ako da zaustavi ju
    @Override
    public void pauseSong() {
        if(audioService.exoPlayer.getPlayWhenReady())
            playFragment.playPauseSong(true);
    }

    @Override
    public void onThemeChanged()
    {
        Resources.Theme theme = super.getTheme();
        if(ThemeManager.getDebug().equals(ThemeManager.DARK_THEME))
            theme.applyStyle(R.style.BlackTheme, true);
        else
            theme.applyStyle(R.style.LightTheme, true);

        historyFragment.setupActionBar();
        historyFragment.refreshFragment();
        defaultPlaylistFragment.setupActionBar();
        defaultPlaylistFragment.refreshFragment();
        searchFragment.refreshFragment();
        radioFragment.setupActionBar();
        radioFragment.refreshFragment();
        tabLayout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

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
