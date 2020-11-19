package com.stipess.youplay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.stipess.youplay.R;
import com.stipess.youplay.Ilisteners.OnMusicSelected;
import com.stipess.youplay.music.Music;

import java.util.List;

/**
 * Created by Stjepan Stjepanovic on 29.12.2017..
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * SearchAdapter.java is part of YouPlay.
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
public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener
{

    private Context context;
    private int resource;
    private List<Music> data;
    private OnMusicSelected listener;
    private int lastPosition = -1;
    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;

    public SearchAdapter(Context context, int resource, List<Music> music)
    {
        this.context = context;
        data = music;
        this.resource = resource;
    }

    public void setListener(OnMusicSelected listener)
    {
        this.listener = listener;
    }


    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        holder.setIsRecyclable(false);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);
            view.setOnLongClickListener(this);
            view.setOnClickListener(this);
            return new ViewHolder(view);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_item, parent, false);
            return new ProgressViewHolder(v);
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ViewHolder) {
            Music list = data.get(position);
            StringBuilder stringBuilder = new StringBuilder();
            if(list.getTimeAgo() == null)
                list.setTimeAgo("");
            stringBuilder.append(list.getTimeAgo()).append(" • ").append(list.getViewsSearch()).append(" ").append(context.getResources().getString(R.string.you_view));
            ((ViewHolder)holder).title.setText(list.getTitle());
            ((ViewHolder)holder).author.setText(list.getAuthor());
            ((ViewHolder)holder).view.setText(stringBuilder);
            ((ViewHolder)holder).duration.setText(list.getDuration());

            if(list.getDownloaded() == 1)
                ((ViewHolder)holder).downloaded.setText(R.string.you_downloaded);
            else
                ((ViewHolder)holder).downloaded.setText("");

            Glide.with(context).load(list.getUrlImage()).apply(new RequestOptions().override(120, 90)).into(((ViewHolder)holder).image);

            ((ViewHolder)holder).info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onInfoClicked(holder.getAdapterPosition(), view);
                    }
                }
            });

            setAnimation(((ViewHolder)holder).itemView, position);

            holder.itemView.setTag(list);
        } else {
            ((ProgressViewHolder) holder).bar.setIndeterminate(true);
            setAnimation(((ProgressViewHolder)holder).itemView, position);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if(listener != null)
        {
            Music pjesma = (Music) view.getTag();
            listener.onLongClick(pjesma, view);
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if(listener != null)
        {
            Music pjesma = (Music) view.getTag();
            listener.onClick(pjesma, view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position) != null ? VIEW_ITEM : VIEW_PROG;
    }

    private void setAnimation(View animate, int position){
        if(position > lastPosition)
        {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down);
            animate.startAnimation(animation);
            lastPosition = position;
        }
    }

    public void setData(List<Music> data){
        this.data.clear();
        this.data.addAll(data);
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        private TextView title;
        private TextView author;
        private TextView duration;
        private TextView view;
        private ImageView image;
        private ImageView info;
        private TextView downloaded;

        ViewHolder(View v){
            super(v);
            title      = v.findViewById(R.id.title);
            author     = v.findViewById(R.id.author);
            duration   = v.findViewById(R.id.duration);
            view       = v.findViewById(R.id.views);
            image      = v.findViewById(R.id.image);
            downloaded = v.findViewById(R.id.downloaded);
            info       = v.findViewById(R.id.info);
        }
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        private ProgressBar bar;

        ProgressViewHolder(View v) {
            super(v);
            bar = v.findViewById(R.id.load_more);
        }
    }
}
