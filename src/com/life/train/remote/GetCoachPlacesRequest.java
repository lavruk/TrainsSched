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

import com.life.train.err.AppException;
import com.life.train.remote.base.BaseRequest;

public class GetCoachPlacesRequest extends BaseRequest<List<Integer>> {

    public static final String COACH_NUM = "coach_num";
    public static final String COACH_TYPE_ID = "coach_type_id";
    public static final String DATE_START = "date_start";
    public static final String STATION_ID_FROM = "station_id_from";
    public static final String STATION_ID_TILL = "station_id_till";
    public static final String TRAIN = "train";

    public GetCoachPlacesRequest() throws URISyntaxException {
        super(buildURI("purchase/coach"));
    }

    @Override
    public List<Integer> parseJson(JSONObject json) throws JSONException, AppException {

        List<Integer> result = new ArrayList<Integer>();

        JSONArray optJSONArray = json.optJSONArray("value");
        if(optJSONArray != null){
            for (int i = 0; i < optJSONArray.length(); i++) {
                result.add(optJSONArray.optInt(i));
            }
        }

        return result;
    }

    public List<Integer> execute(Bundle data) throws AppException {

        ArrayList<NameValuePair> par = new ArrayList<NameValuePair>();

        par.add(new BasicNameValuePair(COACH_NUM,       data.getString(COACH_NUM)));
        par.add(new BasicNameValuePair(COACH_TYPE_ID,   data.getString(COACH_TYPE_ID)));
        par.add(new BasicNameValuePair(DATE_START,      data.getString(DATE_START)));
        par.add(new BasicNameValuePair(STATION_ID_FROM, data.getString(STATION_ID_FROM)));
        par.add(new BasicNameValuePair(STATION_ID_TILL, data.getString(STATION_ID_TILL)));
        par.add(new BasicNameValuePair(TRAIN,           data.getString(TRAIN)));

        return super.execute(par);
    }

}
