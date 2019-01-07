package com.hfad.youplay.fragments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hfad.youplay.Ilisteners.OnItemClicked;
import com.hfad.youplay.Ilisteners.OnRadioSelected;
import com.hfad.youplay.MainActivity;
import com.hfad.youplay.R;
import com.hfad.youplay.adapter.RadioAdapter;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.radio.Browser;
import com.hfad.youplay.radio.Country;
import com.hfad.youplay.radio.RadioBrowser;
import com.hfad.youplay.radio.Station;
import com.hfad.youplay.radio.StationBrowser;
import com.hfad.youplay.utils.ThemeManager;

import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;

import static com.hfad.youplay.utils.Constants.DIALOG_NOW_PLAYING;
import static com.hfad.youplay.utils.Constants.DIALOG_TABLE_DELETE;


/**
 * A simple {@link Fragment} subclass.
 */
public class RadioFragment extends BaseFragment implements OnRadioSelected, View.OnClickListener{

    private final static String TAG = RadioFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    public AppBarLayout divideLayout;
    private TextView connection;
    private RadioBrowser browser;
    private StationBrowser stationBrowser;
    public TextView searchCountry;
    private LinearLayout linearLayout;
    public RadioAdapter radioAdapter;
    private ArrayList<Country> countriesList = new ArrayList<>();
    private ArrayList<Station> stationsList = new ArrayList<>();
    private ArrayList<Station> history = new ArrayList<>();
    private ActionBar actionBar;
    private DividerItemDecoration dividerItemDecoration;
    private OnItemClicked onItemClicked;

    public RadioFragment() {
        // Required empty public constructor
    }

    @Override
    public void buildAlertDialog(int position, View view)
    {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onItemClicked = (OnItemClicked) getActivity();
    }

    public void refreshFragment()
    {
        if(getActivity() != null)
            getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commitAllowingStateLoss();
    }

    @Override
    public void setupActionBar()
    {
        if(actionBar != null)
        {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);

            View customToolbar = LayoutInflater.from(getContext()).inflate(R.layout.toolbar_layout, null);
            actionBar.setCustomView(customToolbar);

            if(getView() != null)
            {
                TextView textView = getView().findViewById(R.id.toolbar_title);
                textView.setTextColor(getResources().getColor(ThemeManager.getFontTheme()));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_radio, container, false);

        divideLayout = view.findViewById(R.id.app_layout);
        recyclerView = view.findViewById(R.id.radio_view);
        connection = view.findViewById(R.id.internet_connection);
        searchCountry = view.findViewById(R.id.search_country);
        progressBar = view.findViewById(R.id.radio_loading_bar);
        linearLayout= view.findViewById(R.id.radio_bar_layout);
        searchCountry.setOnClickListener(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.getRecycledViewPool().clear();
        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        setupAdapter();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        setupActionBar();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
        recyclerView.removeItemDecoration(dividerItemDecoration);
        recyclerView.addItemDecoration(dividerItemDecoration);

//        searchCountry.setTextColor(getResources().getColor(ThemeManager.getFontTheme()));
        linearLayout.setBackgroundColor(getResources().getColor(ThemeManager.getLineSeperatorTheme()));
//        historyBar.setTextColor(getResources().getColor(ThemeManager.getFontTheme()));
//        connection.setTextColor(getResources().getColor(ThemeManager.getFontTheme()));

        if(getView() != null)
            getView().setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

        if(history.isEmpty())
            refreshList();

    }

    @Override
    public void initAudioService() {

    }

    public void setupAdapter()
    {
        radioAdapter = new RadioAdapter(getContext(), history, RadioAdapter.List.HISTORY_LIST);
        recyclerView.setAdapter(radioAdapter);
        radioAdapter.setListener(this);
    }

    private void refreshList()
    {
        if(EasyPermissions.hasPermissions(getContext(), MainActivity.PERMISSIONS))
        {
            YouPlayDatabase youPlayDatabase = YouPlayDatabase.getInstance();
            history.clear();
            history.addAll(youPlayDatabase.getRadios());
            radioAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View view)
    {
        int id = view.getId();
        if(id == R.id.search_country)
        {
            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            extractIfInternet();
        }
    }

    public void extractIfInternet()
    {
        if(internetConnection())
        {
            searchCountry.setVisibility(View.GONE);
            countryExtract();
        }
        else
            Toast.makeText(getContext(), getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
    }

    private boolean internetConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private void countryExtract()
    {

//        connection.setVisibility(View.GONE);
        if(browser == null)
        {
            browser = new RadioBrowser(RadioBrowser.ListType.COUNTRIES);
            browser.setListener(new Browser.Listener() {
                @Override
                public void postExecute(ArrayList<Country> countries) {
                    recyclerView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    countriesList.clear();
                    countriesList.addAll(countries);
                    radioAdapter.setListCountry(countries);
                }

                @Override
                public void preExecute() {
                    recyclerView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void getPostExecute(ArrayList<Station> stations) {

                }
            });
            browser.execute();
        }
        else
        {
            radioAdapter.setListCountry(countriesList);
        }

    }
    
    private void stationExtract(String country)
    {
        if(stationBrowser == null)
        {
            stationBrowser = new StationBrowser(StationBrowser.ListType.STATIONS, country);
            stationBrowser.setListener(new Browser.Listener() {
                @Override
                public void postExecute(ArrayList<Country> countries) {

                }

                @Override
                public void preExecute() {
                    recyclerView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void getPostExecute(ArrayList<Station> stations) {
                    recyclerView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    stationsList.clear();
                    stationsList.addAll(stations);
                    radioAdapter.setListStation(stations);
                }
            });
            stationBrowser.execute();
        }
        else if(!stationBrowser.getCountry().equals(country))
        {
            stationBrowser = new StationBrowser(StationBrowser.ListType.STATIONS, country);
            stationBrowser.setListener(new Browser.Listener() {
                @Override
                public void postExecute(ArrayList<Country> countries) {

                }

                @Override
                public void preExecute() {
                    recyclerView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void getPostExecute(ArrayList<Station> stations) {
                    recyclerView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    stationsList.addAll(stations);
                    radioAdapter.setListStation(stations);
                }
            });
            stationBrowser.execute();
        }
        else
        {
            radioAdapter.setListStation(stationsList);
        }
    }

    @Override
    public void onClickCountry(Country country, View v)
    {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack("Radio_Country");
        fragmentTransaction.commit();
        if(internetConnection())
            stationExtract(country.getName());
        else
            Toast.makeText(getContext(), getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClickStation(Station station, View v)
    {
        if(radioAdapter.getFirstList() == RadioAdapter.List.STATIONS)
            radioAdapter.addRadio(station);
        refreshList();

        onItemClicked.stream(station, history);
    }

    @Override
    public void onInfoClicked(Station station)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), ThemeManager.getDialogTheme());
        builder.setTitle(station.getName())
                .setItems(R.array.you_radio_dialog, (dialogInterface, i) ->
                {
                   if(i == DIALOG_NOW_PLAYING)
                       onItemClicked.stream(station, stationsList);
                   else if(i == DIALOG_TABLE_DELETE)
                   {
                       radioAdapter.deleteRadio(station);
                       Snackbar.make(getView(), getResources().getString(R.string.radio_deleted), Snackbar.LENGTH_SHORT).show();
                   }
                });

        builder.create().show();
    }
}
