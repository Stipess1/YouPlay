package com.stipess.youplay.Ilisteners;

import android.view.View;

import com.stipess.youplay.radio.Country;
import com.stipess.youplay.radio.Station;

public interface OnRadioSelected {
    void onClickCountry(Country country, View v);

    void onClickStation(Station station, View v);

    void onInfoClicked(Station station);
}
