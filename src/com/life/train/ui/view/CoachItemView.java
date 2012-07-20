package com.life.train.ui.view;

import java.text.NumberFormat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.life.train.R;
import com.life.train.entry.Coach;
import com.life.train.util.AppLog;

public class CoachItemView extends LinearLayout{

    private static final String TAG = CoachItemView.class.getSimpleName();

    private Context mContext;

    private TextView mCoachNumberView;
    private TextView mCoachPriceView;

    private TextView mCoachNameView;


    public CoachItemView(Context context) {
        super(context);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.list_item_coach, this, true);

        mCoachNameView = (TextView) findViewById(R.id.name);
        mCoachNumberView = (TextView) findViewById(R.id.coach_number);
        mCoachPriceView = (TextView) findViewById(R.id.price);


    }

    public CoachItemView setCoach(Coach coach) {

        mCoachNameView.setText(coach.name);
        mCoachNumberView.setText(mContext.getString(R.string.train_coach_num, coach.number));
        mCoachPriceView.setText(mContext.getString(R.string.train_coach_price, NumberFormat.getInstance().format((float)(coach.price) /100)));


        ViewGroup placesContainer = (ViewGroup)findViewById(R.id.coach_container);
        if(placesContainer.getChildCount()>0) placesContainer.removeAllViews();
        placesContainer.addView(loadDefaultCoach(coach));



        //        new AsyncTask<Coach, Void, View>(){
        //
        //            @Override
        //            protected View doInBackground(Coach... params) {
        //                View v = null;
        //                try {
        //                    v = loadCoach(params[0]);
        //                } catch (Exception e) {
        //                    AppLog.error(TAG, e.getMessage(), e);
        //                    v = loadDefaultCoach(params[0]);
        //                }
        //                return v;
        //            }
        //
        //            @Override
        //            protected void onPostExecute(View result) {
        //                if(result != null){
        //                    ((ViewGroup)findViewById(R.id.coach_container)).addView(result);
        //                }
        //            };
        //
        //        }.execute(coach);



        return this;
    }

    public View loadCoach(Coach coach){

        switch (coach.coachTypeId) {

            case 1:
            case 2:
            case 3:
            case 4:
                return loadKupeCoach(coach);
            case 7:
            case 8:
                return loadSCoach(coach);
            default:
                return loadDefaultCoach(coach);
        }
    }

    private View loadSCoach(Coach coach) {

        TableLayout root = new TableLayout(mContext);
        root.setStretchAllColumns(true);

        TableRow row = new TableRow(mContext);
        root.addView(row);

        Document doc = Jsoup.parse(coach.content);
        if(doc!=null){

            Element schema = doc.getElementById("ts_chs_scheme");
            Elements coaches = schema.children();


            for (Element cupe : coaches) {

                LinearLayout coachLayout = new LinearLayout(mContext);
                coachLayout.setBackgroundColor(Color.LTGRAY);
                coachLayout.setOrientation(LinearLayout.VERTICAL);
                coachLayout.setPadding(2, 2, 2, 2);

                Elements places = cupe.children();
                for (int i = 0; i < places.size(); i++) {
                    Element place = places.get(i);

                    if(!place.tagName().equals("a")){
                        continue;
                    }

                    TextView tw = new TextView(mContext);
                    tw.setText(place.text());
                    tw.setTextSize(12);
                    if(!coach.places.contains(Integer.parseInt(place.text()))){
                        tw.setTextColor(Color.parseColor("#CCCCCC"));
                    }

                    coachLayout.addView(tw, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                    AppLog.debug("html", place.text()+": "+place.className());
                }

                if (coachLayout.getChildCount() > 0)
                    row.addView(coachLayout, new LinearLayout.LayoutParams(
                            LayoutParams.MATCH_PARENT, mContext.getResources()
                            .getDimensionPixelSize(R.dimen.min_coach_height)));
            }
        }

        return root;
    }

    private View loadKupeCoach(Coach coach) {

        TableLayout root = new TableLayout(mContext);
        root.setStretchAllColumns(true);

        Document doc = Jsoup.parse(coach.content);
        if(doc != null){
            Element schema = doc.getElementById("ts_chs_scheme");
            Elements coaches = schema.children();

            TableRow row1 = new TableRow(mContext);
            row1.setPadding(1, 1, 1, 1);
            root.addView(row1);

            int b = 0;
            for (Element cupe : coaches) {
                if(cupe.tagName().equals("div")){
                    TableRow row2 = new TableRow(mContext);
                    row2.setPadding(1, 1, 1, 1);
                    root.addView(row2);
                    b +=1;
                }else{
                    LinearLayout coachLayout = new LinearLayout(mContext);

                    LinearLayout left = new LinearLayout(mContext);
                    left.setPadding(1, 1, 1, 1);
                    left.setOrientation(LinearLayout.VERTICAL);
                    left.setBackgroundColor(Color.LTGRAY);
                    coachLayout.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));

                    LinearLayout right = new LinearLayout(mContext);
                    right.setPadding(1, 1, 1, 1);
                    right.setOrientation(LinearLayout.VERTICAL);
                    right.setBackgroundColor(Color.YELLOW);
                    coachLayout.addView(right, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));


                    Elements places = cupe.children();
                    for (int i = 0; i < places.size(); i++) {
                        Element place = places.get(i);

                        if(!place.tagName().equals("a")){
                            continue;
                        }
                        String placeText = place.text();

                        TextView tw = new TextView(mContext);
                        tw.setText(placeText);
                        tw.setTextSize(12);
                        if(place.className().equals("upper")){
                            //                            tw.setTextColor(Color.BLUE);
                        }
                        if(place.className().equals("lower")){
                            //                            tw.setTextColor(Color.GREEN);
                        }

                        try {
                            if(!coach.places.contains(Integer.parseInt(placeText))){
                                tw.setTextColor(Color.parseColor("#CCCCCC"));
                            }
                        } catch (Exception e) {
                            AppLog.error(TAG, e.getMessage(), e);
                        }


                        if(i%2==0){
                            ((ViewGroup)coachLayout.getChildAt(1)).addView(tw);
                        }else{
                            ((ViewGroup)coachLayout.getChildAt(0)).addView(tw);
                        }
                        AppLog.debug("html", "block: "+b+", "+place.text()+": "+place.className());
                    }

                    ((ViewGroup)root.getChildAt(b)).addView(coachLayout);

                }
            }

        }
        return root;
    }

    public View loadDefaultCoach(Coach coach){
        TextView v = new TextView(mContext);
        v.setText(mContext.getString(R.string.train_places_free, coach.placesCount) + " " + coach.places.toString());
        return v;
    }


}
