package com.life.train.ui.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.life.train.R;
import com.life.train.provider.TrainContract.TrainSchedule;
import com.life.train.provider.TrainContract.Trains;
import com.life.train.ui.fragment.PlacesListFragment;

public class TrainActivity extends SherlockFragmentActivity {

    private PlacesListFragment mListFtragment;

    @Override
    protected void onCreate(Bundle arg0) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(arg0);

        setContentView(R.layout.activity_train);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportProgressBarIndeterminateVisibility(false);

        String trainNum = null;
        String schedId = getIntent().getData().getPathSegments().get(4);
        Cursor cur = getContentResolver().query(TrainSchedule.buildTrainSchedUri(schedId), null, null, null, null);
        if(cur!=null){
            if(cur.moveToFirst()){
                trainNum = cur.getString(cur.getColumnIndex(TrainSchedule.TRAIN_NUM));
                getSupportActionBar().setTitle(getString(R.string.title_train_details, trainNum));
                getSupportActionBar().setSubtitle(cur.getString(cur.getColumnIndex(TrainSchedule.DATE_DEPARTURE)));

            }
            cur.close();
        }

        if(trainNum != null){
            cur = getContentResolver().query(Trains.buildTrainUri(trainNum), null, null, null, null);
            if(cur!=null){
                if(cur.moveToFirst()){


                    final String stationStart = cur.getString(cur.getColumnIndex(Trains.STATION_START));
                    final String stationEnd = cur.getString(cur.getColumnIndex(Trains.STATION_END));

                    ((TextView)findViewById(R.id.direction)).setText(stationStart+" - "+stationEnd);

                }
                cur.close();
            }
        }


        FragmentManager fm = getSupportFragmentManager();

        mListFtragment = (PlacesListFragment)fm.findFragmentByTag("places_list");

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

}
