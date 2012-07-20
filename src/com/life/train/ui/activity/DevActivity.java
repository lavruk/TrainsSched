package com.life.train.ui.activity;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.life.train.R;
import com.life.train.provider.TrainDatabase;
import com.life.train.util.AppLog;

public class DevActivity extends SherlockActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev);

        ((EditText)findViewById(R.id.dev_query))
        .setText("DROP TRIGGER IF EXISTS station_search_insert");
    }


    public void onClick(View v){
        switch (v.getId()) {
            case R.id.dev_load_html:
                loadHtml();
                break;
            case R.id.dev_run_sql:

                String query = ((EditText)findViewById(R.id.dev_query)).getText().toString();
                TrainDatabase helper = TrainDatabase.getHelper(this);
                SQLiteDatabase db = helper.getDB();
                //                try {
                //                    db.execSQL(query);
                //                } catch (Exception e) {
                //                    new AlertDialog.Builder(this).setMessage(e.toString()).create().show();
                //                }
                //
                //                TrainDatabase.deleteSearchTrigers(db);
                //                TrainDatabase.createSearchTrigers(db);

                helper.onUpgrade(db, 2, 3);

                break;

            default:
                break;
        }
    }


    private void loadHtml() {

        TableLayout root = new TableLayout(this);
        root.setStretchAllColumns(true);

        String bodyHtml = "";


        Document doc = null;
        try {
            doc = Jsoup.parse(getAssets().open("s1.html"), "utf-8", "");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(doc!=null){
            //        Document doc = Jsoup.parseBodyFragment(bodyHtml);
            Element schema = doc.getElementById("ts_chs_scheme");
            Elements coaches = schema.children();

            TableRow row1 = new TableRow(this);
            row1.setPadding(1, 1, 1, 1);
            root.addView(row1);

            int b = 0;
            for (Element coach : coaches) {
                if(coach.tagName().equals("div")){
                    TableRow row2 = new TableRow(this);
                    row2.setPadding(1, 1, 1, 1);
                    root.addView(row2);
                    b +=1;
                }else{
                    LinearLayout coachLayout = new LinearLayout(this);

                    LinearLayout left = new LinearLayout(this);
                    left.setPadding(1, 1, 1, 1);
                    left.setOrientation(LinearLayout.VERTICAL);
                    left.setBackgroundColor(Color.LTGRAY);
                    coachLayout.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));

                    LinearLayout right = new LinearLayout(this);
                    right.setPadding(1, 1, 1, 1);
                    right.setOrientation(LinearLayout.VERTICAL);
                    right.setBackgroundColor(Color.YELLOW);
                    coachLayout.addView(right, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));


                    Elements places = coach.children();
                    for (int i = 0; i < places.size(); i++) {
                        Element place = places.get(i);

                        TextView tw = new TextView(this);
                        tw.setText(place.text());
                        tw.setTextSize(12);
                        if(place.className().equals("upper")){
                            tw.setTextColor(Color.BLUE);
                        }
                        if(place.className().equals("lower")){
                            tw.setTextColor(Color.GREEN);
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


        ((ViewGroup)findViewById(R.id.dev_load_html).getParent()).addView(root);
    }




}
