package com.stipess.youplay.Ilisteners;

import android.view.View;

/**
 * Created by Stjepan on 9.2.2018..
 */

public interface OnPlaylistSelected
{
    void onClick(String title, View view);

    void onLongClick(String title, View view);

    void onInfoClicked(String title, View view);

}
