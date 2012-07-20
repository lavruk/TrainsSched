package com.life.train.ui.activity;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.life.train.Const;
import com.life.train.R;
import com.life.train.entry.Station;
import com.life.train.err.AppException;
import com.life.train.provider.TrainContract;
import com.life.train.provider.TrainContract.Stations;
import com.life.train.remote.GetStationRequest;
import com.life.train.util.AppLog;
import com.life.train.util.Utils;

public class SelectStationActivity extends SherlockFragmentActivity {

    public static final String TAG = SelectStationActivity.class.getSimpleName();

    public static final int REQUES_CODE_SELECT_STATION = 1;
    public static final String EXTRA_DIRECTION_TYPE = "direction";

    public static final String EXTRA_STATION_NAME = "station_id";
    public static final String EXTRA_STATION_ID = "station_id";


    private EditText mTrainsFrom;

    private GetStationTask mSearchTask;

    private CursorLoaderListFragment mListFtragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_station);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportProgressBarIndeterminateVisibility(false);


        FragmentManager fm = getSupportFragmentManager();

        mListFtragment = (CursorLoaderListFragment)fm.findFragmentByTag("select_station");

        mTrainsFrom = (EditText)findViewById(R.id.station_name);
        mTrainsFrom.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                final String searchText = s.toString();

                mListFtragment.onSearch(searchText);
                if(s.length() > 2){
                    final String firstLetters = searchText.substring(0, 3);
                    //                    if(mFirstLetter != firstLetters){
                    //                        mFirstLetter = firstLetters;

                    Cursor query = getContentResolver().query(Stations.buildLetterUri(firstLetters), null, null, null, null);
                    boolean letterInDb = query.moveToFirst();
                    query.close();

                    if(!letterInDb){
                        onSearchTask(firstLetters);
                    }

                    //                    }
                }

            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onDestroy() {
        onCancelSearchTask();
        super.onDestroy();
    }

    protected void onSearchTask(String searchText) {

        onCancelSearchTask();

        mSearchTask = new GetStationTask();
        mSearchTask.execute(searchText);
    }

    protected void onCancelSearchTask(){
        if(mSearchTask != null && mSearchTask.getStatus() != AsyncTask.Status.FINISHED){
            mSearchTask.cancel(true);
            mSearchTask = null;
        }
    }

    public static class CursorLoaderListFragment extends SherlockListFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

        SimpleCursorAdapter mAdapter;

        String mCurFilter;


        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            setEmptyText(getString(R.string.empty_station_list));

            setHasOptionsMenu(true);

            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new SimpleCursorAdapter(getActivity(),
                    android.R.layout.simple_list_item_2, null,
                    new String[] { Stations.STATION_NAME, Stations.TAGS },
                    new int[] { android.R.id.text1, android.R.id.text2}, 0);
            setListAdapter(mAdapter);

            setListShown(false);

            getLoaderManager().initLoader(0, null, this);
        }

        public void onSearch(String newText) {
            mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
            getLoaderManager().restartLoader(0, null, CursorLoaderListFragment.this);
        }

        @Override public void onListItemClick(ListView l, View v, int position, long id) {

            Cursor cur = (Cursor)mAdapter.getItem(position);

            Intent intent = getActivity().getIntent();
            intent.putExtra(EXTRA_STATION_ID, cur.getString(cur.getColumnIndexOrThrow(Stations.STATION_ID)));
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();

        }

        // These are the Contacts rows that we will retrieve.
        static final String[] STATION_SUMMARY_PROJECTION = new String[] {
            Stations._ID,
            Stations.STATION_ID,
            Stations.STATION_NAME,
            Stations.TAGS,
        };

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri baseUri;
            if (mCurFilter != null) {
                //                baseUri = Stations.buildSearchUri(Uri.encode(mCurFilter));
                baseUri = Stations.buildSearchUri(mCurFilter);
            } else {
                baseUri = Stations.CONTENT_URI;
            }

            String select = "((" + Stations.STATION_NAME + " NOTNULL) AND ("
                    + Stations.STATION_NAME + " != '' ))";
            return new CursorLoader(getActivity(), baseUri,
                    STATION_SUMMARY_PROJECTION, select, null,
                    Stations.STATION_NAME + " COLLATE LOCALIZED ASC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);

            // The list should now be shown.
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
    }

    private void showProgress(){
        setSupportProgressBarIndeterminateVisibility(true);
    }

    private void hideProgress(){
        setSupportProgressBarIndeterminateVisibility(false);
    }

    class GetStationTask extends AsyncTask<String, Void, Boolean>{

        boolean errorLoading = false;

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected Boolean doInBackground(String... params) {

            if(params.length == 0){
                return false;
            }

            List<Station> stations = Collections.emptyList();

            // try get stations by UKR
            try {
                if(Utils.isCyrillic(params[0])) {
                    stations = new GetStationRequest(Const.LANG_UA, params[0]).execute(null);
                    stations.addAll(new GetStationRequest(Const.LANG_RU, params[0]).execute(null));
                }else{
                    stations = new GetStationRequest(Const.LANG_EN, params[0]).execute(null);
                }

            } catch (AppException e) {
                errorLoading = true;
                AppLog.error(TAG, e.getMessage(), e);
            } catch (URISyntaxException e) {
                errorLoading = true;
                AppLog.error(TAG, e.getMessage(), e);
            }

            final ContentResolver contentResolver = getContentResolver();
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

            for (Station station : stations) {

                Cursor stationCursor = contentResolver.query(Stations.CONTENT_URI, null, Stations.STATION_ID+"=?", new String[]{station.id}, null);

                if(stationCursor != null && stationCursor.moveToFirst()){

                    String tags = stationCursor.getString(stationCursor.getColumnIndexOrThrow(Stations.TAGS));
                    if(tags.indexOf(station.name.toLowerCase())==-1){
                        tags +=", "+ station.name.toLowerCase();
                    }

                    batch.add(ContentProviderOperation.newUpdate(Stations.CONTENT_URI)
                            .withValue(Stations.TAGS, tags)
                            .withSelection(Stations.STATION_ID+"=?", new String[]{station.id})
                            .build());
                }else{
                    batch.add(ContentProviderOperation.newInsert(Stations.CONTENT_URI)
                            .withValue(Stations.STATION_ID, station.id)
                            .withValue(Stations.STATION_NAME, station.name)
                            .withValue(Stations.TAGS, station.name.toLowerCase())
                            .build());
                }
                if(stationCursor!=null) stationCursor.close();
            }
            try {
                contentResolver.applyBatch(TrainContract.CONTENT_AUTHORITY, batch);
            } catch (RemoteException e) {
                AppLog.error(TAG, e.getMessage(), e);
            } catch (OperationApplicationException e) {
                AppLog.error(TAG, e.getMessage(), e);
            }

            return (stations != null);
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if(isCancelled()){
                return;
            }

            if(errorLoading){
                Toast.makeText(SelectStationActivity.this, getString(R.string.error_connection), Toast.LENGTH_LONG).show();
                mListFtragment.setEmptyText(getString(R.string.error_connection));
            }

            if(result){
                mListFtragment.onSearch(mTrainsFrom.getText().toString());
            }
            hideProgress();
        }

    }


    public static void show(Fragment fragment, int typy) {
        Intent intent = new Intent(fragment.getActivity(), SelectStationActivity.class);
        intent.putExtra(EXTRA_DIRECTION_TYPE, typy);
        fragment.startActivityForResult(intent, REQUES_CODE_SELECT_STATION);
    }

}
