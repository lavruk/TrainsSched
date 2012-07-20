package com.life.train.entry;

import java.util.Date;

import org.json.JSONObject;

import android.database.Cursor;
import android.os.Bundle;

import com.life.train.provider.TrainContract.Stations;
import com.life.train.ui.activity.SelectStationActivity;

public class Station{

    public final String name;
    public final String id;
    public final Date date;

    public Station(String id, String name, Date date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public Station(JSONObject json) {
        this.name = json.optString("title", json.optString("station", "none"));
        this.id = json.optString("station_id");

        long dataSec = json.optLong("date", 0);
        this.date = (dataSec==0) ? null : new Date(dataSec*1000);
    }

    public Station(Bundle bundle) {
        name = bundle.getString(SelectStationActivity.EXTRA_STATION_NAME);
        id = bundle.getString(SelectStationActivity.EXTRA_STATION_ID);
        date = null;
    }

    public Station(Cursor cur) {
        this.id = cur.getString(cur.getColumnIndexOrThrow(Stations.STATION_ID));
        this.name = cur.getString(cur.getColumnIndexOrThrow(Stations.STATION_NAME));
        this.date = null;
    }



}
