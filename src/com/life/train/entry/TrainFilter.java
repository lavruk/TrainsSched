package com.life.train.entry;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class TrainFilter {

    private static final String LAST_DATE_LEFT = "last_date_left";
    private static final String LAST_STATION_TO_NAME = "last_station_to_name";
    private static final String LAST_STATION_TO_ID = "last_station_to_id";
    private static final String LAST_STATION_FROM_NAME = "last_station_from_name";
    private static final String LAST_STATION_FROM_ID = "last_station_from_id";

    public static final SimpleDateFormat DateFormat = new SimpleDateFormat("dd.MM.yyyy");
    public static final SimpleDateFormat TimeFormat = new SimpleDateFormat("HH:mm");

    public Station stationFrom;
    public Station stationTo;
    public Calendar dateLeft;

    public TrainFilter(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        stationFrom = new Station(pref.getString(LAST_STATION_FROM_ID, ""), pref.getString(LAST_STATION_FROM_NAME, ""), null);
        stationTo = new Station(pref.getString(LAST_STATION_TO_ID, ""), pref.getString(LAST_STATION_TO_NAME, ""), null);

        long lastDateInMillis = pref.getLong(LAST_DATE_LEFT, 0);
        if(lastDateInMillis == 0){
            dateLeft = Calendar.getInstance();
            dateLeft.set(Calendar.HOUR_OF_DAY, 0);
            dateLeft.set(Calendar.MINUTE, 0);
        }else{
            dateLeft = Calendar.getInstance();
            dateLeft.setTimeInMillis(lastDateInMillis);
        }
    }

    public void save(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = pref.edit();
        edit.putString(LAST_STATION_FROM_ID, stationFrom.id);
        edit.putString(LAST_STATION_FROM_NAME, stationFrom.name);
        edit.putString(LAST_STATION_TO_ID, stationTo.id);
        edit.putString(LAST_STATION_TO_NAME, stationTo.name);
        edit.putLong(LAST_DATE_LEFT, dateLeft.getTimeInMillis());
        edit.commit();
    }

    public String getDate(){
        return DateFormat.format(dateLeft.getTime());
    }

    public String getTime(){
        return TimeFormat.format(dateLeft.getTime());
    }

}
