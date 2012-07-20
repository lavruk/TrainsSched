package com.life.train.ui.activity;

import android.net.Uri;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.life.train.R;
import com.life.train.ui.fragment.FilterFragment.OnTrainRequestListener;

public class MainActivity extends SherlockFragmentActivity implements OnTrainRequestListener{

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onRequest(Uri data) {
        ScheduleActivity.show(this, data);
    }

}
