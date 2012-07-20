package com.life.train.ui.fragment;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.ActionBarSherlock.OnActionModeFinishedListener;
import com.actionbarsherlock.ActionBarSherlock.OnActionModeStartedListener;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.life.train.R;
import com.life.train.entry.CoachType;
import com.life.train.entry.Train;
import com.life.train.err.AppException;
import com.life.train.provider.TrainContract.TrainCoachPlaces;
import com.life.train.provider.TrainContract.TrainCoaches;
import com.life.train.provider.TrainContract.TrainRequest;
import com.life.train.provider.TrainContract.TrainSchedule;
import com.life.train.provider.TrainContract.Trains;
import com.life.train.remote.GetTrainsRequest;
import com.life.train.ui.activity.TrainActivity;
import com.life.train.ui.view.TrainItemView;
import com.life.train.util.AppLog;
import com.life.train.util.DateTools;

public class ScheduleListFragment extends SherlockListFragment
implements LoaderManager.LoaderCallbacks<Cursor>, OnActionModeStartedListener, OnActionModeFinishedListener{

    public static final String TAG = ScheduleListFragment.class.getSimpleName();

    CursorAdapter mAdapter;

    private boolean mIsActionMode;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.empty_schedule_list));

        getSherlockActivity().setSupportProgress(Window.PROGRESS_END);
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);

        mAdapter = new ScheduleCursorAdapter(getActivity());

        setListAdapter(mAdapter);

        setListShown(false);
        registerForContextMenu(getListView());
        getLoaderManager().initLoader(0, null, this).forceLoad();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        StringBuilder trainText = new StringBuilder();
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        if(cursor != null){
            trainText.append(cursor.getString(cursor.getColumnIndexOrThrow(Trains.TRAIN_NUM)))
            .append(", ")
            .append(cursor.getString(cursor.getColumnIndexOrThrow(Trains.STATION_START)))
            .append("-")
            .append(cursor.getString(cursor.getColumnIndexOrThrow(Trains.STATION_END)))
            .append(", ")
            .append(cursor.getString(cursor.getColumnIndexOrThrow(TrainSchedule.DATE_DEPARTURE)))
            .append("/")
            .append(cursor.getString(cursor.getColumnIndexOrThrow(TrainSchedule.DATE_ARRIVAL)))
            .append(" (");

            final String requestId = cursor.getString(cursor.getColumnIndexOrThrow("request_id"));
            final String scheduleId = cursor.getString(cursor.getColumnIndexOrThrow("scedule_id"));
            Cursor cur = getActivity().getContentResolver().query(TrainCoachPlaces.buildPlacesUri(requestId, scheduleId), null, null, null, null);
            while (cur.moveToNext()) {
                trainText.append(cur.getString(cur.getColumnIndexOrThrow(TrainCoachPlaces.COACH_LETTER)))
                .append("-")
                .append(cur.getString(cur.getColumnIndexOrThrow(TrainCoachPlaces.COACH_PLACES)));

                if(!cur.isLast()){
                    trainText.append(", ");
                }
            }
            trainText.append(")");
            cur.close();
        }

        getSherlockActivity().startActionMode(new ScheduleActionMode(trainText.toString()));

    }

    // These are the Contacts rows that we will retrieve.
    static final String[] TRAIN_REQUESTA_PROJECTION = new String[] {
        TrainRequest._ID,
        TrainRequest.STATION_NAME_FROM,
        TrainRequest.STATION_NAME_TO,
        TrainRequest.DEPARTURE_DATE,
        TrainRequest.CREATE_DATE
    };


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new TrainsLoader(getActivity(), getActivity().getIntent().getData());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        final boolean errorLoading = ((TrainsLoader)loader).errorLoading;

        if(errorLoading){
            Toast.makeText(getActivity(), getString(R.string.error_connection), Toast.LENGTH_LONG).show();
            setEmptyText(getString(R.string.error_connection));
        }

        mAdapter.swapCursor(data);

        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }



    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        // action mode, do nothing
        if(mIsActionMode){
            return;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(position);
        if(cursor != null){
            final String requestId = cursor.getString(cursor.getColumnIndexOrThrow("request_id"));
            final String scheduleId = cursor.getString(cursor.getColumnIndexOrThrow("scedule_id"));

            Intent intent = new Intent(getActivity(), TrainActivity.class);
            intent.setData(TrainCoaches.buildUri(requestId, scheduleId));
            startActivity(intent);
        }else{
            Toast.makeText(getActivity(), R.string.error_activity_data_toast, Toast.LENGTH_SHORT).show();
        }


    }



    static class TrainsLoader extends AsyncTaskLoader<Cursor>{

        boolean errorLoading = false;

        private static final String[] TRAINS_SCHED_PROJECTION = new String[] {
            "request_id", "scedule_id", BaseColumns._ID, Trains.TRAIN_NUM, TrainSchedule.DATE_DEPARTURE,
            TrainSchedule.DATE_ARRIVAL, Trains.STATION_START, Trains.STATION_END, "duration"
        };

        private Uri mRequestUri;

        public TrainsLoader(Context context, Uri data) {
            super(context);
            mRequestUri = data;
        }

        @Override
        public Cursor loadInBackground() {

            loadNewTrainSchedule();

            return getContext().getContentResolver().query(mRequestUri, TRAINS_SCHED_PROJECTION, null, null, TrainSchedule.DATE_DEPARTURE);
        }

        void loadNewTrainSchedule(){

            ContentResolver contentResolver = getContext().getContentResolver();

            final String requestId = TrainRequest.getRequestId(mRequestUri);

            Uri request = null;
            Cursor requestCur = contentResolver.query(TrainRequest.buildRequestUri(requestId), null, null, null, null);
            if(requestCur.moveToFirst()){
                request = TrainRequest.buildRequestUri(
                        requestCur.getString(requestCur
                                .getColumnIndexOrThrow(TrainRequest.STATION_ID_FROM)), requestCur.getString(requestCur
                                        .getColumnIndexOrThrow(TrainRequest.STATION_ID_TO)), DateTools.fromSQL(requestCur
                                                .getString(requestCur.getColumnIndexOrThrow(TrainRequest.DEPARTURE_DATE))));
            }
            requestCur.close();


            if(request == null){
                return;
            }

            List<Train> trainsSchedule = Collections.emptyList();
            try {
                trainsSchedule = new GetTrainsRequest().execute(request);
            } catch (AppException e) {
                errorLoading = true;
                AppLog.error(TAG, e.getMessage(), e);
            } catch (URISyntaxException e) {
                errorLoading = true;
                AppLog.error(TAG, e.getMessage(), e);
            }


            final Date requestDate = new Date();


            if(trainsSchedule != null){
                ContentValues values = new ContentValues();
                for (Train train : trainsSchedule) {
                    // add train
                    values.clear();
                    values.put(Trains.TRAIN_NUM, train.number);
                    values.put(Trains.TRAIN_MODEL, train.model);
                    values.put(Trains.STATION_START, train.fromStation.name);
                    values.put(Trains.STATION_END, train.toStation.name);
                    contentResolver.insert(Trains.CONTENT_URI, values);


                    // add train schedule
                    String schedId = null;
                    Cursor cur = contentResolver.query(TrainSchedule.CONTENT_URI,
                            new String[] { BaseColumns._ID },
                            TrainSchedule.TRAIN_NUM + "=? AND " + TrainSchedule.DATE_DEPARTURE
                            + "=? AND " + TrainSchedule.DATE_ARRIVAL + "=? AND "
                            + TrainSchedule.STATION_ID_FROM + "=? AND "
                            + TrainSchedule.STATION_ID_TO + "=?",
                            new String[] {
                            train.number, TrainRequest.formatDate(train.fromStation.date),
                            TrainRequest.formatDate(train.toStation.date), train.fromStation.id, train.toStation.id
                    }, null);
                    if (cur.moveToFirst()) {
                        schedId = cur.getString(0);
                    }
                    cur.close();

                    if(schedId == null){
                        values.clear();
                        values.put(TrainSchedule.TRAIN_NUM, train.number);
                        values.put(TrainSchedule.DATE_DEPARTURE, TrainRequest.formatDate(train.fromStation.date));
                        values.put(TrainSchedule.DATE_ARRIVAL, TrainRequest.formatDate(train.toStation.date));
                        values.put(TrainSchedule.STATION_ID_FROM, train.fromStation.id);
                        values.put(TrainSchedule.STATION_ID_TO, train.toStation.id);

                        final Uri insertUri = contentResolver.insert(TrainSchedule.CONTENT_URI, values);
                        schedId = TrainSchedule.getSchedulateId(insertUri);
                    }

                    // add train coach places
                    for (CoachType coachtype : train.cars) {
                        values.clear();
                        values.put(TrainCoachPlaces.TRAIN_REQUEST_ID, requestId);
                        values.put(TrainCoachPlaces.TRAIN_SCHED_ID, schedId);
                        values.put(TrainCoachPlaces.TRAIN_NUM, train.number);
                        values.put(TrainCoachPlaces.DATE_REQUEST, TrainRequest.formatDate(requestDate));
                        values.put(TrainCoachPlaces.COACH_TYPE, coachtype.typeId);
                        values.put(TrainCoachPlaces.COACH_NAME, coachtype.title);
                        values.put(TrainCoachPlaces.COACH_LETTER, coachtype.letter);
                        values.put(TrainCoachPlaces.COACH_PLACES, coachtype.places);
                        contentResolver.insert(TrainCoachPlaces.CONTENT_URI, values);
                    }

                }
            }
        }

    }

    static class ScheduleCursorAdapter extends CursorAdapter{

        public ScheduleCursorAdapter(Context context) {
            super(context, null, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TrainItemView trainView = (TrainItemView) view;
            trainView.setTrain(context, cursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup arg2) {
            return new TrainItemView(context, null);
        }

    }


    private final class ScheduleActionMode implements ActionMode.Callback {

        private String mTrainText;

        public ScheduleActionMode(String text) {
            mTrainText = text;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            onActionModeStarted(mode);

            menu.add(Menu.NONE, 101, Menu.NONE, "Share")
            .setIcon(android.R.drawable.ic_menu_share)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case 101:

                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, mTrainText);
                    startActivity(Intent.createChooser(share, getString(R.string.share_train_description)));

                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            onActionModeFinished(mode);
        }


    }


    @Override
    public void onActionModeFinished(ActionMode mode) {
        mIsActionMode = false;
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        mIsActionMode = true;
    }





}
