package com.life.train.entry;

import org.json.JSONObject;

public class CoachType {

    public final Train train;
    public final int typeId;
    public final String title;
    public final String letter;
    public final float price;
    public final int places;

    public CoachType(Train train, JSONObject json) {
        this.train= train;
        this.typeId = json.optInt("type_id");
        this.title = json.optString("title");
        this.letter = json.optString("letter");
        this.price = json.optInt("price");
        this.places = json.optInt("places");
    }

}
