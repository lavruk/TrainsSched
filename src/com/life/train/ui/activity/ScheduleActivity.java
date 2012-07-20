package com.life.train.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.life.train.R;

public class ScheduleActivity extends SherlockFragmentActivity {

    private static final String TAG = ScheduleActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle arg0) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(arg0);
        setContentView(R.layout.activity_schedule);
        setSupportProgressBarIndeterminateVisibility(false);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Uri data = getIntent().getData();
        if(data == null){
            Toast.makeText(getBaseContext(), R.string.error_activity_data_toast, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    public static void show(Context context, Uri data) {
        Intent intent = new Intent(context, ScheduleActivity.class);
        intent.setData(data);
        context.startActivity(intent);
    }

}
