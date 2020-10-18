package com.stipess.youplay.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.stipess.youplay.AudioService;
import com.stipess.youplay.Ilisteners.OnItemClicked;
import com.stipess.youplay.Ilisteners.OnRadioSelected;
import com.stipess.youplay.MainActivity;
import com.stipess.youplay.R;
import com.stipess.youplay.adapter.RadioAdapter;
import com.stipess.youplay.database.YouPlayDatabase;
import com.stipess.youplay.radio.Browser;
import com.stipess.youplay.radio.Country;
import com.stipess.youplay.radio.RadioBrowser;
import com.stipess.youplay.radio.Station;
import com.stipess.youplay.radio.StationBrowser;
import com.stipess.youplay.utils.ThemeManager;

import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;

import static com.stipess.youplay.utils.Constants.DIALOG_NOW_PLAYING;
import static com.stipess.youplay.utils.Constants.DIALOG_TABLE_DELETE;


/**
 * Created by Stjepan Stjepanovic
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * RadioFragment.java is part of YouPlay.
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
public class RadioFragment extends BaseFragment implements OnRadioSelected, View.OnClickListener{

    private final static String TAG = RadioFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private RadioBrowser browser;
    private StationBrowser stationBrowser;
    public TextView searchCountry;
    public RadioAdapter radioAdapter;
    private ArrayList<Country> countriesList = new ArrayList<>();
    private ArrayList<Station> stationsList = new ArrayList<>();
    private ArrayList<Station> history = new ArrayList<>();
    private ActionBar actionBar;
    private DividerItemDecoration dividerItemDecoration;
    private OnItemClicked onItemClicked;
    private boolean offset;

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
        Log.d(TAG, "Refresh");
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
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_radio, container, false);

        AppBarLayout divideLayout = view.findViewById(R.id.app_layout);
        recyclerView = view.findViewById(R.id.radio_view);
        TextView connection = view.findViewById(R.id.internet_connection);
        searchCountry = view.findViewById(R.id.search_country);
        progressBar = view.findViewById(R.id.radio_loading_bar);
        LinearLayout linearLayout = view.findViewById(R.id.radio_bar_layout);
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
            offsetRecyclerView();
            countryExtract();
        }
        else
            Toast.makeText(getContext(), getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
    }

    public void offsetRecyclerView()
    {
        if(getView() != null && !offset)
        {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();

            params.topToBottom = R.id.bar_layout;
            recyclerView.setLayoutParams(params);

            offset = true;
        }
        else if(getView() != null && offset)
        {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();

            params.topToBottom = R.id.app_layout;
            recyclerView.setLayoutParams(params);

            offset = false;
        }
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
            stationExtract(country.getCountryCode());
        else
            Toast.makeText(getContext(), getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClickStation(Station station, View v)
    {
        AudioService.getInstance().getAudioPlayer().setStationList(history);
        if(radioAdapter.getFirstList() == RadioAdapter.List.STATIONS)
            radioAdapter.addRadio(station);
        refreshList();

        onItemClicked.stream(station, history);
    }

    @Override
    public void onInfoClicked(final Station station)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(station.getName())
                .setItems(R.array.you_radio_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == DIALOG_NOW_PLAYING)
                            onItemClicked.stream(station, stationsList);
                        else if (i == DIALOG_TABLE_DELETE) {
                            radioAdapter.deleteRadio(station);
                            Snackbar.make(getView(), RadioFragment.this.getResources().getString(R.string.radio_deleted), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });

        builder.create().show();
    }
}
