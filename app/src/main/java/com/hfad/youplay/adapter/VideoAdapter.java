package com.hfad.youplay.adapter;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.hfad.youplay.R;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.Ilisteners.OnMusicSelected;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.utils.Constants;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.ThemeManager;
import com.hfad.youplay.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.hfad.youplay.utils.Constants.AUTHOR;
import static com.hfad.youplay.utils.Constants.DOWNLOADED;
import static com.hfad.youplay.utils.Constants.DURATION;
import static com.hfad.youplay.utils.Constants.ID;
import static com.hfad.youplay.utils.Constants.TITLE;
import static com.hfad.youplay.utils.Constants.VIEWS;


/**
 * Created by Stjepan on 26.11.2017..
 */

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener
{
    private static final String TAG = VideoAdapter.class.getSimpleName();

    private List<Music> data;

    // Koristimo u filter funkciji
    private List<Music> filterData = new ArrayList<>();
    private Context context;
    private int resource;
    private OnMusicSelected listener;
    private YouPlayDatabase youPlayDatabase;
    private ItemTouchHelper touchHelper;
    private boolean play_fragment = false;
    private boolean setEdit = false;
    private boolean moved = false;
    private String tableName;
    private boolean history;
    // za filter
    private List<Music> selected = new ArrayList<>();


    public VideoAdapter(Context context, int resource, List<Music> objects, boolean history)
    {
        this.context = context;
        this.resource = resource;
        data = objects;
        this.history = history;
        youPlayDatabase = YouPlayDatabase.getInstance(context);
    }


    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.DOWN | ItemTouchHelper.UP,
                        0);

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

                if(viewHolder.getItemViewType() != target.getItemViewType() || viewHolder.getAdapterPosition() == 0)
                    return false;

                moved = true;
                int adapterPos = viewHolder.getAdapterPosition();
                int targetPos = target.getAdapterPosition();
                if(data.size() > 1)
                {
                    Collections.swap(data, viewHolder.getAdapterPosition()-1, target.getAdapterPosition()-1);
                    adapterPos = viewHolder.getAdapterPosition()-1;
                    targetPos = target.getAdapterPosition()-1;
                }
                else
                    Collections.swap(data, viewHolder.getAdapterPosition(), target.getAdapterPosition());

                notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());

                SQLiteDatabase db;
                if(tableName.equals(Constants.TABLE_NAME) && history)
                    db = youPlayDatabase.getDatabase(YouPlayDatabase.YOUPLAY_DB);
                else
                    db  = youPlayDatabase.getDatabase(YouPlayDatabase.PLAYLIST_DB);;

                Music from = data.get(adapterPos);
                int fromPos = youPlayDatabase.getIdOrder(from.getId(), tableName);

                Music to = data.get(targetPos);
                int toPos = youPlayDatabase.getIdOrder(to.getId(), tableName);

                ContentValues newValues = new ContentValues();
                newValues.put(TITLE, from.getTitle());
                newValues.put(ID, from.getId());
                newValues.put(AUTHOR, from.getAuthor());
                newValues.put(DURATION, from.getDuration());
                newValues.put(VIEWS, from.getViews());
                newValues.put(DOWNLOADED, from.getDownloaded());

                db.update(tableName, newValues, "_id = ?", new String[]{Integer.toString(toPos)});

                ContentValues oldValues = new ContentValues();
                oldValues.put(TITLE, to.getTitle());
                oldValues.put(ID, to.getId());
                oldValues.put(AUTHOR, to.getAuthor());
                oldValues.put(DURATION, to.getDuration());
                oldValues.put(VIEWS, to.getViews());
                oldValues.put(DOWNLOADED, to.getDownloaded());

                db.update(tableName, oldValues, "_id = ?", new String[]{Integer.toString(fromPos)});

                db.close();
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction)
            {

            }


            @Override
            public boolean isLongPressDragEnabled()
            {
                return false;
            }

        };
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    public boolean getMoved()
    {
        return moved;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
            if(viewType == 0)
            {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_top, parent, false);
                return new FirstViewHolder(view);
            }
            else
            {
                View view = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);
                view.setOnClickListener(this);
                view.setOnLongClickListener(this);
                return new ViewHolder(view);
            }
    }

    @SuppressLint({"ClickableViewAccessibility"})
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

            switch (holder.getItemViewType())
            {
                case 0:
                    long milis = 0;
                    for(Music pjesma : data)
                    {
                        if(pjesma.getDuration() != null)
                        {
                            milis += Utils.convertToMilis(pjesma.getDuration());
                        }
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    FirstViewHolder firstViewHolder = (FirstViewHolder) holder;
                    if(data.size() > 1)
                        stringBuilder.append(data.size()).append(" ").append(context.getResources().getString(R.string.songs));
                    else
                        stringBuilder.append(data.size()).append(" ").append(context.getResources().getString(R.string.songs_language));

                    firstViewHolder.songCount.setText(stringBuilder);
                    firstViewHolder.songCount.setTextColor(context.getResources().getColor(ThemeManager.getFontTheme()));
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(Utils.convertDuration(milis)).append(" ").append(context.getResources().getString(R.string.song_mins));
                    firstViewHolder.songsDuration.setText(stringBuilder);
                    firstViewHolder.songsDuration.setTextColor(context.getResources().getColor(ThemeManager.getFontTheme()));
                    firstViewHolder.itemView.setBackgroundColor(context.getResources().getColor(ThemeManager.getTheme()));
                    break;
                case 1:
                    final int pos = (data.size() > 1) ? position - 1: position;
                    final Music list = data.get(pos);
                    final ViewHolder viewHolder = (ViewHolder) holder;

                    StringBuilder stringBuilder1 = new StringBuilder();
                    stringBuilder1.append(list.getViews()).append(" ").append(context.getResources().getString(R.string.you_view));
                    viewHolder.title.setText(list.getTitle());
                    viewHolder.title.setTextColor(context.getResources().getColor(ThemeManager.getFontTheme()));
                    viewHolder.author.setText(list.getAuthor());
                    viewHolder.author.setTextColor(context.getResources().getColor(ThemeManager.getFontTheme()));
                    viewHolder.view.setText(stringBuilder1);
                    viewHolder.view.setTextColor(context.getResources().getColor(ThemeManager.getFontTheme()));
                    viewHolder.duration.setText(list.getDuration());
                    viewHolder.duration.setTextColor(context.getResources().getColor(ThemeManager.getFontTheme()));

                    Glide.with(context).load(FileManager.getPictureFile(list.getId())).thumbnail( 0.1f ).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).
                            skipMemoryCache(true).format(DecodeFormat.PREFER_RGB_565).override(480,360)).into(viewHolder.image);

                    if(list.getDownloaded() == 1)
                        viewHolder.downloaded.setText(R.string.you_downloaded);
                    else
                        viewHolder.downloaded.setText("");

                    if(ThemeManager.getDebug().equals("Dark"))
                        viewHolder.info.setImageResource(R.drawable.info_dark);
                    else
                        viewHolder.info.setImageResource(R.drawable.info);

                    if(!setEdit)
                    {
                        viewHolder.info.setVisibility(View.VISIBLE);
                        viewHolder.downloaded.setVisibility(View.VISIBLE);
                        viewHolder.dragDrop.setVisibility(View.GONE);
                        viewHolder.itemView.setBackgroundColor(context.getResources().getColor(ThemeManager.getTheme()));
                    }
                    else
                    {
                        viewHolder.info.setVisibility(View.GONE);
                        viewHolder.dragDrop.setVisibility(View.VISIBLE);
                        viewHolder.itemView.setOnClickListener(this);

                        viewHolder.dragDrop.setOnTouchListener((view, motionEvent) -> {
                            if(motionEvent.getAction() == MotionEvent.ACTION_MOVE ||
                                    motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                            {
                                touchHelper.startDrag(viewHolder);
                                return true;
                            }
                            return false;
                        });

                        if(selected.contains(list))
                            viewHolder.itemView.setBackgroundColor(context.getResources().getColor(ThemeManager.getSelectedTheme()));
                        else
                            viewHolder.itemView.setBackgroundColor(context.getResources().getColor(ThemeManager.getUnselectedTheme()));
                    }

                    viewHolder.info.setOnClickListener(view -> {
                        if(listener != null)
                        {
                            listener.onInfoClicked(pos, viewHolder.info);
                        }
                    });
                    viewHolder.itemView.setTag(list);
                    break;
            }
    }

    public boolean getState()
    {
        return setEdit;
    }

    public void setEdit(String table, Music pjesma)
    {
        setEdit = true;
        tableName = table;
        selected.clear();

        int position = data.indexOf(pjesma);
        addIfNotExists(pjesma, position);
        notifyDataSetChanged();
    }

    public void disableEdit()
    {
        setEdit = false;
        notifyDataSetChanged();
    }

    public List<Music> getAll()
    {
        return selected;
    }

    @Override
    public void onClick(View view) {
        if(listener != null && !setEdit)
        {
            Music pjesma = (Music) view.getTag();
            listener.onClick(pjesma, view);
        }
        else
        {
            Music pjesma = (Music) view.getTag();
            int pos = data.indexOf(pjesma);
            if(data.size() > 1)
                addIfNotExists(pjesma, pos+1);
            else
                addIfNotExists(pjesma, pos-1);
        }
    }

    private void addIfNotExists(Music pjesma, int rowPos)
    {
        if(selected.contains(pjesma))
            selected.remove(pjesma);
        else
            selected.add(pjesma);

        Log.d(TAG, "addifnotexists: " + pjesma.getTitle());
        notifyItemChanged(rowPos);
//        notifyDataSetChanged();
    }

    @Override
    public boolean onLongClick(View view)
    {
        if(listener != null)
        {
            Music pjesma = (Music) view.getTag();
            listener.onLongClick(pjesma, view);
        }
        return true;
    }

    public void setListener(OnMusicSelected listener)
    {
        this.listener = listener;
    }

    public void deleteMusic(int position)
    {
//        data.remove(position);
        if(data.size() > 1)
        {
            notifyItemRemoved(position+1);
            notifyItemRangeChanged(position+1, getItemCount()+1);
            notifyItemChanged(0);
        }
        else
            notifyDataSetChanged();

    }

    public void refreshList()
    {
        data.clear();
        data.addAll(youPlayDatabase.getData());
        notifyFilterData(data);
        notifyDataSetChanged();
    }

    public void filter(String query)
    {
        data.clear();

        if(query.isEmpty())
            data.addAll(filterData);
        else
        {
            query = query.toLowerCase();
            for(Music pjesma : filterData)
            {
                if(pjesma.getTitle().toLowerCase().replaceAll("-"," ").contains(query) ||
                        pjesma.getAuthor().toLowerCase().replaceAll("-"," ").contains(query))
                {
                    data.add(pjesma);
                }
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position)
    {
        if(position == 0 && data.size() > 1)
        {
            return position;
        }
        return 1;
    }

    @Override
    public int getItemCount()
    {
        if(!play_fragment)
            return (data.size() > 1) ? data.size() + 1 : data.size();
        else
            return data.size();
    }

    public void notifyFilterData(List<Music> musicList) {
        filterData.clear();
        filterData.addAll(musicList);
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        private TextView title;
        private TextView author;
        private TextView duration;
        private TextView view;
        private ImageView image;
        private ImageView info;
        private TextView downloaded;
        private ImageView dragDrop;

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
            dragDrop   = v.findViewById(R.id.drag_and_drop);

        }

    }

    class FirstViewHolder extends RecyclerView.ViewHolder
    {
        private TextView songCount;
        private TextView songsDuration;

        FirstViewHolder(View v)
        {
            super(v);
            songCount = v.findViewById(R.id.songs);
            songsDuration = v.findViewById(R.id.songs_duration);
        }
    }


}
