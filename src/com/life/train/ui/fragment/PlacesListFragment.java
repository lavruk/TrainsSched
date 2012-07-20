package com.life.train.ui.fragment;

import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.life.train.R;
import com.life.train.entry.Coach;
import com.life.train.err.AppException;
import com.life.train.provider.TrainContract.TrainCoachPlaces;
import com.life.train.provider.TrainContract.TrainRequest;
import com.life.train.provider.TrainContract.TrainSchedule;
import com.life.train.remote.GetCoachPlacesRequest;
import com.life.train.remote.GetTrainDetailsRequest;
import com.life.train.ui.view.CoachItemView;
import com.life.train.util.AppLog;
import com.life.train.util.DateTools;

public class PlacesListFragment extends SherlockListFragment
implements LoaderManager.LoaderCallbacks<List<Coach>>{

    public static final String TAG = PlacesListFragment.class.getSimpleName();

    CoachCursorAdapter mAdapter;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.empty_coach_list));

        getSherlockActivity().setSupportProgress(Window.PROGRESS_END);
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);

        mAdapter = new CoachCursorAdapter(getActivity());

        setListAdapter(mAdapter);

        setListShown(false);

        registerForContextMenu(getListView());

        getLoaderManager().initLoader(0, null, this).forceLoad();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

        Coach coach = mAdapter.getItem(info.position);

        StringBuilder sb = new StringBuilder();
        sb.append("№").append(coach.number).append(", ")
        .append(coach.name).append(", ")
        .append(getString(R.string.train_coach_price, NumberFormat.getInstance().format((float)(coach.price) /100))).append(", ")
        .append(getString(R.string.train_places_free, coach.placesCount)).append(", ")
        .append(coach.places.toString());

        getSherlockActivity().startActionMode(new CoachActionMode(sb.toString()));
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
    public Loader<List<Coach>> onCreateLoader(int id, Bundle args) {
        return new CoachLoader(getActivity(), getActivity().getIntent().getData());
    }

    @Override
    public void onLoadFinished(Loader<List<Coach>> loader, List<Coach> data) {

        final boolean errorLoading = ((CoachLoader)loader).errorLoading;

        if(errorLoading){
            Toast.makeText(getActivity(), getString(R.string.error_connection), Toast.LENGTH_LONG).show();
            setEmptyText(getString(R.string.error_connection));
        }

        mAdapter.addItems(data);

        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Coach>> loader) {
        mAdapter.notifyDataSetChanged();
    }


    static class CoachLoader extends AsyncTaskLoader<List<Coach>>{

        boolean errorLoading = false;

        private Uri mRequestUri;

        public CoachLoader(Context context, Uri data) {
            super(context);
            mRequestUri = data;
        }

        @Override
        public List<Coach> loadInBackground() {

            List<Coach> result = new ArrayList<Coach>();

            Cursor cur = getContext().getContentResolver().query(
                    mRequestUri,
                    new String[] {
                            TrainSchedule.STATION_ID_FROM, TrainSchedule.STATION_ID_TO,
                            TrainSchedule.TRAIN_NUM, TrainSchedule.DATE_DEPARTURE,
                            TrainCoachPlaces.COACH_TYPE, TrainCoachPlaces.COACH_NAME,
                            TrainCoachPlaces.COACH_PLACES
                    }, null, null, null);
            if(cur != null){
                while (cur.moveToNext()) {
                    Bundle requestData = new Bundle();
                    requestData.putString(GetTrainDetailsRequest.STATION_ID_FROM, cur.getString(cur.getColumnIndexOrThrow(TrainSchedule.STATION_ID_FROM)));
                    requestData.putString(GetTrainDetailsRequest.STATION_ID_TILL, cur.getString(cur.getColumnIndexOrThrow(TrainSchedule.STATION_ID_TO)));
                    requestData.putString(GetTrainDetailsRequest.TRAIN, cur.getString(cur.getColumnIndexOrThrow(TrainSchedule.TRAIN_NUM)));
                    requestData.putString(GetTrainDetailsRequest.COACH_TYPE_ID, cur.getString(cur.getColumnIndexOrThrow(TrainCoachPlaces.COACH_TYPE)));

                    final Date startDate = DateTools.fromSQL(cur.getString(cur.getColumnIndexOrThrow(TrainSchedule.DATE_DEPARTURE)));
                    requestData.putString(GetTrainDetailsRequest.DATE_START, DateTools.dateToSec(startDate));
                    List<Coach> execute = new ArrayList<Coach>();
                    try {
                        execute = new GetTrainDetailsRequest().execute(requestData);
                    } catch (AppException e) {
                        AppLog.error(TAG, e.getMessage(), e);
                    } catch (URISyntaxException e) {
                        AppLog.error(TAG, e.getMessage(), e);
                    }

                    requestData.putString(GetTrainDetailsRequest.DATE_START, new SimpleDateFormat("yyyy-MM-dd").format(startDate));
                    for (Coach coach : execute) {
                        requestData.putString(GetCoachPlacesRequest.COACH_NUM, coach.number+"");

                        coach.name = cur.getString(cur.getColumnIndex(TrainCoachPlaces.COACH_NAME));

                        try {
                            coach.places.addAll(new GetCoachPlacesRequest().execute(requestData));
                        } catch (AppException e) {
                            AppLog.error(TAG, e.getMessage(), e);
                        } catch (URISyntaxException e) {
                            AppLog.error(TAG, e.getMessage(), e);
                        }
                        result.add(coach);
                    }
                }
                cur.close();
            }


            return result;
        }

    }

    static class CoachCursorAdapter extends BaseAdapter{

        private Context mContext;
        private List<Coach> items = new ArrayList<Coach>();

        public CoachCursorAdapter(FragmentActivity activity) {
            this.mContext = activity;
        }

        public void addItems(List<Coach> data) {
            items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Coach getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup arg2) {
            return new CoachItemView(mContext).setCoach(getItem(position));
        }
    }

    private final class CoachActionMode implements ActionMode.Callback {

        private String mTrainText;

        public CoachActionMode(String text) {
            mTrainText = text;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {


            menu.add(Menu.NONE, R.id.share, Menu.NONE, R.string.share)
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
                case R.id.share:

                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, mTrainText);
                    startActivity(Intent.createChooser(share, getString(R.string.share_coach_description)));

                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }


    }


}
