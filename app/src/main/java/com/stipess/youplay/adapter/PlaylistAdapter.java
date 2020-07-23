package com.stipess.youplay.adapter;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.stipess.youplay.Ilisteners.OnMusicSelected;
import com.stipess.youplay.Ilisteners.OnPlaylistSelected;
import com.stipess.youplay.Ilisteners.OnRadioSelected;
import com.stipess.youplay.R;
import com.stipess.youplay.database.YouPlayDatabase;
import com.stipess.youplay.fragments.PlayFragment;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.radio.Station;
import com.stipess.youplay.utils.FileManager;

import org.schabi.newpipe.extractor.comments.CommentsInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.stipess.youplay.utils.Constants.AUTHOR;
import static com.stipess.youplay.utils.Constants.DOWNLOADED;
import static com.stipess.youplay.utils.Constants.DURATION;
import static com.stipess.youplay.utils.Constants.ID;
import static com.stipess.youplay.utils.Constants.TITLE;
import static com.stipess.youplay.utils.Constants.VIEWS;


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
    private boolean isEdit = false;
    private ItemTouchHelper touchHelper;
    private boolean moved = false;
    private YouPlayDatabase youPlayDatabase;

    public PlaylistAdapter(Context context, int resource, List<Music> list, ListType play)
    {
        this.context   = context;
        this.resource  = resource;
        this.list      = list;
        this.play      = play;
        youPlayDatabase = YouPlayDatabase.getInstance(context);
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

        this.playlists = playlists;

        notifyDataSetChanged();
    }

    private void setMusics()
    {
        play = ListType.SUGGESTIONS;
        this.notifyDataSetChanged();
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

        PLAYLIST_TABLE,
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback(){

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Nezelimo swipe-at pjesmu koja trenutno svira
                if(play == ListType.PLAYLIST_TABLE && isEdit) {
                    return makeMovementFlags(ItemTouchHelper.DOWN | ItemTouchHelper.UP, 0);
                }
                if(viewHolder.getAdapterPosition() == position || play == ListType.STATIONS)
                    return makeMovementFlags(0,0);
                return makeMovementFlags(0,
                        ItemTouchHelper.RIGHT);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if(play == ListType.PLAYLIST_TABLE) {
                    moved = true;
                    int adapterPos = viewHolder.getAdapterPosition();
                    int targetPos = target.getAdapterPosition();
                    if(playlists.size() > 1)
                    {
                        Collections.swap(playlists, viewHolder.getAdapterPosition(), target.getAdapterPosition());
                        adapterPos = viewHolder.getAdapterPosition();
                        targetPos = target.getAdapterPosition();
                    }


                    notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());

                   SQLiteDatabase db  = youPlayDatabase.getDatabase(YouPlayDatabase.PLAYLIST_DB);

                    String from = playlists.get(adapterPos);
                    int fromPos = youPlayDatabase.getIdOrder(from, "playlistTables");
                    Log.d("Playlist", "From: " + from);

                    String to = playlists.get(targetPos);
                    int toPos = youPlayDatabase.getIdOrder(to, "playlistTables");
                    Log.d("Playlist", "To: " + to);

                    ContentValues newValues = new ContentValues();
                    newValues.put(TITLE, from);

                    // nemoze se ovako jer kad se ova linija izvrsi onda postoje "dvije iste baze pod".
                    db.update("playlistTables", newValues, "_id = ?", new String[]{Integer.toString(toPos)});

                    ContentValues oldValues = new ContentValues();
                    oldValues.put(TITLE, to);

                    db.update("playlistTables", oldValues, "_id = ?", new String[]{Integer.toString(fromPos)});

                    db.close();
                    return true;
                }
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
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        touchHelper.attachToRecyclerView(null);
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

    @SuppressLint("ClickableViewAccessibility")
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

            if(isEdit) {
                holder.dragDrop.setVisibility(View.VISIBLE);
                holder.info.setVisibility(View.GONE);

                holder.dragDrop.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if(motionEvent.getAction() ==  MotionEvent.ACTION_MOVE ||
                                motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            touchHelper.startDrag(holder);
                            return true;
                        }
                        return false;
                    }
                });
            } else {
                holder.dragDrop.setVisibility(View.GONE);
                holder.info.setVisibility(View.VISIBLE);
            }

            if(id != null)
                Glide.with(context).load(FileManager.getPictureFile(id)).apply(new RequestOptions().override(80,120)).into(holder.image);
            else
                Glide.with(context).load(R.mipmap.ic_launcher).into(holder.image);

            holder.title.setText(title);
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
                Glide.with(context).load(station.getIcon()).apply(new RequestOptions().error(R.drawable.image_holder)).into(holder.image);
            else
                Glide.with(context).load(R.drawable.image_holder).into(holder.image);


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
        private ImageView dragDrop;

        ViewHolder(View v)
        {
            super(v);
            image    = v.findViewById(R.id.playlist_image);
            title    = v.findViewById(R.id.playlist_title);
            duration = v.findViewById(R.id.playlist_duration);
            info     = v.findViewById(R.id.playlist_info);
            dragDrop = v.findViewById(R.id.drag_drop_playlist);
        }
    }

    @Override
    public void onClick(View view) {
        if(listener != null && !isEdit && play == ListType.PLAYLIST_TABLE)
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

    public void setEdit(boolean edit) {
        isEdit = edit;
        notifyDataSetChanged();
    }

    public boolean getEdit() {
        return isEdit;
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
