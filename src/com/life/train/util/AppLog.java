package com.life.train.util;

import static com.life.train.Const.DEV_MODE;

public class AppLog {

    public static void error(String tag, String msg, Throwable tr){
        if(DEV_MODE){
            android.util.Log.e(tag, msg, tr);
        }
    }

    public static void debug(String tag, String msg){
        if(DEV_MODE){
            android.util.Log.d(tag, msg);
        }
    }

}
