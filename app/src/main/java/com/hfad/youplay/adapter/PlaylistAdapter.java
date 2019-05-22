package com.hfad.youplay.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.GenericTransitionOptions;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.hfad.youplay.Ilisteners.OnMusicSelected;
import com.hfad.youplay.Ilisteners.OnPlaylistSelected;
import com.hfad.youplay.Ilisteners.OnRadioSelected;
import com.hfad.youplay.R;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.fragments.PlayFragment;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.radio.Station;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Stjepan on 6.2.2018..
 */

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener
{

    private Context context;
    private int resource;
    private OnPlaylistSelected listener;
    private OnMusicSelected song_listener;
    private OnRadioSelected onRadioSelected;
    private OnSwipeListener onswipeListener;
    private YouPlayDatabase db;
    private List<Music> list;
    private List<String> playlists = new ArrayList<>();
    private List<Station> stations = new ArrayList<>();
    private ListType play;
    private int position = -1;
    private int lastPos;

    public PlaylistAdapter(Context context, int resource, List<Music> list, ListType play)
    {
        this.context   = context;
        this.resource  = resource;
        this.list      = list;
        this.play      = play;
    }

    public interface OnSwipeListener{
        void onSwipe(int position);
    }

    public void setStations(List<Station> stations)
    {
        play = ListType.STATIONS;

        this.stations.clear();
        this.stations.addAll(stations);

        notifyDataSetChanged();
    }

    public void setPlay(ListType play) {
        this.play = play;
        this.notifyDataSetChanged();
    }

    public void setPlaylists(List<String> playlists)
    {
        play = ListType.PLAYLIST_TABLE;

        this.playlists.clear();
        this.playlists.addAll(playlists);

        notifyDataSetChanged();
    }

    private void setMusics()
    {
        play = ListType.SUGGESTIONS;
        this.notifyDataSetChanged();
    }

    public void removePlaylistSong(int position)
    {
        playlists.remove(position);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(resource, null ,false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);

        db = YouPlayDatabase.getInstance(context);
        return new ViewHolder(view);
    }

    public ListType getPlay() {
        return play;
    }

    // Ovo je najbolje napravit preko get view type da netrebamo posebno postavljat listener
    public enum ListType
    {
        STATIONS,

        SUGGESTIONS,

        PLAYLIST_TABLE
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback(){

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Nezelimo swipe-at pjesmu koja trenutno svira
                if(viewHolder.getAdapterPosition() == position || play == ListType.PLAYLIST_TABLE || play == ListType.STATIONS)
                    return makeMovementFlags(0,0);
                return makeMovementFlags(0,
                        ItemTouchHelper.RIGHT);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                int adapterPos = viewHolder.getAdapterPosition();
                if(play != ListType.STATIONS)
                {
                    list.remove(adapterPos);
                    notifyItemRemoved(adapterPos);
                    notifyItemRangeChanged(adapterPos, list.size());
                    onswipeListener.onSwipe(list.indexOf(PlayFragment.currentlyPlayingSong));
                }
            }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    public void setListner(OnPlaylistSelected listner)
    {
        this.listener = listner;
    }

    public List<Music> getList()
    {
        return list;
    }

    public void setListener(OnMusicSelected listener, OnRadioSelected onRadioSelected)
    {
        this.song_listener = listener;
        this.onRadioSelected = onRadioSelected;
    }

    public void setOnSwipeListener(OnSwipeListener onSwipeListener)
    {
        this.onswipeListener = onSwipeListener;
    }

    public void setCurrent(int position)
    {
        this.position = position;
    }

    public void setLastCurrent(int lastPos)
    {
        this.lastPos = lastPos;
    }

    public int getLastPos() {
        return lastPos;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position)
    {

        // Ako su pjesme dobivene iz searchfragmenta (suggestions).
        if(play == ListType.SUGGESTIONS)
        {
            Music pjesma = list.get(position);
            holder.title.setText(pjesma.getTitle());
            if(this.position != position)
            {
                holder.title.setTextColor(context.getResources().getColor(R.color.suggestions));
                holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.black));
                holder.title.setEllipsize(TextUtils.TruncateAt.END);
                holder.title.setSelected(false);
                if(pjesma.getDownloaded() == 1)
                    holder.duration.setTextColor(context.getResources().getColor(R.color.suggestions));
                else
                    holder.duration.setTextColor(context.getResources().getColor(R.color.grey));
            }
            else
            {
                holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.lighter_black));
                holder.title.setTextColor(context.getResources().getColor(R.color.seekbar_progress));
                holder.title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                holder.title.setSelected(true);
                holder.title.setMarqueeRepeatLimit(-1);
                holder.title.setSingleLine(true);
                holder.duration.setTextColor(context.getResources().getColor(R.color.seekbar_progress));
            }

            if(pjesma.getDownloaded() == 0 && !pjesma.equals(PlayFragment.currentlyPlayingSong))
                holder.title.setTextColor(context.getResources().getColor(R.color.grey));

            holder.duration.setText(pjesma.getDuration());

            if(FileManager.getPictureFile(pjesma.getId()).exists())
                Glide.with(context).load(FileManager.getPictureFile(pjesma.getId())).thumbnail(0.1f).apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(false).override(120, 90).dontAnimate()).into(holder.image);
            else
                Glide.with(context).load(pjesma.getUrlImage()).apply(new RequestOptions().skipMemoryCache(true)).into(holder.image);


            holder.itemView.setTag(pjesma);
        }
        else if(play == ListType.PLAYLIST_TABLE)
        {
            final String title = playlists.get(position);
            // vidjet jeli je table updatean sa id od slike!
            String id = db.getPicTable(title);


            if(id != null)
                Glide.with(context).load(FileManager.getPictureFile(id)).apply(new RequestOptions().override(80,120)).into(holder.image);
            else
                Glide.with(context).load(R.mipmap.ic_launcher).into(holder.image);


            holder.title.setText(title);
//            holder.title.setTextColor(context.getResources().getColor(ThemeManager.getFontTheme()));
//            holder.itemView.setBackgroundColor(context.getResources().getColor(ThemeManager.getTheme()));


            holder.info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onInfoClicked(title, view);
                    }
                }
            });

            holder.itemView.setTag(title);
        }
        else
        {
            Station station = stations.get(position);
            holder.title.setText(station.getName());
            if(!station.getIcon().isEmpty())
                Glide.with(context).load(station.getIcon()).apply(new RequestOptions().error(R.mipmap.ic_launcher).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)).into(holder.image);
            else
                Glide.with(context).load(R.mipmap.ic_launcher).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)).into(holder.image);


            if(this.position != position)
            {
                holder.title.setTextColor(context.getResources().getColor(R.color.suggestions));
                holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.black));
                holder.duration.setTextColor(context.getResources().getColor(R.color.suggestions));
            }
            else
            {
                holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.lighter_black));
                holder.title.setTextColor(context.getResources().getColor(R.color.seekbar_progress));
                holder.duration.setTextColor(context.getResources().getColor(R.color.seekbar_progress));
            }

            holder.itemView.setTag(station);
        }
    }

    @Override
    public int getItemViewType(int position)
    {
        if(play == ListType.SUGGESTIONS)
            return 1;
        else if(play == ListType.STATIONS)
            return 2;
        return 3;
    }

    public void reloadList(List<Music> pjesme)
    {
        if(pjesme != null)
        {
            List<Music> local = new ArrayList<>(pjesme);
            list.clear();
            list.addAll(local);
            setMusics();
        }
    }

    @Override
    public int getItemCount() {
        if(play == ListType.PLAYLIST_TABLE)
            return playlists.size();
        else if(play == ListType.SUGGESTIONS)
            return list.size();

        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        private ImageView image;
        private TextView title;
        private TextView duration;
        private ImageView info;

        ViewHolder(View v)
        {
            super(v);
            image    = v.findViewById(R.id.playlist_image);
            title    = v.findViewById(R.id.playlist_title);
            duration = v.findViewById(R.id.playlist_duration);
            info     = v.findViewById(R.id.playlist_info);
        }
    }

    @Override
    public void onClick(View view) {
        if(listener != null && play == ListType.PLAYLIST_TABLE)
        {
            String title = (String) view.getTag();
            listener.onClick(title, view);
        }
        else if(song_listener != null && play == ListType.SUGGESTIONS)
        {
            Music pjesma = (Music) view.getTag();
            song_listener.onClick(pjesma, view);
        }
        else if(onRadioSelected != null && play == ListType.STATIONS)
        {
            Station station = (Station) view.getTag();
            onRadioSelected.onClickStation(station, view);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if(listener != null)
        {
            String title = (String) view.getTag();
            listener.onLongClick(title, view);
            return true;
        }
        else if(song_listener != null && play != ListType.STATIONS)
        {
            Music pjesma = (Music) view.getTag();
            song_listener.onLongClick(pjesma , view);
            return true;
        }
        return false;
    }
}
