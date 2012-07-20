package com.life.train.ui.fragment;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Window;
import com.life.train.R;
import com.life.train.provider.TrainContract.TrainRequest;

public class RequestsListFragment extends SherlockListFragment
implements LoaderManager.LoaderCallbacks<Cursor>{

    // This is the Adapter being used to display the list's data.
    SimpleCursorAdapter mAdapter;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    private OnRequestSelectedListener mListener;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No requests");

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_item_train_request, null,
                new String[] { TrainRequest.STATION_NAME_FROM, TrainRequest.STATION_NAME_TO, TrainRequest.DEPARTURE_DATE, TrainRequest.CREATE_DATE },
                new int[] { R.id.station_from, R.id.station_to, R.id.departure_date, R.id.created_date}, 0);
        setListAdapter(mAdapter);

        getSherlockActivity().setSupportProgress(Window.PROGRESS_END);
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnRequestSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "+OnRequestSelectedListener.class.getSimpleName());
        }
    }

    public void onSearch(String newText) {
        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {

        Uri requestUri = ContentUris.withAppendedId(TrainRequest.CONTENT_URI, id);
        if(mListener!=null) mListener.onRequestSelected(requestUri);

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
        return new CursorLoader(getActivity(), TrainRequest.CONTENT_URI,
                TRAIN_REQUESTA_PROJECTION, null, null,
                TrainRequest.CREATE_DATE + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        mAdapter.swapCursor(data);

        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);

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

    public interface OnRequestSelectedListener{
        void onRequestSelected(Uri requestUri);
    }

}
