package com.coolweather.android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity{

    public DrawerLayout drawerLayout;

    private Button navButton;

    public SwipeRefreshLayout swipeRefresh;

    private String mWeatherId;//用于记录城市的天气id

    private ImageView bingPicImg;

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    //请求天气数据
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 21){
            //拿到当前活动的DecorView
            View decorView = getWindow().getDecorView();
            //使用setSystemUiVisibility方法来改变系统UI
            decorView.setSystemUiVisibility(   //表示活动的布局会显示在状态栏上
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            //将状态栏设置为透明色
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化各种控件
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);//设置下拉刷新进度条的颜色
        bingPicImg = findViewById(R.id.bing_pic_img);
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText =  findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        SharedPreferences prefs = PreferenceManager
                   .getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        //从缓存中获取背景图片
        String bingPic = prefs.getString("bing_pic",null);
        //有缓存直接使用Glide来加载图片
        if(bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
           //调用laodBingPic方法获取图片
           loadBingPic();
        }

        //尝试从本地缓存中读取数据
        if(weatherString != null){
            //有缓存时,直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            //String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);//将 ScrollView 隐藏
            //请求天气数据
            //requestWeather(weatherId);
            requestWeather(mWeatherId);
            //刷新背景图片
            loadBingPic();
        }
        //设置下拉刷新的监听器
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //请求天气
                requestWeather(mWeatherId);
            }
        });
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);//打开滑动菜单
            }
        });
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic(){
         String requestBingPic = "http://guolin.tech/api/bing_pic";
         HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {

             @Override
             public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                //把图片放到缓存中
                SharedPreferences.Editor editor = PreferenceManager.
                        getDefaultSharedPreferences(WeatherActivity.this)
                        .edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                //把线程切换为主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //使用Glide加载图片
                        Glide.with(WeatherActivity.this)
                                .load(bingPic)
                                .into(bingPicImg);
                    }
                });
             }

             @Override
             public void onFailure(Call call, IOException e) {
                 e.printStackTrace();
             }
         });
    }

    /**
     * 根据天气id,请求城市天气信息
     */
    public void requestWeather(final String weatherId) {
        //将传入的天气id和我们申请好的APIKey拼装
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId
                + "&key=a514649c96784e0e964022bf912354f7";

        //向 weatherUrl 发送 请求
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {

            //将 JSON 数据转成 Weather对象
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);

                //将当前线程切换到主线程
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if(weather != null && "ok".equals(weather.status)){
                           SharedPreferences.Editor editor =  PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this)
                                   .edit();
                           //将数据存储到SharedPreferences
                           editor.putString("weather",responseText);
                           editor.apply();
                           mWeatherId = weather.basic.weatherId;
                           showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this,
                                    "获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);//刷新事件结束,并隐藏刷新进度条
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败"
                                ,Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    /**
     * 处理并展示 Weather 实体类中的数据
     */
    private void showWeatherInfo(Weather weather) {

         String cityName = weather.basic.cityName;
         String updateTime =  weather.basic.update.updateTime.split(" ")[1];
         String degree = weather.now.temperature + "℃";
         String weatherInfo = weather.now.more.info;
         //从 Weather 对象中获取数据,然后显示到相应的控件上
         titleCity.setText(cityName);
         titleUpdateTime.setText(updateTime);
         degreeText.setText(degree);
         weatherInfoText.setText(weatherInfo);
         forecastLayout.removeAllViews();
         //显示 未来几天天气预报
         for (Forecast forecast : weather.forecastList){
             //动态 forecast_item布局,并设置相应的数据,然后添加到父布局中
             View view = LayoutInflater.from(this).
                     inflate(R.layout.forecast_item,
                     forecastLayout,false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
         }
         if(weather.aqi != null){
             aqiText.setText(weather.aqi.city.aqi);
             pm25Text.setText(weather.aqi.city.pm25);
         }
         String comfort = "舒适度: " + weather.suggestion.comfort.info;
         String carWash = "洗车指数: " + weather.suggestion.carWash.info;
         String  sport = "运动建议: " + weather.suggestion.sport.info;
         comfortText.setText(comfort);
         carWashText.setText(carWash);
         sportText.setText(sport);
         weatherLayout.setVisibility(View.VISIBLE);//将ScrollView变成可见
         //激活服务
         Intent intent = new Intent(this, AutoUpdateService.class);
         startService(intent);
    }

}
