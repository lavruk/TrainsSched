package com.life.train.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.life.train.App;
import com.life.train.Const;

public class DateTools {

    private static final SimpleDateFormat mIso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DateFormat = new SimpleDateFormat("EEE, dd.MM.yyyy");
    private static final SimpleDateFormat TimeFormat = new SimpleDateFormat("HH:mm");

    public static final SimpleDateFormat RequestDateFormatCyr = new SimpleDateFormat("dd.MM.yyyy");
    public static final SimpleDateFormat RequestDateFormatEn = new SimpleDateFormat("MM.dd.yyyy");


    public static Date fromSQL(String data){
        Date result = null;
        try {
            result = mIso8601DateFormat.parse(data);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String toSQL(Date date){
        return mIso8601DateFormat.format(date);
    }

    public static String dateToRequest(Date date){
        if(App.getLang().equals(Const.LANG_EN)){
            return RequestDateFormatEn.format(date);
        }else{
            return RequestDateFormatCyr.format(date);
        }
    }

    public static String time(Date date){
        return TimeFormat.format(date);
    }

    public static String dateToSec(Date date){
        return Long.toString(date.getTime()/1000);
    }

}
