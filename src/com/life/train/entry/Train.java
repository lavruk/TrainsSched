package com.life.train.entry;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Train {

    public final String number;
    public final int model;
    public final Station fromStation;
    public final Station toStation;
    public final List<CoachType> cars;

    public Train(JSONObject json) {
        number = json.optString("num");
        model = json.optInt("model");
        fromStation = new Station(json.optJSONObject("from"));
        toStation = new Station(json.optJSONObject("till"));
        cars = parseCoachList(json.optJSONArray("types"));
    }

    public List<CoachType> parseCoachList(JSONArray optJSONArray) {

        List<CoachType> resultList = new ArrayList<CoachType>();

        if(optJSONArray != null){
            for (int i = 0; i < optJSONArray.length(); i++) {
                resultList.add(new CoachType(this, optJSONArray.optJSONObject(i)));
            }
        }

        return resultList;
    }

}
