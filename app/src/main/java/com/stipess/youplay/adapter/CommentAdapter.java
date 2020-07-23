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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.stipess.youplay.Ilisteners.OnCommentClicked;
import com.stipess.youplay.R;
import com.stipess.youplay.fragments.PlayFragment;

import org.schabi.newpipe.extractor.comments.CommentsInfoItem;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    private Context context;
    private int resource;
    private OnCommentClicked listener;
    private List<CommentsInfoItem> list = new ArrayList<>();
    private  ItemTouchHelper touchHelper;
    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;
    private int lastPosition = -1;

    public CommentAdapter(Context context, int resource, List<CommentsInfoItem> list) {
        this.context = context;
        this.resource = resource;
        this.list = list;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_ITEM) {
            View view = LayoutInflater.from(context).inflate(resource, parent, false);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(lp);
            view.setOnClickListener(this);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.progress_item, parent, false);
            return new ProgressViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ViewHolder) {
            CommentsInfoItem item = list.get(position);

            ((ViewHolder) holder).author.setText(item.getUploaderName());
            ((ViewHolder) holder).commentText.setText(item.getCommentText());
            ((ViewHolder) holder).commentLike.setText(Integer.toString(item.getLikeCount()));
            ((ViewHolder) holder).commentDate.setText(item.getTextualUploadDate());
            Glide.with(context).load(item.getUploaderAvatarUrl()).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(false)
            ).into(((ViewHolder) holder).image);

            ((ViewHolder) holder).itemView.setTag(item);

            setAnimation(((ViewHolder) holder).itemView, position);
        } else {
            ((ProgressViewHolder) holder).bar.setIndeterminate(true);
            setAnimation(((ProgressViewHolder) holder).itemView, position);
        }

    }

    private void setAnimation(View animate, int position){
        if(position > lastPosition)
        {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down);
            animate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position) != null ? VIEW_ITEM : VIEW_PROG;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
//        super.onAttachedToRecyclerView(recyclerView);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }
        };

        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(null);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        touchHelper.attachToRecyclerView(null);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onClick(View view) {
        CommentsInfoItem item = (CommentsInfoItem) view.getTag();
        listener.onCommentClicked(item, view);
    }

    public void setListener(OnCommentClicked onCommentClicked) {
        this.listener = onCommentClicked;
    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        holder.setIsRecyclable(false);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private TextView author;
        private TextView commentText;
        private TextView commentLike;
        private TextView commentDate;

        ViewHolder(@NonNull View v) {
            super(v);

            image = v.findViewById(R.id.comment_image);
            author = v.findViewById(R.id.comment_author);
            commentText = v.findViewById(R.id.comment_text);
            commentLike = v.findViewById(R.id.comment_like_number);
            commentDate = v.findViewById(R.id.comment_date);
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
