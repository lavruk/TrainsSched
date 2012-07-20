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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.util.Log;

import com.life.train.provider.TrainContract.SearchSuggest;
import com.life.train.provider.TrainContract.Stations;
import com.life.train.provider.TrainContract.TrainCoachPlaces;
import com.life.train.provider.TrainContract.TrainRequest;
import com.life.train.provider.TrainContract.TrainSchedule;
import com.life.train.provider.TrainContract.Trains;
import com.life.train.provider.TrainDatabase.StationSearchColumns;
import com.life.train.provider.TrainDatabase.Tables;
import com.life.train.util.SelectionBuilder;

/**
 * Provider that stores {@link TrainContract} data. Data is usually inserted
 * by {@link SyncService}, and queried by various {@link Activity} instances.
 */
public class TrainProvider extends ContentProvider {
    private static final String TAG = "ScheduleProvider";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

    private TrainDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int STATIONS = 100;
    private static final int STATIONS_ID = 101;
    private static final int STATION_SEARCH = 102;
    private static final int STATION_LETTER = 103;

    private static final int TRAINS = 200;
    private static final int TRAINS_ID = 201;

    private static final int SEARCH_SUGGEST = 300;

    private static final int TRAINS_REQUEST = 400;
    private static final int TRAINS_REQUEST_LAST = 401;
    private static final int TRAINS_REQUEST_FROM_TO = 402;
    private static final int TRAINS_REQUEST_ID = 403;
    private static final int TRAINS_REQUEST_ID_TRAINS = 404;


    private static final int TRAINS_SCHEDULE = 500;
    private static final int TRAINS_SCHEDULE_ID = 501;

    private static final int TRAINS_COACH_PLACES = 600;
    private static final int TRAINS_PLACES_BY_REQUEST_ID_AND_SCHEDULE_ID = 601;
    private static final int TRAINS_COACH_PLACES_ID = 602;

    private static final int TRAIN_COACHES_BY_REQUES_AND_SCHED = 700;




    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = TrainContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "stations", STATIONS);
        matcher.addURI(authority, "stations/search/*", STATION_SEARCH);
        matcher.addURI(authority, "stations/letter/*", STATION_LETTER);
        matcher.addURI(authority, "stations/*", STATIONS_ID);

        matcher.addURI(authority, "train_requests", TRAINS_REQUEST);
        matcher.addURI(authority, "train_requests/last", TRAINS_REQUEST_LAST);
        matcher.addURI(authority, "train_requests/from/*/to/*/departure/*", TRAINS_REQUEST_FROM_TO);
        matcher.addURI(authority, "train_requests/*", TRAINS_REQUEST_ID);
        matcher.addURI(authority, "train_requests/*/trains", TRAINS_REQUEST_ID_TRAINS);


        matcher.addURI(authority, "trains", TRAINS);
        matcher.addURI(authority, "trains/*", TRAINS_ID);

        matcher.addURI(authority, "trains_schedule", TRAINS_SCHEDULE);
        matcher.addURI(authority, "trains_schedule/*", TRAINS_SCHEDULE_ID);

        matcher.addURI(authority, "trains_coach_places", TRAINS_COACH_PLACES);
        matcher.addURI(authority, "trains_coach_places/request_id/*/schedule_id/*", TRAINS_PLACES_BY_REQUEST_ID_AND_SCHEDULE_ID);
        matcher.addURI(authority, "trains_coach_places/*", TRAINS_COACH_PLACES_ID);

        matcher.addURI(authority, "train_coaches/request_id/*/schedule_id/*", TRAIN_COACHES_BY_REQUES_AND_SCHED);

        matcher.addURI(authority, "search_suggest_query", SEARCH_SUGGEST);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = TrainDatabase.getHelper(context);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case STATIONS:
                return Stations.CONTENT_TYPE;
            case STATIONS_ID:
                return Stations.CONTENT_ITEM_TYPE;
            case TRAINS:
                return Trains.CONTENT_TYPE;
            case TRAINS_ID:
                return Trains.CONTENT_ITEM_TYPE;
            case STATION_SEARCH:
                return Stations.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (LOGV) Log.v(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        final SQLiteDatabase db = mOpenHelper.getDB();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
            }
            case SEARCH_SUGGEST: {
                final SelectionBuilder builder = new SelectionBuilder();

                // Adjust incoming query to become SQL text match
                selectionArgs[0] = selectionArgs[0] + "%";
                builder.table(Tables.SEARCH_SUGGEST);
                builder.where(selection, selectionArgs);
                builder.map(SearchManager.SUGGEST_COLUMN_QUERY,
                        SearchManager.SUGGEST_COLUMN_TEXT_1);

                projection = new String[] { BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1,
                        SearchManager.SUGGEST_COLUMN_QUERY };

                final String limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
                return builder.query(db, projection, null, null, SearchSuggest.DEFAULT_SORT, limit);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (LOGV) Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getDB();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case STATIONS: {
                db.insertOrThrow(Tables.STATIONS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Stations.buildStationUri(values.getAsString(Stations.STATION_ID));
            }
            case TRAINS: {
                db.insertOrThrow(Tables.TRAINS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Trains.buildTrainUri(values.getAsString(Trains.TRAIN_NUM));

            }
            case TRAINS_REQUEST: {
                long requestId = db.insertOrThrow(Tables.TRAIN_REQUESTS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return TrainRequest.buildRequestUri(Long.toString(requestId));
            }
            case TRAINS_SCHEDULE: {
                long id = db.insertOrThrow(Tables.TRAIN_SCHEDULE, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return TrainSchedule.buildTrainSchedUri(Long.toString(id));
            }
            case TRAINS_COACH_PLACES: {
                long id = db.insertOrThrow(Tables.TRAIN_PLACES, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return null;//TrainCoachPlaces.buildTrainSchedUri(Long.toString(id));
            }
            case SEARCH_SUGGEST: {
                db.insertOrThrow(Tables.SEARCH_SUGGEST, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return SearchSuggest.CONTENT_URI;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (LOGV) Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getDB();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (LOGV) Log.v(TAG, "delete(uri=" + uri + ")");
        final SQLiteDatabase db = mOpenHelper.getDB();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getDB();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case STATIONS: {
                return builder.table(Tables.STATIONS);
            }
            case TRAINS_SCHEDULE: {
                return builder.table(Tables.TRAIN_SCHEDULE);
            }
            case STATIONS_ID: {
                final String blockId = Stations.getStationId(uri);
                return builder.table(Tables.STATIONS)
                        .where(Stations.STATION_ID + "=?", blockId);
            }
            case TRAINS: {
                return builder.table(Tables.TRAINS);
            }
            case TRAINS_ID: {
                final String trackId = Trains.getTrackId(uri);
                return builder.table(Tables.TRAINS)
                        .where(Trains.TRAIN_NUM + "=?", trackId);
            }
            case SEARCH_SUGGEST: {
                return builder.table(Tables.SEARCH_SUGGEST);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case STATIONS: {
                return builder.table(Tables.STATIONS);
            }
            case TRAINS_SCHEDULE: {
                return builder.table(Tables.TRAIN_SCHEDULE);
            }
            case TRAINS_SCHEDULE_ID: {
                final String schedId = TrainSchedule.getSchedulateId(uri);
                return builder.table(Tables.TRAIN_SCHEDULE)
                        .where(BaseColumns._ID+"=?", schedId);
            }
            case STATIONS_ID: {
                final String stationId = Stations.getStationId(uri);
                return builder.table(Tables.STATIONS)
                        .where(Stations.STATION_ID+"=?", stationId);
            }
            case TRAINS_ID: {
                final String trackId = Trains.getTrackId(uri);
                return builder.table(Tables.TRAINS)
                        .where(Trains.TRAIN_NUM + "=?", trackId);
            }
            case TRAINS_REQUEST: {
                return builder.table(Tables.TRAIN_REQUESTS_JOIN_STATIONS)
                        .map("_id", Tables.TRAIN_REQUESTS+"._id")
                        .map(TrainRequest.STATION_NAME_FROM, "s1."+Stations.STATION_NAME)
                        .map(TrainRequest.STATION_ID_FROM, "s1."+Stations.STATION_ID)
                        .map(TrainRequest.STATION_NAME_TO, "s2."+Stations.STATION_NAME)
                        .map(TrainRequest.STATION_ID_TO, "s2."+Stations.STATION_ID);
            }
            case TRAINS_REQUEST_ID: {
                return builder.table(Tables.TRAIN_REQUESTS_JOIN_STATIONS)
                        .map("_id", Tables.TRAIN_REQUESTS+"._id")
                        .map(TrainRequest.STATION_NAME_FROM, "s1."+Stations.STATION_NAME)
                        .map(TrainRequest.STATION_ID_FROM, "s1."+Stations.STATION_ID)
                        .map(TrainRequest.STATION_NAME_TO, "s2."+Stations.STATION_NAME)
                        .map(TrainRequest.STATION_ID_TO, "s2."+Stations.STATION_ID)
                        .where(Tables.TRAIN_REQUESTS+"._id=?", TrainRequest.getRequestId(uri));
            }
            case TRAINS_REQUEST_LAST: {
                return builder.table(Tables.TRAIN_REQUESTS_JOIN_STATIONS)
                        .map("_id", Tables.TRAIN_REQUESTS+"._id")
                        .map(TrainRequest.STATION_NAME_FROM, "s1."+Stations.STATION_NAME)
                        .map(TrainRequest.STATION_ID_FROM, "s1."+Stations.STATION_ID)
                        .map(TrainRequest.STATION_NAME_TO, "s2."+Stations.STATION_NAME)
                        .map(TrainRequest.STATION_ID_TO, "s2."+Stations.STATION_ID)
                        .limit("1");
            }
            case TRAINS_REQUEST_FROM_TO: {

                final String from = TrainRequest.getStationFrom(uri);
                final String to = TrainRequest.getStationTo(uri);
                final String departure = TrainRequest.getDeparture(uri);

                return builder.table(Tables.TRAIN_REQUESTS_JOIN_TRAINS_SCHEDULE)
                        .map(BaseColumns._ID, "trains."+BaseColumns._ID)
                        .map(Trains.TRAIN_NUM, "trains."+Trains.TRAIN_NUM)
                        .map(TrainSchedule.DATE_DEPARTURE, "ts."+TrainSchedule.DATE_DEPARTURE)
                        .map(TrainSchedule.DATE_ARRIVAL, "ts."+TrainSchedule.DATE_ARRIVAL)
                        .map(Trains.STATION_START, "trains."+Trains.STATION_START)
                        .map(Trains.STATION_END, "trains."+Trains.STATION_END)
                        .map("duration", "(strftime('%s',ts.date_arrival) - strftime('%s', ts.date_departure))")
                        .where("trains.station_id_from=? AND trains.station_id_to=? AND ts.date_departure > ? AND ts.date_departure < datetime(?, 'start of day', '+1 day')", from, to, departure, departure);
            }

            case TRAINS_REQUEST_ID_TRAINS: {

                final String requestId = TrainRequest.getRequestId(uri);

                return builder.table(Tables.TRAIN_REQUESTS_JOIN_TRAINS_SCHEDULE)
                        .map("request_id", "tr."+BaseColumns._ID)
                        .map("scedule_id", "ts."+BaseColumns._ID)
                        .map(BaseColumns._ID, "trains."+BaseColumns._ID)
                        .map(Trains.TRAIN_NUM, "trains."+Trains.TRAIN_NUM)
                        .map(TrainSchedule.DATE_DEPARTURE, "ts."+TrainSchedule.DATE_DEPARTURE)
                        .map(TrainSchedule.DATE_ARRIVAL, "ts."+TrainSchedule.DATE_ARRIVAL)
                        .map(Trains.STATION_START, "trains."+Trains.STATION_START)
                        .map(Trains.STATION_END, "trains."+Trains.STATION_END)
                        .map("duration", "(strftime('%s',ts.date_arrival) - strftime('%s', ts.date_departure))")
                        .where("tr._id=?", requestId);
            }
            case TRAINS_PLACES_BY_REQUEST_ID_AND_SCHEDULE_ID: {

                final String requestId = uri.getPathSegments().get(2);
                final String scheduleId = uri.getPathSegments().get(4);

                return builder.table(Tables.TRAIN_PLACES)
                        .where(TrainCoachPlaces.TRAIN_REQUEST_ID+"=? AND "+TrainCoachPlaces.TRAIN_SCHED_ID+"=?", requestId, scheduleId);
            }
            case TRAIN_COACHES_BY_REQUES_AND_SCHED: {

                final String requestId = uri.getPathSegments().get(2);
                final String scheduleId = uri.getPathSegments().get(4);

                return builder.table(Tables.TRAIN_PLACES_JOIN_TRAINS_SCHEDULE)
                        .map(TrainSchedule.STATION_ID_FROM, "ts."+TrainSchedule.STATION_ID_FROM)
                        .map(TrainSchedule.STATION_ID_TO, "ts."+TrainSchedule.STATION_ID_TO)
                        .map(TrainSchedule.TRAIN_NUM, "ts."+TrainSchedule.TRAIN_NUM)
                        .map(TrainCoachPlaces.COACH_TYPE, "tp."+TrainCoachPlaces.COACH_TYPE)
                        .map(TrainSchedule.DATE_DEPARTURE, "ts."+TrainSchedule.DATE_DEPARTURE)
                        .map(TrainCoachPlaces.COACH_NAME, "tp."+TrainCoachPlaces.COACH_NAME)
                        .map(TrainCoachPlaces.COACH_PLACES, "tp."+TrainCoachPlaces.COACH_PLACES)
                        .where(TrainCoachPlaces.TRAIN_REQUEST_ID+"=? AND "+TrainCoachPlaces.TRAIN_SCHED_ID+"=?", requestId, scheduleId);
            }
            case STATION_SEARCH: {
                final String query = Stations.getSearchQuery(uri);
                return builder.table(Tables.STATION_SEARCH_JOIN_STATION)
                        .mapToTable(Stations._ID, Tables.STATIONS)
                        .mapToTable(Stations.STATION_ID, Tables.STATIONS)
                        .mapToTable(Stations.STATION_NAME, Tables.STATIONS)
                        .where(StationSearchColumns.BODY + " MATCH ?", query.toLowerCase()+"*");
            }
            case STATION_LETTER: {
                final String letter = uri.getPathSegments().get(2);
                return builder.table(Tables.STATIONS)
                        .where("upper(substr(station_name, 1, 3)) = upper(?)", letter).limit("1");
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private interface Subquery {

        /*String BLOCK_CONTAINS_STARRED = "(SELECT MAX(" + Qualified.SESSIONS_STARRED + ") FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + ")";*/


    }

    /**
     * {@link TrainContract} fields that are fully qualified with a specific
     * parent {@link Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {
        //        String SESSIONS_SESSION_ID = Tables.SESSIONS + "." + Sessions.SESSION_ID;
        //        String SESSIONS_BLOCK_ID = Tables.SESSIONS + "." + Sessions.BLOCK_ID;
        //        String SESSIONS_ROOM_ID = Tables.SESSIONS + "." + Sessions.ROOM_ID;
        //
        //        String SESSIONS_TRACKS_SESSION_ID = Tables.SESSIONS_TRACKS + "."
        //                + SessionsTracks.SESSION_ID;
        //        String SESSIONS_TRACKS_TRACK_ID = Tables.SESSIONS_TRACKS + "."
        //                + SessionsTracks.TRACK_ID;
        //
        //        String SESSIONS_SPEAKERS_SESSION_ID = Tables.SESSIONS_SPEAKERS + "."
        //                + SessionsSpeakers.SESSION_ID;
        //        String SESSIONS_SPEAKERS_SPEAKER_ID = Tables.SESSIONS_SPEAKERS + "."
        //                + SessionsSpeakers.SPEAKER_ID;
        //
        //        String VENDORS_VENDOR_ID = Tables.VENDORS + "." + Vendors.VENDOR_ID;
        //        String VENDORS_TRACK_ID = Tables.VENDORS + "." + Vendors.TRACK_ID;
        //
        //        @SuppressWarnings("hiding")
        //        String SESSIONS_STARRED = Tables.SESSIONS + "." + Sessions.SESSION_STARRED;
        //
        //        String TRACKS_TRACK_ID = Tables.TRAINS + "." + Tracks.TRACK_ID;
        //        String BLOCKS_BLOCK_ID = Tables.STATIONS + "." + Stations.STATION_ID;
    }
}
