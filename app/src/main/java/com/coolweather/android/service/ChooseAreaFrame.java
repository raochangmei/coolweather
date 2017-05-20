package com.coolweather.android.service;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.coolweather.android.MainActivity;
import com.coolweather.android.R;
import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/5/2.
 */

public class ChooseAreaFrame extends Fragment {
    public static  final int LEVEL_PROVINCE = 0;
    public static  final int LEVEL_CITY = 1;
    public static  final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private Button back_button;
    private TextView title_text;
    private ListView list_view;
    private List<String> dataList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private int currentLevel;
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province select_province;
    private City select_city;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        back_button = (Button)view.findViewById(R.id.back_button);
        title_text = (TextView) view.findViewById(R.id.title_text);
        list_view = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        list_view.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE) {
                    select_province = provinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY) {
                    select_city = cityList.get(position);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActicity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if( getActivity() instanceof WeatherActicity) {

                        WeatherActicity activity = (WeatherActicity) getActivity();
                        activity.drawer.closeDrawers();
                        activity.swipe.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }

                }
            }
        });
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTY) {
                    queryCities();
                }else if(currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 获取城市数据
     */
    public void queryCities() {
        title_text.setText(select_province.getProvinceName());
        back_button.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(select_province.getId())).find(City.class);
        if(cityList.size() > 0) {
            dataList.clear();
            for(City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = select_province.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }

    /**
     * 获取区域数据
     */
    public void queryCounties() {
        title_text.setText(select_city.getCityName());
        back_button.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(select_city.getId())).find(County.class);
        if(countyList.size() >0) {
            dataList.clear();
            for(County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            int provinceCode = select_province.getProvinceCode();
            int cityCode = select_city.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }

    /**
     * 获取省份数据
     */
    public void queryProvinces() {
        title_text.setText("中国");
        back_button.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size() >0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    private void queryFromServer(String address,final String type) {
        HttpUtil.sendOkhttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                showProgressDialog();
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText,select_province.getId());
                }else if("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText,select_city.getId());
                }
                if(result) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)) {
                                queryProvinces();
                            }else if ("city".equals(type)) {
                                queryCities();
                            }else if("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }
    private void showProgressDialog() {
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载中");
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }
    private void closeProgressDialog() {
        if(progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
