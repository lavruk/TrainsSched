package com.life.train.remote;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.life.train.entry.Coach;
import com.life.train.err.AppException;
import com.life.train.remote.base.BaseRequest;

public class GetTrainDetailsRequest extends BaseRequest<List<Coach>> {



    public static final String DATE_START = "date_start";
    public static final String COACH_TYPE_ID = "coach_type_id";
    public static final String TRAIN = "train";
    public static final String STATION_ID_TILL = "station_id_till";
    public static final String STATION_ID_FROM = "station_id_from";

    public GetTrainDetailsRequest() throws URISyntaxException {
        super(buildURI("purchase/coaches"));
    }

    @Override
    public List<Coach> parseJson(JSONObject json) throws JSONException, AppException {

        List<Coach> result = new ArrayList<Coach>();

        JSONArray optJSONArray = json.optJSONObject("value").optJSONArray("coaches");
        if(optJSONArray != null){
            for (int i = 0; i < optJSONArray.length(); i++) {
                Coach coach = new Coach(optJSONArray.optJSONObject(i));
                coach.content = json.optJSONObject("value").optString("content");
                result.add(coach);
            }
        }

        return result;
    }

    public List<Coach> execute(Bundle data) throws AppException {

        ArrayList<NameValuePair> par = new ArrayList<NameValuePair>();

        par.add(new BasicNameValuePair(COACH_TYPE_ID,   data.getString(COACH_TYPE_ID)));
        par.add(new BasicNameValuePair(DATE_START,      data.getString(DATE_START)));
        par.add(new BasicNameValuePair(STATION_ID_FROM, data.getString(STATION_ID_FROM)));
        par.add(new BasicNameValuePair(STATION_ID_TILL, data.getString(STATION_ID_TILL)));
        par.add(new BasicNameValuePair(TRAIN,           data.getString(TRAIN)));
        par.add(new BasicNameValuePair("round_trip",    "0"));

        return super.execute(par);
    }

}
