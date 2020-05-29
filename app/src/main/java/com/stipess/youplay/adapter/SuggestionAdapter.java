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
 * Created by Stjepan on 17.3.2018..
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
