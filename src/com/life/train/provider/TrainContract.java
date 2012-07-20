/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.life.train.provider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.life.train.util.ParserUtils;

/**
 * Contract class for interacting with {@link TrainProvider}. Unless
 * otherwise noted, all time-based fields are milliseconds since epoch and can
 * be compared against {@link System#currentTimeMillis()}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link Uri} are generated
 * using stronger {@link String} identifiers, instead of {@code int}
 * {@link BaseColumns#_ID} values, which are prone to shuffle during sync.
 */
public class TrainContract {

    /**
     * Special value for {@link SyncColumns#UPDATED} indicating that an entry
     * has never been updated, or doesn't exist yet.
     */
    public static final long UPDATED_NEVER = -2;

    /**
     * Special value for {@link SyncColumns#UPDATED} indicating that the last
     * update time is unknown, usually when inserted from a local file source.
     */
    public static final long UPDATED_UNKNOWN = -1;

    public interface SyncColumns {
        /** Last time this entry was updated or synchronized. */
        String UPDATED = "updated";
    }

    interface StationsColumns {
        String STATION_ID = "station_id";
        String STATION_NAME = "station_name";
        String TAGS = "tags";
    }

    interface TrainStationColumns {
        String STATION_ID_FROM = "station_id_from";
        String STATION_ID_TO = "station_id_to";
    }

    interface TrainRequestColumns {
        String DEPARTURE_DATE = "departure_date";
        String CREATE_DATE = "create_date";
    }

    interface TrainColumns {
        String TRAIN_NUM = "train_num";
        String TRAIN_MODEL = "train_model";
        String STATION_START = "station_start";
        String STATION_END = "station_end";
    }

    interface TrainSchedColumns{
        String TRAIN_NUM = TrainColumns.TRAIN_NUM;
        String DATE_DEPARTURE = "date_departure";
        String DATE_ARRIVAL = "date_arrival";
    }

    interface TrainCoachTypeColumns{
        String TRAIN_NUM = TrainColumns.TRAIN_NUM;
        String TRAIN_REQUEST_ID = "train_request_id";
        String TRAIN_SCHED_ID = "train_sched_id";
        String DATE_REQUEST = "date_request";
        String COACH_NAME = "coach_name";
        String COACH_LETTER = "coach_letter";
        String COACH_TYPE = "coach_type";
        String COACH_PLACES = "coach_places";
        String RECEIVED_TIME = "received_time";
    }

    interface TrainCoachColumns{
        String TRAIN_REQUEST_ID = "train_request_id";
        String TRAIN_SCHED_ID = "train_sched_id";

        String COACH_NUMBER = "coach_num";
        String SERVICE = "service";
        String PRICE = "coach_type";
        String PLACES_COUNT = "coach_places";
    }

    interface PlacesColumns{

        String TRAIN_REQUEST_ID = "train_request_id";
        String TRAIN_SCHED_ID = "train_sched_id";
        String COACH_ID = "coach_id";

        String PLACE_NUMBER = "place_num";
        String PLACE_TYPE = "place_type";
    }

    public static final String CONTENT_AUTHORITY = "com.life.train.provider";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_STATIONS = "stations";
    private static final String PATH_TRAINS = "trains";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_LETTER = "letter";
    private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";
    private static final String PATH_TRAIN_REQUEST = "train_requests";
    private static final String PATH_TRAINS_SCHEDULE = "trains_schedule";
    private static final String PATH_TRAINS_COACH_PLACES = "trains_coach_places";
    private static final String PATH_TRAIN_COACHES = "train_coaches";



    public static class Stations implements StationsColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_STATIONS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.comlife.station";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.comlife.station";

        public static Uri buildStationUri(int stationId) {
            return buildStationUri(Integer.toString(stationId));
        }

        public static Uri buildStationUri(String stationId) {
            return CONTENT_URI.buildUpon().appendPath(stationId).build();
        }

        public static Uri buildSearchUri(String query) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).appendPath(query).build();
        }

        public static Uri buildLetterUri(String letter) {
            return CONTENT_URI.buildUpon().appendPath(PATH_LETTER).appendPath(letter).build();
        }

        public static String getSearchQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getStationId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String generateStationId(String stationId) {
            return ParserUtils.sanitizeId(stationId);
        }

        public static boolean exist(ContentResolver contentResolver, String id) {
            boolean result = false;
            Cursor cur = contentResolver.query(CONTENT_URI, new String[]{STATION_ID}, STATION_ID+"=?", new String[]{id}, null);
            if(cur!=null){
                result = cur.moveToFirst();
                cur.close();
            }
            return result;
        }
    }

    public static class TrainRequest implements TrainRequestColumns, TrainStationColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRAIN_REQUEST).build();
        private static final SimpleDateFormat mIso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public static final String STATION_NAME_FROM = "station_name_from";
        public static final String STATION_NAME_TO = "station_name_to";

        //        public static final String CONTENT_TYPE =
        //                "vnd.android.cursor.dir/vnd.comlife.station";
        //        public static final String CONTENT_ITEM_TYPE =
        //                "vnd.android.cursor.item/vnd.comlife.station";

        public static String formatDate(Date date){
            return mIso8601DateFormat.format(date);
        }

        public static Date parseDate(String date) throws ParseException{
            return mIso8601DateFormat.parse(date);
        }

        public static Uri buildRequestUri(String from, String to, Date date) {
            return CONTENT_URI.buildUpon()
                    .appendPath("from").appendPath(from)
                    .appendPath("to").appendPath(to)
                    .appendPath("departure").appendPath(Long.toString(date.getTime()))
                    .build();
        }

        public static Uri buildLastRequestUri() {
            return CONTENT_URI.buildUpon().appendPath("last").build();
        }


        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = TrainRequestColumns.CREATE_DATE + " DESC";

        public static String getStationFrom(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getStationTo(Uri uri) {
            return uri.getPathSegments().get(4);
        }

        public static String getDeparture(Uri uri) {
            return mIso8601DateFormat
                    .format(new Date(Long.parseLong(uri.getPathSegments().get(6))));
        }

        public static String getRequestId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static Uri buildRequestUri(String requestId) {
            return CONTENT_URI.buildUpon().appendPath(requestId).build();
        }

    }



    public static class Trains implements TrainColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRAINS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.comlife.train";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.comlife.train";

        public static final String DEFAULT_SORT = TRAIN_NUM + " ASC";

        public static Uri buildTrainUri(String trackId) {
            return CONTENT_URI.buildUpon().appendPath(trackId).build();
        }

        public static String getTrackId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String generateTrackId(String title) {
            return ParserUtils.sanitizeId(title);
        }
    }


    public static class TrainSchedule implements TrainSchedColumns, TrainStationColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRAINS_SCHEDULE).build();

        public static String getSchedulateId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static Uri buildTrainSchedUri(String trainId) {
            return CONTENT_URI.buildUpon().appendPath(trainId).build();
        }

    }

    public static class TrainCoachPlaces implements TrainCoachTypeColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRAINS_COACH_PLACES).build();

        public static Uri buildPlacesUri(String requestId, String scheduleId) {
            return CONTENT_URI.buildUpon()
                    .appendPath("request_id").appendPath(requestId)
                    .appendPath("schedule_id").appendPath(scheduleId)
                    .build();
        }

    }

    public static class TrainCoaches implements TrainCoachTypeColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRAIN_COACHES).build();

        public static Uri buildUri(String requestId, String scheduleId) {
            return CONTENT_URI.buildUpon()
                    .appendPath("request_id").appendPath(requestId)
                    .appendPath("schedule_id").appendPath(scheduleId)
                    .build();
        }

    }



    public static class SearchSuggest {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_SUGGEST).build();

        public static final String DEFAULT_SORT = SearchManager.SUGGEST_COLUMN_TEXT_1
                + " COLLATE NOCASE ASC";
    }

    private TrainContract() {
    }
}
