package com.life.train.util;

import android.text.TextUtils;

public class Utils {

    public static void logError(String tag, String message, Throwable tr){

    }

    public static boolean isCyrillic(String text) {

        if(TextUtils.isEmpty(text)) return false;

        char[] charArray = text.toCharArray();

        for (char c : charArray) {
            if(Character.UnicodeBlock.CYRILLIC.equals(Character.UnicodeBlock.of(c))){
                return true;
            }
        }
        return false;
    }

}
