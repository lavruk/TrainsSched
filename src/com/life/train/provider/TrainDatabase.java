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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.life.train.provider.TrainContract.Stations;
import com.life.train.provider.TrainContract.StationsColumns;
import com.life.train.provider.TrainContract.TrainCoachPlaces;
import com.life.train.provider.TrainContract.TrainColumns;
import com.life.train.provider.TrainContract.TrainRequest;
import com.life.train.provider.TrainContract.TrainRequestColumns;
import com.life.train.provider.TrainContract.TrainSchedule;
import com.life.train.provider.TrainContract.TrainStationColumns;
import com.life.train.provider.TrainContract.Trains;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link TrainProvider}.
 */
public class TrainDatabase extends SQLiteOpenHelper {
    private static final String TAG = "ScheduleDatabase";

    private static final String DATABASE_NAME = "schedule.db";

    private SQLiteDatabase mDbConnection;
    private static TrainDatabase instance;

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int VER_LAUNCH = 1;
    private static final int VER_2 = 2;
    private static final int VER_UPDATE_SEARCH = 3;


    private static final int DATABASE_VERSION = VER_UPDATE_SEARCH;

    private TrainDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized TrainDatabase getHelper(Context context) {
        if (instance == null)
            instance = new TrainDatabase(context);

        return instance;
    }


    public SQLiteDatabase getDB(){

        if(mDbConnection == null || !mDbConnection.isOpen()){
            mDbConnection = getWritableDatabase();
        }
        return mDbConnection;
    }

    /*@Override @Deprecated
    public synchronized SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase();
    }*/

    /*@Override @Deprecated
    public synchronized SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase();
    }*/

    interface Tables {
        String STATIONS = "stations";
        String TRAINS = "trains";
        String TRAIN_REQUESTS = "trains_requests";
        String TRAIN_SCHEDULE = "trains_schedule";
        String TRAIN_PLACES = "trains_places";

        String STANTION_SEARCH = "station_search";

        String SEARCH_SUGGEST = "search_suggest";

        String TRAIN_REQUESTS_JOIN_STATIONS = "trains_requests "
                + " LEFT JOIN stations as s1 ON trains_requests.station_id_from=s1.station_id"
                + " LEFT JOIN stations as s2 ON trains_requests.station_id_to=s2.station_id";

        String TRAIN_REQUESTS_JOIN_TRAINS_SCHEDULE = "trains " +
                " LEFT JOIN trains_schedule as ts ON trains.train_num=ts.train_num " +
                " LEFT JOIN trains_requests as tr ON ts.station_id_from=tr.station_id_from " +
                " AND ts.station_id_to = tr.station_id_to " +
                " AND ts.date_departure >= tr.departure_date " +
                " AND ts.date_departure < datetime(tr.departure_date, 'start of day', '+1 day')";

        String TRAIN_PLACES_JOIN_TRAINS_SCHEDULE = "trains_places as tp " +
                "LEFT JOIN trains_schedule as ts " +
                "ON tp.train_sched_id=ts._id";

        String STATION_SEARCH_JOIN_STATION = "station_search "
                + "LEFT OUTER JOIN stations ON station_search.station_id=stations.station_id";

    }

    private interface Triggers {
        String STATION_SEARCH_INSERT = "station_search_insert";
        String STATION_SEARCH_DELETE = "station_search_delete";
        String STATION_SEARCH_UPDATE = "station_search_update";
    }

    interface StationSearchColumns {
        String STANTION_ID = "station_id";
        String BODY = "body";
    }

    /** Fully-qualified field names. */
    private interface Qualified {
        String STATION_SEARCH_STATION_ID = Tables.STANTION_SEARCH + "."
                + StationSearchColumns.STANTION_ID;

        String STATION_SEARCH = Tables.STANTION_SEARCH + "(" + StationSearchColumns.STANTION_ID
                + "," + StationSearchColumns.BODY + ")";
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String STATION_ID = "REFERENCES " + Tables.STATIONS + "(" + Stations.STATION_ID + ")";
        String TRAIN_NUM = "REFERENCES " + Tables.TRAINS + "(" + Trains.TRAIN_NUM + ")";
    }

    private interface Subquery {
        /**
         * Subquery used to build the {@link StationsSearchColumns#BODY} string
         * used for indexing {@link Stations} content.
         */
        String STATION_BODY = "(new." + Stations.STATION_NAME
                + "||'; '||" + "coalesce(new." + Stations.TAGS + ", '')"
                + ")";


    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //STATIONS
        db.execSQL("CREATE TABLE " + Tables.STATIONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + StationsColumns.STATION_ID + " TEXT NOT NULL,"
                + StationsColumns.STATION_NAME + " TEXT NOT NULL,"
                + StationsColumns.TAGS + " TEXT NOT NULL,"
                + "UNIQUE (" + StationsColumns.STATION_ID + ") ON CONFLICT IGNORE)");

        //TRAIN_REQUESTS
        db.execSQL("CREATE TABLE " + Tables.TRAIN_REQUESTS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TrainRequest.STATION_ID_FROM + " TEXT NOT NULL,"
                + TrainRequest.STATION_ID_TO + " TEXT NOT NULL,"
                + TrainRequestColumns.DEPARTURE_DATE + " TEXT NOT NULL,"
                + TrainRequestColumns.CREATE_DATE + " TEXT NOT NULL)");
        //TRAINS
        db.execSQL("CREATE TABLE " + Tables.TRAINS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Trains.TRAIN_NUM + " TEXT NOT NULL,"
                + Trains.TRAIN_MODEL + " INTEGER,"
                + Trains.STATION_START + " TEXT NOT NULL,"
                + Trains.STATION_END + " TEXT NOT NULL,"
                + "UNIQUE (" + TrainColumns.TRAIN_NUM + ") ON CONFLICT IGNORE)");

        //TRAIN_SCHEDULE
        db.execSQL("CREATE TABLE " + Tables.TRAIN_SCHEDULE + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TrainSchedule.TRAIN_NUM + " TEXT NOT NULL,"
                + TrainStationColumns.STATION_ID_FROM + " TEXT NOT NULL,"
                + TrainStationColumns.STATION_ID_TO + " TEXT NOT NULL,"
                + TrainSchedule.DATE_DEPARTURE+ " TEXT NOT NULL,"
                + TrainSchedule.DATE_ARRIVAL + " TEXT NOT NULL,"
                + "UNIQUE (" + TrainColumns.TRAIN_NUM + ","
                + TrainSchedule.DATE_DEPARTURE + ","
                + TrainSchedule.DATE_ARRIVAL + ","
                + TrainStationColumns.STATION_ID_FROM + ","
                + TrainStationColumns.STATION_ID_TO +
                " ) ON CONFLICT IGNORE)");

        //TRAIN_PLACES
        db.execSQL("CREATE TABLE " + Tables.TRAIN_PLACES+ " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TrainCoachPlaces.TRAIN_NUM + " TEXT NOT NULL,"
                + TrainCoachPlaces.TRAIN_REQUEST_ID+ " INTEGER NOT NULL,"
                + TrainCoachPlaces.TRAIN_SCHED_ID+ " INTEGER NOT NULL,"
                + TrainCoachPlaces.DATE_REQUEST + " TEXT NOT NULL,"
                + TrainCoachPlaces.COACH_NAME + " TEXT NOT NULL,"
                + TrainCoachPlaces.COACH_LETTER + " TEXT NOT NULL,"
                + TrainCoachPlaces.COACH_TYPE + " INTEGER NOT NULL,"
                + TrainCoachPlaces.COACH_PLACES + " INTEGER NOT NULL, "
                + TrainCoachPlaces.RECEIVED_TIME + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"+")");


        createStantionSearch(db);

        db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL)");

    }

    /**
     * Create triggers that automatically build {@link Tables#STANTION_SEARCH}
     * as values are changed in {@link Tables#SESSIONS}.
     */
    public static void createStantionSearch(SQLiteDatabase db) {
        // Using the "porter" tokenizer for simple stemming, so that
        // "frustration" matches "frustrated."

        db.execSQL("CREATE VIRTUAL TABLE " + Tables.STANTION_SEARCH + " USING fts3("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + StationSearchColumns.BODY + " TEXT NOT NULL,"
                + StationSearchColumns.STANTION_ID
                + " TEXT NOT NULL " + References.STATION_ID
                + ")");
        //                + ",tokenize=porter)");

        // TODO: handle null fields in body, which cause trigger to fail
        // TODO: implement update trigger, not currently exercised

        createSearchTrigers(db);

    }

    public static void createSearchTrigers(SQLiteDatabase db){
        db.execSQL("CREATE TRIGGER " + Triggers.STATION_SEARCH_INSERT + " AFTER INSERT ON "
                + Tables.STATIONS + " BEGIN "
                + "DELETE FROM station_search  WHERE station_search.station_id=new.station_id;"
                + "INSERT INTO " + Qualified.STATION_SEARCH + " "
                + " VALUES(new." + Stations.STATION_ID + ", " + Subquery.STATION_BODY + ");"
                + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.STATION_SEARCH_DELETE + " AFTER DELETE ON "
                + Tables.STATIONS + " BEGIN DELETE FROM " + Tables.STANTION_SEARCH + " "
                + " WHERE " + Qualified.STATION_SEARCH_STATION_ID + "=old." + Stations.STATION_ID
                + ";" + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.STATION_SEARCH_UPDATE
                + " AFTER UPDATE ON " + Tables.STATIONS
                + " BEGIN UPDATE " + Tables.STANTION_SEARCH + " SET " + StationSearchColumns.BODY  + " = "
                + Subquery.STATION_BODY + " WHERE station_id = old.station_id"
                + "; END;");
    }

    public static void deleteSearchTrigers(SQLiteDatabase db){
        db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.STATION_SEARCH_INSERT);
        db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.STATION_SEARCH_DELETE);
        db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.STATION_SEARCH_UPDATE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // NOTE: This switch statement is designed to handle cascading database
        // updates, starting at the current version and falling through to all
        // future upgrade cases. Only use "break;" when you want to drop and
        // recreate the entire database.
        int version = oldVersion;

        switch (version) {
            case VER_LAUNCH:{
                version = VER_2;
            }
            case VER_2:{

                //refresh trigers
                deleteSearchTrigers(db);
                createSearchTrigers(db);

                // add new column "tags"
                List<String> stationsColumns = getColumns(db, Tables.STATIONS);
                if(!stationsColumns.contains(Stations.TAGS)){
                    db.execSQL("ALTER TABLE " + Tables.STATIONS + " ADD COLUMN "
                            + Stations.TAGS + " TEXT DEFAULT ''");


                    Cursor cur = db.rawQuery("SELECT * FROM "+Tables.STATIONS, null);
                    ContentValues values = new ContentValues();
                    while (cur.moveToNext()) {
                        values.clear();
                        values.put(Stations.TAGS, cur.getString(cur.getColumnIndex(Stations.STATION_NAME)).toLowerCase());
                        db.update(Tables.STATIONS, values, BaseColumns._ID+"=?", new String[]{cur.getString(cur.getColumnIndex(BaseColumns._ID))});
                    }
                    cur.close();
                }

                version = VER_UPDATE_SEARCH;
            }

        }

        Log.d(TAG, "after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            Log.w(TAG, "Destroying old data during upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.STATIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRAIN_REQUESTS);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRAINS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRAIN_SCHEDULE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRAIN_PLACES);

            deleteSearchTrigers(db);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.STANTION_SEARCH);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);

            onCreate(db);
        }
    }

    public static List<String> getColumns(SQLiteDatabase db, String tableName) {
        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = new ArrayList<String>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }
}
