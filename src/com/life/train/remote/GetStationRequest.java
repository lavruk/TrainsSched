package com.life.train.remote;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.life.train.Const;
import com.life.train.entry.Station;
import com.life.train.err.AppException;
import com.life.train.remote.base.BaseRequest;

public class GetStationRequest extends BaseRequest<List<Station>> {

    public GetStationRequest(String lang, String text) throws URISyntaxException {
        super(new URI(Const.BASE_URL+lang+"purchase/station/"+text+"/"));
    }

    @Override
    public List<Station> parseJson(JSONObject json) throws JSONException, AppException {

        List<Station> result = new ArrayList<Station>();

        JSONArray optJSONArray = json.optJSONArray("value");
        if(optJSONArray != null){
            for (int i = 0; i < optJSONArray.length(); i++) {
                result.add(new Station(optJSONArray.optJSONObject(i)));
            }
        }

        return result;
    }


    /*@Override
    public List<Station> parseJackson(String json) throws AppException {

        List<Station> result = new ArrayList<Station>();

        JsonFactory jfactory = new JsonFactory();
        try {

            JsonParser jParser = jfactory.createJsonParser(json);

            // loop until token equal to "}"
            while (jParser.nextToken() != JsonToken.END_OBJECT) {

                if ("value".equals(jParser.getCurrentName())) {

                    jParser.nextToken(); // current token is "[", move next

                    // messages is array, loop until token equal to "]"
                    while (jParser.nextToken() != JsonToken.END_ARRAY) {

                        String stationId = null;
                        String name = null;
                        Date date = null;

                        // loop until token equal to "}"
                        while (jParser.nextToken() != JsonToken.END_OBJECT) {

                            String fieldname = jParser.getCurrentName();

                            if ("station_id".equals(fieldname)) {
                                jParser.nextToken();
                                stationId = jParser.getText();
                            }

                            if ("title".equals(fieldname) || "station".equals(fieldname)) {
                                jParser.nextToken();
                                name = jParser.getText();
                            }

                            if ("date".equals(fieldname)) {
                                jParser.nextToken();

                                long dataSec = jParser.getLongValue();
                                date = (dataSec==0) ? null : new Date(dataSec*1000);
                            }
                        }

                        if(!TextUtils.isEmpty(stationId) && !TextUtils.isEmpty(name)){
                            result.add(new Station(stationId, name, date));
                        }

                    }

                }

            }
            jParser.close();


        } catch (JsonParseException e) {
            throw new AppException(e.getMessage(), e);
        } catch (IOException e) {
            throw new AppException(e.getMessage(), e);
        }


        return result;
    }*/

}
