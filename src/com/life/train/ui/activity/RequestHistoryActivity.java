package com.life.train.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.life.train.R;
import com.life.train.ui.fragment.RequestsListFragment.OnRequestSelectedListener;

public class RequestHistoryActivity extends SherlockFragmentActivity implements OnRequestSelectedListener {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_reques_history);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    public static void show(Fragment fragment, int requestCodeHistorySelected) {
        fragment.startActivityForResult(new Intent(fragment.getActivity(), RequestHistoryActivity.class), requestCodeHistorySelected);
    }

    @Override
    public void onRequestSelected(Uri requestUri) {
        Intent intent = new Intent();
        intent.setData(requestUri);
        setResult(RESULT_OK, intent);
        finish();
    }

}
