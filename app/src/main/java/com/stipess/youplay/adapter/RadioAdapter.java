package com.stipess.youplay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.stipess.youplay.Ilisteners.OnRadioSelected;
import com.stipess.youplay.R;
import com.stipess.youplay.database.YouPlayDatabase;
import com.stipess.youplay.radio.Country;
import com.stipess.youplay.radio.Station;

import java.util.ArrayList;

/**
 * Created by Stjepan Stjepanovic
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * RadioAdapter.java is part of YouPlay.
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

public class RadioAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener
{
    private ArrayList<Station> radioMusics;
    private ArrayList<Country> countries;
    private ArrayList<Station> history;
    private Context context;
    private List firstList;
    private OnRadioSelected listener;
    private RecyclerView recyclerView;
    private YouPlayDatabase db;

    public RadioAdapter(Context context, ArrayList<Station> history, List firstList)
    {
        this.context = context;
        this.history = history;
        this.firstList = firstList;

        radioMusics = new ArrayList<>();
        countries = new ArrayList<>();
        db = YouPlayDatabase.getInstance(context);
    }

    public enum List{

        COUNTRIES,

        STATIONS,

        HISTORY_LIST
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void setListStation(ArrayList<Station> array)
    {
        this.firstList = List.STATIONS;

        if(radioMusics.isEmpty() || !array.equals(radioMusics))
        {
            radioMusics.clear();
            radioMusics.addAll(array);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        notifyDataSetChanged();
        recyclerView.scrollToPosition(0);
    }

    public void setListCountry(ArrayList<Country> array)
    {
        this.firstList = List.COUNTRIES;

        if(countries.isEmpty())
        {
            countries.addAll(array);
        }
        recyclerView.setLayoutManager(new GridLayoutManager(context, 3));
        notifyDataSetChanged();
    }

    public void setListCountry()
    {
        this.firstList = List.COUNTRIES;
        recyclerView.setLayoutManager(new GridLayoutManager(context, 3));
        notifyDataSetChanged();
    }

    public void setHistoryList()
    {
        this.firstList = List.HISTORY_LIST;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        notifyDataSetChanged();
    }

    public List getFirstList()
    {
        return firstList;
    }

    @Override
    public void onClick(View view)
    {
        if(listener != null && firstList == List.COUNTRIES)
        {
            Country radioMusic = (Country) view.getTag();
            listener.onClickCountry(radioMusic, view);
        }
        else if(listener != null)
        {
            Station station = (Station) view.getTag();
            listener.onClickStation(station, view);
        }
    }

    public void setListener(OnRadioSelected listener)
    {
        this.listener = listener;
    }

    public void deleteRadio(Station station)
    {
        db.deleteRadio(station);
        history.remove(station);
        notifyDataSetChanged();
    }

    public void addRadio(Station station)
    {
        if(!db.radioExists(station))
        {
            db.insertRadio(station);
            history.add(station);
            notifyDataSetChanged();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if(viewType == 0)
        {
            View view = LayoutInflater.from(context).inflate(R.layout.radio_adapter_view, parent ,false);
            view.setOnClickListener(this);
            return new ViewHolder(view);
        }
        else if(viewType == 1)
        {
            View view = LayoutInflater.from(context).inflate(R.layout.radio_country_view, parent ,false);
            view.setOnClickListener(this);
            return new ViewHolderCountry(view);
        }
            View view = LayoutInflater.from(context).inflate(R.layout.radio_adapter_view, parent ,false);
            view.setOnClickListener(this);
            return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if(firstList == List.STATIONS)
        {
            Station station = radioMusics.get(position);
            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.name.setText(station.getName());
            String bitrate = station.getBitRate() + " " + context.getResources().getString(R.string.radio_bitrate);
            viewHolder.bitRate.setText(bitrate);
            viewHolder.country.setText(station.getCountry());
            viewHolder.info.setVisibility(View.GONE);

            if(!station.getIcon().equals(""))
                Glide.with(context).load(station.getIcon()).apply(new RequestOptions().error(R.drawable.image_holder)).into(viewHolder.image);

            viewHolder.itemView.setTag(station);
        }
        else if(firstList == List.HISTORY_LIST)
        {
            final Station station = history.get(position);
            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.name.setText(station.getName());
            String bitrate = station.getBitRate() + " " + context.getResources().getString(R.string.radio_bitrate);
            viewHolder.bitRate.setText(bitrate);
            viewHolder.country.setText(station.getCountry());
            viewHolder.info.setVisibility(View.VISIBLE);

            viewHolder.info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onInfoClicked(station);
                    }
                }
            });

            viewHolder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if(!station.getIcon().isEmpty())
                Glide.with(context).load(station.getIcon()).apply(new RequestOptions().error(R.drawable.image_holder)).into(viewHolder.image);
            else
                Glide.with(context).load(R.drawable.image_holder).into(viewHolder.image);

            viewHolder.itemView.setTag(station);
        }
        else if(firstList == List.COUNTRIES)
        {
            Country country = countries.get(position);
            ViewHolderCountry viewHolderCountry = (ViewHolderCountry) holder;
            viewHolderCountry.country.setText(country.getName());
            String count = context.getResources().getString(R.string.radio_stations) + country.getStationCount();
            viewHolderCountry.stationCount.setText(count);
            viewHolderCountry.itemView.setTag(country);
        }
    }

    @Override
    public int getItemViewType(int position) {

        if(firstList == List.COUNTRIES)
            return 1;
        else if(firstList == List.HISTORY_LIST)
            return 2;

        return 0;
    }

    @Override
    public int getItemCount() {
        if(firstList == List.STATIONS)
            return radioMusics.size();
        else if(firstList == List.HISTORY_LIST)
            return history.size();

        return countries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        private TextView name;
        private TextView bitRate;
        private TextView country;
        private ImageView image;
        private ImageView info;

        ViewHolder(View v)
        {
            super(v);
            name = v.findViewById(R.id.name);
            bitRate = v.findViewById(R.id.bitrate);
            country = v.findViewById(R.id.country);
            image = v.findViewById(R.id.imageView);
            info = v.findViewById(R.id.radio_info);
        }
    }

    static class ViewHolderCountry extends RecyclerView.ViewHolder
    {
        private TextView country;
        private TextView stationCount;

        ViewHolderCountry(View v)
        {
            super(v);
            country = v.findViewById(R.id.country_name);
            stationCount = v.findViewById(R.id.station_count);
        }
    }
}
