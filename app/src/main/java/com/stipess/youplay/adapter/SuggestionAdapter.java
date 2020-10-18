package com.stipess.youplay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.stipess.youplay.Ilisteners.OnSuggestionSelected;
import com.stipess.youplay.R;

import java.util.List;

/**
 * Created by Stjepan Stjepanovic on 17.3.2018..
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * SuggestionAdapter.java is part of YouPlay.
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
public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> implements View.OnClickListener
{

    private Context context;
    private int resource;
    private OnSuggestionSelected listener;
    private List<String> query;

    public SuggestionAdapter(Context context, int resource, List<String> query)
    {
        this.context = context;
        this.resource = resource;
        this.query = query;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(resource, parent ,false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        final String Q = query.get(position);
        holder.suggestion.setText(Q);

        holder.autoQuery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onAutoClick(Q);
                }
            }
        });

        holder.itemView.setTag(Q);
    }

    @Override
    public int getItemCount() {
        return query.size();
    }

    public void setListener(OnSuggestionSelected listener)
    {
        this.listener = listener;
    }

    @Override
    public void onClick(View view)
    {
        if(listener != null)
        {
            String query = (String) view.getTag();
            listener.onClick(query);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        private TextView suggestion;
        private TextView autoQuery;

        ViewHolder(View v)
        {
            super(v);
            suggestion = v.findViewById(R.id.you_suggestions);
            autoQuery  = v.findViewById(R.id.auto_query);
        }
    }
}
