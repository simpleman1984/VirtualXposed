package io.virtualapp.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.lody.virtual.sandxposed.XposedModuleProfile;

import io.island.R;
import io.virtualapp.abs.ui.VActivity;

/**
 * 百度地图官方手册
 * http://lbsyun.baidu.com/index.php?title=androidsdk/guide/tool/coordinate
 */
public class BaiduMapActivity extends VActivity {

    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();
    protected String mLastCity = "";

    private TextView currentFakeGps;
    private MapView mMapView = null;
    private BaiduMap mBaiduMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        //设置内容
        setContentView(R.layout.activity_location_settings);
        initMap();
        initLocationAndOption();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fake_menu, menu);
        menu.findItem(R.id.current_pos).setTitle(Html.fromHtml("<font color='#ff3824'>定位</font>"));
        menu.findItem(R.id.clear_fakeGps).setTitle(Html.fromHtml("<font color='#ff3824'>重置</font>"));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.current_pos:
                Log.e("X","定位到当前位置~~");
                //手动定位当前的百度地图位置
                mLocationClient.registerLocationListener(myListener);
                mLocationClient.start();
                mLocationClient.requestLocation();
                break;
            case R.id.clear_fakeGps:
                XposedModuleProfile.fakeLatitude(-1d);
                XposedModuleProfile.fakeLongitude(-1d);
                updateCurrentFakeGpsView();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initMap(){
        currentFakeGps = findViewById(R.id.currentFakeGps);
        mMapView = findViewById(R.id.baidu_map_view);
        mBaiduMap = mMapView.getMap();

        //获取屏幕高度宽度
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);

        //去掉logo
        mMapView.removeViewAt(1);
        // 设置启用内置的缩放控件
        mMapView.showZoomControls(true);

        // 设置地图模式为交通地图
        mBaiduMap.setTrafficEnabled(true);

        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);

        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus, int i) {

            }

            @Override
            public void onMapStatusChangeStart(MapStatus arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onMapStatusChangeFinish(MapStatus arg0) {
                // TODO Auto-generated method stub
                LatLng target = mBaiduMap.getMapStatus().target;
                System.out.println(target.toString());
                //mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(target));
            }

            @Override
            public void onMapStatusChange(MapStatus arg0) {
                // TODO Auto-generated method stub

            }
        });


        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                updatePosition(latLng, false);
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                showPositionInfo(mapPoi.getPosition(), mapPoi.getName());
                return false;
            }
        });

        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                showPositionInfo(latLng, "");
            }
        });


    }

    private void initLocationAndOption() {
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类

        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//设置定位模式
        option.setOpenGps(true); //是否打开gps
        option.setCoorType("bd09ll");//返回的定位结果是百度经纬度,默认值gcj02

        //当所设的整数值大于等于1000（ms）时，定位SDK内部使用定时定位模式。调用requestLocation( )后，每隔设定的时间，定位SDK就会进行一次定位。
        //当不设此项，或者所设的整数值小于1000（ms）时，采用一次定位模式。
        option.setScanSpan(5000);//设置发起定位请求的间隔时间为5000ms
        option.setIsNeedAddress(true);//返回的定位结果包含地址信息
        option.setNeedDeviceDirect(true);//返回的定位结果包含手机机头的方向
        mLocationClient.setLocOption(option);

        double latitude = XposedModuleProfile.fakeLatitude();
        double longitude = XposedModuleProfile.fakeLongitude();
        updateCurrentFakeGpsView();
        if (latitude < 0 || longitude < 0) {
            //自动定位
            mLocationClient.registerLocationListener(myListener);    //注册监听函数
            mLocationClient.start();
            mLocationClient.requestLocation();
        } else {
            //定位到上次选择的位置
            updatePosition(new LatLng(latitude, longitude), true);
        }
    }

    protected void showPositionInfo(final LatLng latLng, String posName) {
        updatePosition(latLng, false);

        //保存地图选点并返回
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        builder.setTitle(posName);
        builder.setMessage(latLng.toString());
        builder.setNegativeButton("取消", null);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                LatLng desLatLng = MapConvert.convertBaiduToGPS(latLng);
                double latitude = latLng.latitude;
                double longitude = latLng.longitude;
                XposedModuleProfile.fakeLatitude(latitude);
                XposedModuleProfile.fakeLongitude(longitude);
                updateCurrentFakeGpsView();
            }
        });
        builder.create().show();
    }

    protected void updatePosition(LatLng latLng, boolean reCenter) {
        mBaiduMap.clear();

        // 只是完成了定位
        MyLocationData locData = new MyLocationData.Builder().latitude(latLng.latitude)
                .longitude(latLng.longitude).build();

        //设置图标在地图上的位置
        mBaiduMap.setMyLocationData(locData);

        if (reCenter == true) {
//            //获得百度地图状态
//            MapStatus.Builder builder = new MapStatus.Builder();
//            builder.target(latLng);
//            //设置缩放级别 16对应比例尺200米
//            builder.zoom(16);
//            MapStatus mapStatus = builder.build();
//            MapStatusUpdate m = MapStatusUpdateFactory.newMapStatus(mapStatus);
//            mBaiduMap.setMapStatus(m);

            // 开始移动百度地图的定位地点到中心位置
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(latLng, 16);
            mBaiduMap.animateMapStatus(u);
        }
    }

    class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                Toast.makeText(getApplicationContext(), "获取位置信息失败", Toast.LENGTH_LONG).show();
                return;
            }

            mLastCity = location.getCity();
            // 只是完成了定位
            MyLocationData locData = new MyLocationData.Builder().latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();

            //设置图标在地图上的位置
            mBaiduMap.setMyLocationData(locData);


            // 开始移动百度地图的定位地点到中心位置
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 16.0f);
            mBaiduMap.animateMapStatus(u);

            //定位成功后关闭定位
            mLocationClient.stop();
            //取消监听函数。
            mLocationClient.unRegisterLocationListener(myListener);
        }

    }

    private void updateCurrentFakeGpsView(){
        double longitude = XposedModuleProfile.fakeLongitude();
        double latitude = XposedModuleProfile.fakeLatitude();
        String txt = "虚拟经度：" + longitude + ",虚拟纬度：" + latitude;
        if(latitude < 0 || longitude < 0){
            txt = "暂未设置虚拟地址";
        }
        currentFakeGps.setText(txt);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mLocationClient.stop();
        //mSearch.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

}
