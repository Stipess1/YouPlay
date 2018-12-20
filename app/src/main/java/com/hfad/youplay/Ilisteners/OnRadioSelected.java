package com.hfad.youplay.Ilisteners;

import android.view.View;

import com.hfad.youplay.radio.Country;
import com.hfad.youplay.radio.Station;

public interface OnRadioSelected {
    void onClickCountry(Country country, View v);

    void onClickStation(Station station, View v);

    void onInfoClicked(Station station);
}
