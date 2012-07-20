package com.life.train.remote;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.life.train.entry.Train;
import com.life.train.err.AppException;
import com.life.train.remote.base.BaseRequest;
import com.life.train.util.DateTools;

public class GetTrainsRequest extends BaseRequest<List<Train>> {

    public static final SimpleDateFormat DateFormat = new SimpleDateFormat("dd.MM.yyyy");
    public static final SimpleDateFormat TimeFormat = new SimpleDateFormat("HH:mm");

    public GetTrainsRequest() throws URISyntaxException {
        super(buildURI("purchase/search"));
    }

    @Override
    public List<Train> parseJson(JSONObject json) throws JSONException, AppException {

        List<Train> result = new ArrayList<Train>();

        JSONArray optJSONArray = json.optJSONArray("value");
        if(optJSONArray != null){
            for (int i = 0; i < optJSONArray.length(); i++) {
                result.add(new Train(optJSONArray.optJSONObject(i)));
            }
        }

        return result;
    }

    public List<Train> execute(Uri request) throws AppException {

        final String stationFromId = request.getPathSegments().get(2);
        final String stationToId = request.getPathSegments().get(4);
        final String departure = request.getPathSegments().get(6);

        final Date departureDate = new Date(Long.parseLong(departure));


        ArrayList<NameValuePair> par = new ArrayList<NameValuePair>();
        par.add(new BasicNameValuePair("date_start", DateTools.dateToRequest(departureDate)));
        par.add(new BasicNameValuePair("time_from", DateTools.time(departureDate)));
        //      par.add(new BasicNameValuePair("station_from", filter.stationFrom.name));
        //      par.add(new BasicNameValuePair("station_till", filter.stationTo.name));
        par.add(new BasicNameValuePair("station_id_from", stationFromId));
        par.add(new BasicNameValuePair("station_id_till", stationToId));
        par.add(new BasicNameValuePair("search", ""));

        return super.execute(par);
    }

}
