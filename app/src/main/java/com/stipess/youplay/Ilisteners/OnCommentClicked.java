package com.stipess.youplay.Ilisteners;

import android.view.View;

import org.schabi.newpipe.extractor.comments.CommentsInfoItem;

public interface OnCommentClicked {

    void onCommentClicked(CommentsInfoItem item, View view);
}
