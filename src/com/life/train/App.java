package com.life.train;

import java.util.Locale;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;


@ReportsCrashes(formKey = "dE1PTDY2UHpkLTFqZDMzVURGRmJDVVE6MQ",
mode = ReportingInteractionMode.TOAST,
forceCloseDialogAfterToast = false, // optional, default false
resToastText = R.string.crash_toast_text)
public class App extends Application {

    private static App instance;

    private String localeEn = "en";
    private String localeRu = "ru";

    public synchronized static Context getContext(){
        return instance;
    }

    public synchronized static App get(){
        return instance;
    }

    public String getLocale(){
        return localeEn;
    }

    @Override
    public void onCreate() {
        instance = this;
        ACRA.init(this);
        super.onCreate();

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Editor edit = pref.edit();
        edit.putBoolean("acra.enable", !Const.DEV_MODE);
        edit.commit();

    }

    public static String getLang() {
        final String locale = Locale.getDefault().getLanguage();
        if("uk".equals(locale)){
            return Const.LANG_UA;
        }else if("ru".equals(locale)) {
            return Const.LANG_RU;
        }else {
            return Const.LANG_EN;
        }
    }

}
