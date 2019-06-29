package com.hfad.youplay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.hfad.youplay.R;
import com.hfad.youplay.Ilisteners.OnMusicSelected;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.utils.ThemeManager;

import java.util.List;

/**
 * Created by Stjepan on 29.12.2017..
 */

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener
{

    private Context context;
    private int resource;
    private List<Music> data;
    private OnMusicSelected listener;
    private int lastPosition = -1;

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
    public void onViewAttachedToWindow(ViewHolder holder) {
        holder.setIsRecyclable(false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);
        view.setOnLongClickListener(this);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Music list = data.get(position);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(list.getViews()).append(" ").append(context.getResources().getString(R.string.you_view));
        holder.title.setText(list.getTitle());
        holder.author.setText(list.getAuthor());
        holder.view.setText(stringBuilder);
        holder.duration.setText(list.getDuration());

        if(list.getDownloaded() == 1)
            holder.downloaded.setText(R.string.you_downloaded);
        else
            holder.downloaded.setText("");

        Glide.with(context).load(list.getUrlImage()).apply(new RequestOptions().override(120, 90)).into(holder.image);

        holder.info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onInfoClicked(holder.getAdapterPosition(), view);
                }
            }
        });

        setAnimation(holder.image, position);

        holder.itemView.setTag(list);
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

    private void setAnimation(View animate, int position)
    {
        if(position > lastPosition)
        {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            animate.startAnimation(animation);
            lastPosition = position;
        }
    }

    public void setData(List<Music> data)
    {
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

        ViewHolder(View v)
        {
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
}
