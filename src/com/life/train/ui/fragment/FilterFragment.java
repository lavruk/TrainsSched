package com.life.train.ui.fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.life.train.R;
import com.life.train.provider.TrainContract.Stations;
import com.life.train.provider.TrainContract.TrainRequest;
import com.life.train.ui.activity.RequestHistoryActivity;
import com.life.train.ui.activity.SelectStationActivity;
import com.life.train.ui.fragment.RequestsListFragment.OnRequestSelectedListener;

public class FilterFragment extends SherlockFragment implements OnClickListener,
LoaderManager.LoaderCallbacks<Cursor> , OnRequestSelectedListener{

    private static final int MENU_ITEM_HISTORY = 100;
    private static final int MENU_ID_CHANGE_STATIONS = 200;

    private static final int REQUES_CODE_SELECT_STATION = 1;
    private static final int REQUEST_CODE_HISTORY_SELECTED = 2;

    private static final int DIRECTION_FROM = 0;
    private static final int DIRECTION_TO = 1;

    public static final SimpleDateFormat DateFormat = new SimpleDateFormat("dd.MM.yyyy");
    public static final SimpleDateFormat TimeFormat = new SimpleDateFormat("HH:mm");

    private static final String[] DATA_PROJECTION = new String[] {
        TrainRequest.STATION_NAME_FROM, TrainRequest.STATION_NAME_TO,
        TrainRequest.STATION_ID_FROM, TrainRequest.STATION_ID_TO,
        TrainRequest.DEPARTURE_DATE
    };


    private Button mStationFromView;
    private Button mStationToView;
    private Button mStartDateView;
    private Button mStartTimeView;
    private String mStationIdFrom;
    private String mStationIdTo;
    private Calendar mDepartureDate;
    private OnTrainRequestListener mRequestListener;


    public static FilterFragment newInstance(){
        return new FilterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {


        menu.add(Menu.NONE, MENU_ID_CHANGE_STATIONS, 0, "Change station")
        .setIcon(R.drawable.ic_shuffle)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(Menu.NONE, MENU_ITEM_HISTORY, 0, "History")
        .setIcon(R.drawable.ic_history)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == MENU_ID_CHANGE_STATIONS){
            changeStations();
            return true;
        }else if(item.getItemId() == MENU_ITEM_HISTORY){
            RequestHistoryActivity.show(this, REQUEST_CODE_HISTORY_SELECTED);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mRequestListener = (OnTrainRequestListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnTrainRequestListener");
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_find_train, null, false);

        mDepartureDate = Calendar.getInstance();
        mDepartureDate.set(Calendar.HOUR_OF_DAY, 0);
        mDepartureDate.set(Calendar.MINUTE, 0);
        mDepartureDate.set(Calendar.SECOND, 0);

        mStationFromView = (Button) v.findViewById(R.id.station_from);
        mStationFromView.setOnClickListener(this);

        mStationToView = (Button) v.findViewById(R.id.station_to);
        mStationToView.setOnClickListener(this);

        mStartDateView = (Button) v.findViewById(R.id.start_date);
        mStartDateView.setOnClickListener(this);

        mStartTimeView = (Button) v.findViewById(R.id.start_time);
        mStartTimeView.setOnClickListener(this);

        Button searchTrainButton = (Button) v.findViewById(R.id.search_train);
        searchTrainButton.setOnClickListener(this);

        //        v.findViewById(R.id.shuffle_stations).setOnClickListener(this);

        updateDateTimeView();

        return  v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // init loader
        getLoaderManager().initLoader(0, null, this);
    }


    void updateDateTimeView(){
        mStartDateView.setText(DateFormat.format(mDepartureDate.getTime()));
        mStartTimeView.setText(TimeFormat.format(mDepartureDate.getTime()));
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.station_from) {
            SelectStationActivity.show(this, DIRECTION_FROM);
        } else if (v.getId() == R.id.station_to) {
            SelectStationActivity.show(this, DIRECTION_TO);
        } else if (v.getId() == R.id.start_date) {
            new DatePickerDialog(new ContextThemeWrapper(getActivity(), R.style.Theme_HoloEverywhereDark_Sherlock),

                    new OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear,
                        int dayOfMonth) {
                    mDepartureDate.set(year, monthOfYear, dayOfMonth);
                    updateDateTimeView();
                }
            }, mDepartureDate.get(Calendar.YEAR), mDepartureDate.get(Calendar.MONTH),
            mDepartureDate.get(Calendar.DAY_OF_MONTH)).show();
        } else if (v.getId() == R.id.start_time) {
            new TimePickerDialog(new ContextThemeWrapper(getActivity(), R.style.Theme_HoloEverywhereDark_Sherlock), new OnTimeSetListener() {

                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    mDepartureDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    mDepartureDate.set(Calendar.MINUTE, minute);

                    updateDateTimeView();
                }
            }, mDepartureDate.get(Calendar.HOUR_OF_DAY), mDepartureDate.get(Calendar.MINUTE), true).show();
        } else if (v.getId() == R.id.search_train) {

            if(TextUtils.isEmpty(mStationIdFrom) || TextUtils.isEmpty(mStationIdTo)){
                Toast.makeText(getActivity(), R.string.error_station_is_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(TrainRequest.STATION_ID_FROM, mStationIdFrom);
            values.put(TrainRequest.STATION_ID_TO, mStationIdTo);
            values.put(TrainRequest.DEPARTURE_DATE, TrainRequest.formatDate(mDepartureDate.getTime()));
            values.put(TrainRequest.CREATE_DATE, TrainRequest.formatDate(new Date()));
            final Uri schedUri = getActivity().getContentResolver().insert(TrainRequest.CONTENT_URI, values);
            Uri requestUri = schedUri.buildUpon().appendPath("trains").build();
            if(mRequestListener!=null) mRequestListener.onRequest(requestUri);
        }
    }


    private void changeStations(){
        final String idFrom = mStationIdFrom;
        final String nameFrom = mStationFromView.getText().toString();

        final String idTo = mStationIdTo;
        final String nameTo = mStationToView.getText().toString();

        mStationIdFrom = idTo;
        mStationFromView.setText(nameTo);

        mStationIdTo = idFrom;
        mStationToView.setText(nameFrom);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            switch (requestCode) {
                case REQUES_CODE_SELECT_STATION:
                    setStationByData(data);
                    break;

                case REQUEST_CODE_HISTORY_SELECTED:
                    onRequestSelected(data.getData());
                    break;

                default:
                    break;
            }
        }

    }

    private void setStationByData(Intent data) {

        int direction = data.getIntExtra(SelectStationActivity.EXTRA_DIRECTION_TYPE, DIRECTION_FROM);
        String stationId = data.getStringExtra(SelectStationActivity.EXTRA_STATION_ID);
        String stationName = "";

        Cursor cur = getActivity().getContentResolver().query(Stations.buildStationUri(stationId), null, null, null, null);
        if(cur.moveToFirst()){
            stationName = cur.getString(cur.getColumnIndexOrThrow(Stations.STATION_NAME));
        }
        cur.close();

        switch (direction) {
            case DIRECTION_FROM:
                mStationIdFrom = stationId;
                mStationFromView.setText(stationName);
                break;
            case DIRECTION_TO:
                mStationIdTo = stationId;
                mStationToView.setText(stationName);
                break;

            default:
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(getActivity(), TrainRequest.buildLastRequestUri(), DATA_PROJECTION, null, null, TrainRequest.CREATE_DATE + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cur) {
        if(cur.moveToFirst()){

            mStationIdFrom = cur.getString(cur.getColumnIndexOrThrow(TrainRequest.STATION_ID_FROM));
            mStationIdTo = cur.getString(cur.getColumnIndexOrThrow(TrainRequest.STATION_ID_TO));

            mStationFromView.setText(cur.getString(cur.getColumnIndexOrThrow(TrainRequest.STATION_NAME_FROM)));
            mStationToView.setText(cur.getString(cur.getColumnIndexOrThrow(TrainRequest.STATION_NAME_TO)));

            try {
                mDepartureDate.setTime(TrainRequest.parseDate(cur.getString(cur.getColumnIndexOrThrow(TrainRequest.DEPARTURE_DATE))));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            updateDateTimeView();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public interface OnTrainRequestListener{
        void onRequest(Uri uri);
    }

    @Override
    public void onRequestSelected(Uri requestUri) {
        Cursor cur = getActivity().getContentResolver().query(requestUri, DATA_PROJECTION, null, null, null);
        onLoadFinished(null, cur);
        cur.close();
    }

}
