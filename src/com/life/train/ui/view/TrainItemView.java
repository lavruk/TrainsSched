package com.life.train.ui.view;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.life.train.R;
import com.life.train.entry.CoachType;
import com.life.train.entry.Train;
import com.life.train.provider.TrainContract.TrainCoachPlaces;
import com.life.train.provider.TrainContract.TrainSchedule;
import com.life.train.provider.TrainContract.Trains;
import com.life.train.util.DateTools;

public class TrainItemView extends LinearLayout{

    private static final SimpleDateFormat DateFormat = new SimpleDateFormat("EEE, dd.MM.yyyy");
    private static final SimpleDateFormat TimeFormat = new SimpleDateFormat("HH:mm");

    private TextView mTrainNumberView;
    private TextView mTrainFromView;
    private LinearLayout mTrainPlacesContainer;
    private Context mContext;
    private TextView mTrainToView;
    private TextView mDateStartView;
    private TextView mDateFinishView;
    private TextView mTimeStartView;
    private TextView mTimeFinishView;
    private TextView mDurationView;
    private OnClickListener mCoachClickListener;

    public TrainItemView(Context context, OnClickListener coachClickListener) {
        super(context);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.list_item_train, this, true);

        mTrainNumberView = (TextView) findViewById(R.id.train_number);
        mTrainFromView = (TextView) findViewById(R.id.train_from);
        mTrainToView = (TextView) findViewById(R.id.train_to);

        mDateStartView = (TextView) findViewById(R.id.train_start_date);
        mDateFinishView = (TextView) findViewById(R.id.train_finish_date);

        mTimeStartView = (TextView) findViewById(R.id.train_time_start);
        mTimeFinishView = (TextView) findViewById(R.id.train_time_finish);

        mDurationView = (TextView) findViewById(R.id.duration);

        mTrainPlacesContainer = (LinearLayout) findViewById(R.id.train_places);
        mCoachClickListener = coachClickListener;
    }

    public void setTrain(Train train) {
        mTrainNumberView.setText(train.number);
        mTrainFromView.setText(train.fromStation.name);
        mTrainToView.setText(train.toStation.name);

        mDateStartView.setText(DateFormat.format(train.fromStation.date));
        mTimeStartView.setText(TimeFormat.format(train.fromStation.date));


        mDateFinishView.setText(DateFormat.format(train.toStation.date));
        mTimeFinishView.setText(TimeFormat.format(train.toStation.date));

        long duration = train.toStation.date.getTime() - train.fromStation.date.getTime();
        int hours = (int)(duration/DateUtils.HOUR_IN_MILLIS);
        int minutes = (int)((duration - hours*DateUtils.HOUR_IN_MILLIS)/DateUtils.MINUTE_IN_MILLIS);

        mDurationView.setText(hours+":"+String.format("%02d", minutes));

        // setup places
        if(mTrainPlacesContainer.getChildCount() > 0) mTrainPlacesContainer.removeAllViews();

        for (CoachType coachType : train.cars) {
            TextView tw = new TextView(mContext);
            tw.setGravity(Gravity.CENTER);
            tw.setTextSize(16);
            tw.setTypeface(Typeface.DEFAULT_BOLD);
            tw.setTag(coachType);
            //            tw.setBackgroundResource(android.R.drawable.btn_default_small);



            tw.setText(coachType.letter+" - "+coachType.places);
            tw.setOnClickListener(mCoachClickListener);
            mTrainPlacesContainer.addView(tw, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }

    }

    public void setTrain(Context context, Cursor cursor) {

        mTrainNumberView.setText(cursor.getString(cursor.getColumnIndexOrThrow(Trains.TRAIN_NUM)));
        mTrainFromView.setText(cursor.getString(cursor.getColumnIndexOrThrow(Trains.STATION_START)));
        mTrainToView.setText(cursor.getString(cursor.getColumnIndexOrThrow(Trains.STATION_END)));


        Date dateDeparture = DateTools.fromSQL(cursor.getString(cursor.getColumnIndexOrThrow(TrainSchedule.DATE_DEPARTURE)));
        Date dateArrival = DateTools.fromSQL(cursor.getString(cursor.getColumnIndexOrThrow(TrainSchedule.DATE_ARRIVAL)));

        mDateStartView.setText(DateFormat.format(dateDeparture));
        mTimeStartView.setText(TimeFormat.format(dateDeparture));

        mDateFinishView.setText(DateFormat.format(dateArrival));
        mTimeFinishView.setText(TimeFormat.format(dateArrival));

        long duration = dateArrival.getTime() - dateDeparture.getTime();
        int hours = (int)(duration/DateUtils.HOUR_IN_MILLIS);
        int minutes = (int)((duration - hours*DateUtils.HOUR_IN_MILLIS)/DateUtils.MINUTE_IN_MILLIS);

        mDurationView.setText(hours+":"+String.format("%02d", minutes));

        // setup places
        if(mTrainPlacesContainer.getChildCount() > 0) mTrainPlacesContainer.removeAllViews();

        final String requestId = cursor.getString(cursor.getColumnIndexOrThrow("request_id"));
        final String scheduleId = cursor.getString(cursor.getColumnIndexOrThrow("scedule_id"));
        Cursor cur = context.getContentResolver().query(TrainCoachPlaces.buildPlacesUri(requestId, scheduleId), null, null, null, null);
        while (cur.moveToNext()) {

            TextView tw = new TextView(mContext);
            tw.setGravity(Gravity.CENTER);
            tw.setTextSize(16);
            tw.setTypeface(Typeface.DEFAULT_BOLD);
            tw.setText(cur.getString(cur.getColumnIndexOrThrow(TrainCoachPlaces.COACH_LETTER))+" - "+cur.getString(cur.getColumnIndexOrThrow(TrainCoachPlaces.COACH_PLACES)));
            mTrainPlacesContainer.addView(tw, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        cur.close();

    }




}
