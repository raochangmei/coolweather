package com.coolweather.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWrather();
        updateBingPic();

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHouer = 8*60*60*1000;
        long triggerAtTime = SystemClock.elapsedRealtime()+anHouer;
        Intent intent1 = new Intent(this,AutoUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this,0,intent1,0);
        manager.cancel(pendingIntent);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pendingIntent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateBingPic() {
        String requestBingPIc= "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkhttpRequest(requestBingPIc, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                edit.putString("bing_pic",bingPic);
                edit.apply();
            }
        });
    }

    private void updateWrather() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String weather = sp.getString("weather", null);
        if(weather != null) {
            Weather weather1 = Utility.handleWeatherResponse(weather);
            String weatherId = weather1.basic.weatherId;
            String weatherurl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=b95a57a437d54fee8da7a3f8bc0494fb";
            HttpUtil.sendOkhttpRequest(weatherurl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseText = response.body().string();
                    final Weather weather = Utility.handleWeatherResponse(responseText);
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        edit.putString("weather", responseText);
                        edit.apply();
                    }
                }
            });
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
