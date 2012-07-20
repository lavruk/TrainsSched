package com.life.train.entry;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class Coach {

    public String name;
    public int number;
    public boolean service;
    public int price;
    public int reservePrice;
    public List<Integer> places = new ArrayList<Integer>();
    public int placesCount;
    public int coachTypeId;
    public String content;

    public Coach(JSONObject json) {
        this.number = json.optInt("num");
        this.service = json.optBoolean("service");
        this.price = json.optInt("price");
        if(price!=0) price -= 7 * 100; // tax for operation
        this.reservePrice = json.optInt("reserve_price");
        this.placesCount = json.optInt("places_cnt");
        this.places = new ArrayList<Integer>();
        this.coachTypeId = json.optInt("coach_type_id");

        //        JSONArray optJSONArray = json.optJSONArray("places");
        //        this.places = new int[optJSONArray.length()];
        //        for (int i = 0; i < optJSONArray.length(); i++) {
        //            this.places[i] = optJSONArray.optInt(i);
        //        }

    }
}
