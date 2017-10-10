package com.gm.sysinfo;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
//import java.util.logging.Logger;

import org.apache.http.conn.util.InetAddressUtils;
import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxLuaJavaBridge;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.gm.service.RestartService;
import com.gm.service.InstallService;
import com.gm.utils.Logger;


public class sysinfo {
    //-----------------------------------------------------
    public static int m_battery = 0;
    //-----------------------------------------------------
    //-----------------------------------------------------
    public static Cocos2dxActivity m_context;
    public static PackageManager m_pm = null;
    public static PackageInfo m_pkginfo = null;
    public static ApplicationInfo m_appinfo = null;
    public static BroadcastReceiver m_broadcastReceiver = null;

    public static void init(Cocos2dxActivity context) {
        m_context = context;
        m_pm = m_context.getPackageManager();
        //获取包信息
        try {
            m_pkginfo = m_pm.getPackageInfo(m_context.getPackageName(), 0);
            m_appinfo = m_pkginfo.applicationInfo;
        } catch (Exception e) {

        }
        listenbattary();
    }

    //-----------------------------------------------------
    //0 no
    //1 4g
    //2 wifi
    //3 other
    //网络状态
    public static int netstate() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) m_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo != null && mNetworkInfo.isAvailable()) {
            int t = mNetworkInfo.getType();
            if (ConnectivityManager.TYPE_MOBILE == t) {
                return 1;
            } else if (ConnectivityManager.TYPE_WIFI == t) {
                return 2;
            }
            return 3;
        }
        return 0;
    }

    //电池电量0-100
    public static int batteryinfo() {
        return m_battery;
    }

    //获取imsi
    public static String imsi() {
        return "0000";
    }

    //获取imei
    public static String imei() {
        String imeiStr = "";
        try {
            imeiStr = ((TelephonyManager) m_context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (imeiStr == null || imeiStr.length() == 0) {
            imeiStr = macaddress();
        }

        if (imeiStr == null || imeiStr.length() == 0) {
            Random r = new Random(System.currentTimeMillis());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 16; i++) {
                int b = r.nextInt(9);
                sb.append(b);
            }
            imeiStr = sb.toString();
        }
        return imeiStr;
    }

    //ip地址
    public static String ipaddress() {
        int st = netstate();
        if (0 == st) {
            return "";
        } else if (2 == st) {
            return getWifiIpAddress();
//            return getLocalIpAddress();
        } else {
            return getLocalIpAddress();
        }
    }

    //获取mac地址
    public static String macaddress() {
        String macAddress = "";
        try {
            WifiManager wifi = (WifiManager) m_context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            if (info == null)
                return macAddress;
            macAddress = info.getMacAddress();
            if (macAddress.length() > 0) {
                macAddress = macAddress.replace(":", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return macAddress;
    }

    //获取手机型号
    public static String mobilemodel() {
        return android.os.Build.MODEL;
    }

    //获取系统版本号
    public static String systemversion() {
        return String.valueOf(android.os.Build.VERSION.SDK_INT);
    }

    //获取应用包名
    public static String packagename() {
        return m_context.getPackageName();
    }

    //获取应用名称
    public static String appname() {
        int labelRes = m_appinfo.labelRes;
        return m_context.getResources().getString(labelRes);
    }

    //获取应用版本
    public static String appversion() {
        return m_pkginfo.versionName;
    }

    //国家
    public static String country() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = m_context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = m_context.getResources().getConfiguration().locale;
        }
        return locale.getCountry();
    }

    //下载地址
    public static String downloadurl() {
        return "";
    }

    //是否安装了某个app
    public static boolean isinstall(String name) {
        android.content.pm.PackageInfo packageInfo;
        try {
            packageInfo = m_context.getPackageManager().getPackageInfo(name, 0);

        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        if(packageInfo ==null){
            System.out.println("not installed");
            return false;
        }else{
            System.out.println("is installed");
            return true;
        }
    }
    //安装apk
    public static void installapp(final String filepath) {
        m_context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                File file = new File(filepath);
                intent.setDataAndType(Uri.fromFile(file),
                        "application/vnd.android.package-archive");
                m_context.startActivity(intent);
            }
        });
    }

    //打开app
    public static void openapp(final String name) {
        m_context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean flag = isinstall(name);
                if(flag)
                {
                    android.content.pm.PackageManager packageManager = m_context.getPackageManager();
                    Intent intent = packageManager.getLaunchIntentForPackage(name);
                    m_context.startActivity(intent);
                } else {
                    Toast.makeText(m_context, "没有安装" + name, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //开机时间秒
    public static long elapsedtime() {
        //可以直接从lua调用
        //public static native long elapsedRealtime();
        return SystemClock.elapsedRealtime() / 1000;
    }

    //-------------------
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("local IpAddress", ex.toString());
        }
        return "";
    }
//    public static String getLocalIpAddress() {
//        try {
//            String ipv4;
//            List<NetworkInterface> nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
//            for (NetworkInterface ni : nilist) {
//                List<InetAddress> ialist = Collections.list(ni.getInetAddresses());
//                for (InetAddress address : ialist) {
//                    if (!address.isLoopbackAddress() &&
//                            InetAddressUtils.isIPv4Address(ipv4 = address.getHostAddress())) {
//                        return ipv4;
//                    }
//                }
//            }
//        } catch (SocketException ex) {
//            ex.printStackTrace();
//        }
//        return "";
//    }

    private static String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }

    public static String getWifiIpAddress() {

        WifiManager wifiManager = (WifiManager) m_context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
//        	wifiManager.setWifiEnabled(true); 
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            return intToIp(ipAddress);
        }
        return "";
    }

    //电量检测
    public static void listenbattary() {
        if (m_broadcastReceiver != null) {
            return;
        }
        // 声明广播接受者对象
        m_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                    // 得到电池状态：
                    // BatteryManager.BATTERY_STATUS_CHARGING：充电状态。
                    // BatteryManager.BATTERY_STATUS_DISCHARGING：放电状态。
                    // BatteryManager.BATTERY_STATUS_NOT_CHARGING：未充满。
                    // BatteryManager.BATTERY_STATUS_FULL：充满电。
                    // BatteryManager.BATTERY_STATUS_UNKNOWN：未知状态。
                    int status = intent.getIntExtra("status", 0);
                    // 得到健康状态：
                    // BatteryManager.BATTERY_HEALTH_GOOD：状态良好。
                    // BatteryManager.BATTERY_HEALTH_DEAD：电池没有电。
                    // BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE：电池电压过高。
                    // BatteryManager.BATTERY_HEALTH_OVERHEAT：电池过热。
                    // BatteryManager.BATTERY_HEALTH_UNKNOWN：未知状态。
                    int health = intent.getIntExtra("health", 0);
                    // boolean类型
                    boolean present = intent.getBooleanExtra("present", false);
                    // 得到电池剩余容量
                    int level = intent.getIntExtra("level", 0);

                    m_battery = level;

                    // 得到电池最大值。通常为100。
                    int scale = intent.getIntExtra("scale", 0);
                    // 得到图标ID
                    int icon_small = intent.getIntExtra("icon-small", 0);
                    // 充电方式：　BatteryManager.BATTERY_PLUGGED_AC：AC充电。　BatteryManager.BATTERY_PLUGGED_USB：USB充电。
                    int plugged = intent.getIntExtra("plugged", 0);
                    // 得到电池的电压
                    int voltage = intent.getIntExtra("voltage", 0);
                    // 得到电池的温度,0.1度单位。例如 表示197的时候，意思为19.7度
                    int temperature = intent.getIntExtra("temperature", 0);
                    // 得到电池的类型
                    String technology = intent.getStringExtra("technology");
                    // 得到电池状态
                    String statusString = "";
                    // 根据状态id，得到状态字符串
                    switch (status) {
                        case BatteryManager.BATTERY_STATUS_UNKNOWN:
                            statusString = "unknown";
                            break;
                        case BatteryManager.BATTERY_STATUS_CHARGING:
                            statusString = "charging";
                            break;
                        case BatteryManager.BATTERY_STATUS_DISCHARGING:
                            statusString = "discharging";
                            break;
                        case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                            statusString = "not charging";
                            break;
                        case BatteryManager.BATTERY_STATUS_FULL:
                            statusString = "full";
                            break;
                    }
                    //得到电池的寿命状态
                    String healthString = "";
                    //根据状态id，得到电池寿命
                    switch (health) {
                        case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                            healthString = "unknown";
                            break;
                        case BatteryManager.BATTERY_HEALTH_GOOD:
                            healthString = "good";
                            break;
                        case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                            healthString = "overheat";
                            break;
                        case BatteryManager.BATTERY_HEALTH_DEAD:
                            healthString = "dead";
                            break;
                        case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                            healthString = "voltage";
                            break;
                        case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                            healthString = "unspecified failure";
                            break;
                    }
                    //得到充电模式
                    String acString = "";
                    //根据充电状态id，得到充电模式
                    switch (plugged) {
                        case BatteryManager.BATTERY_PLUGGED_AC:
                            acString = "plugged ac";
                            break;
                        case BatteryManager.BATTERY_PLUGGED_USB:
                            acString = "plugged usb";
                            break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        m_context.registerReceiver(m_broadcastReceiver, filter);
    }

    public static void unRegisterReceiver() {
        if (m_broadcastReceiver != null) {
            m_context.unregisterReceiver(m_broadcastReceiver);
            m_broadcastReceiver = null;
        }
    }

    public static String metadata(String key)
    {
        try {
            ApplicationInfo ai = m_pm.getApplicationInfo(m_context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            String myApiKey = bundle.getString(key);
            if(myApiKey==null)
            {
//                int v = bundle.getInt(key,-999);
//                if(v!=-999)
//                {
//                    myApiKey = String.valueOf(v);
//                }
                  Object o = bundle.get(key);
                  if(o!=null)
                  {
                      myApiKey = String.valueOf(o);
                  }
            }
            return myApiKey;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e("Failed to load meta-data, NameNotFound" + e.getMessage());
        } catch (NullPointerException e) {
            Logger.e("Failed to load meta-data, NullPointer: " + e.getMessage());
        }
        return null;
    }

    public static double availmem() {
        ActivityManager acmng = (ActivityManager) m_context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if(memoryInfo!=null) {
            //获得系统可用内存，保存在MemoryInfo对象上
            acmng.getMemoryInfo(memoryInfo);
            long memSize = memoryInfo.availMem;
            return memSize / (1024 * 1024);
        }
        return 0;
    }
    public static double totalmem()
    {
        ActivityManager acmng = (ActivityManager) m_context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if(memoryInfo!=null) {
            //获得系统可用内存，保存在MemoryInfo对象上
            acmng.getMemoryInfo(memoryInfo);
            long memSize = memoryInfo.totalMem;
            return memSize / (1024 * 1024);
        }
        return 0;
    }
    public static void test() {
        Logger.ffkk();
        Logger.d("netstate:" + String.valueOf(netstate()));
        Logger.d("batteryinfo:" + String.valueOf(batteryinfo()));
        Logger.d("imei:" + imei());
        Logger.d("ipaddress:" + ipaddress());
        Logger.d("macaddress:" + macaddress());
        Logger.d("mobilemodel:" + mobilemodel());
        Logger.d("systemversion:" + systemversion());
        Logger.d("packagename:" + packagename());
        Logger.d("appname:" + appname());
        Logger.d("appversion:" + appversion());
        Logger.d("country:" + country());
        Logger.d("availmem:" + String.valueOf(availmem()));
        Logger.d("totalmem:" + String.valueOf(totalmem()));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 重启整个APP
     * @param context
     * @param Delayed 延迟多少毫秒
     */
    public static void restartAPP_Ex(Context context,long Delayed){

//        /**开启一个新的服务，用来重启本APP*/
//        Intent intent1=new Intent(context,RestartService.class);
//        intent1.putExtra("PackageName",context.getPackageName());
//        intent1.putExtra("Delayed",Delayed);
//        intent1.putExtra("Appid",android.os.Process.myPid());
//        context.startService(intent1);
//        /**杀死整个进程**/
//        android.os.Process.killProcess(android.os.Process.myPid());

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent restartIntent = PendingIntent.getActivity(context.getApplicationContext(), 11223344, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 150, restartIntent);
        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /***重启整个APP*/
    public static void restartAPP(){
        m_context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                restartAPP_Ex(m_context, 2000);
            }
        });
    }

    public static void installAPP_Ex(String filepath,int killself){

        /**开启一个新的服务，用来重启本APP*/
        Intent intent1=new Intent(m_context,InstallService.class);
        intent1.putExtra("FileName",filepath);
        intent1.putExtra("Delayed",500);
        m_context.startService(intent1);


        if(killself>0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, 5000);
        }

        /**杀死整个进程**/
//        if(killself>0) {
//            android.os.Process.killProcess(android.os.Process.myPid());
//        }
    }
    /***安装整个APP*/
    public static void installAPP(final String filepath,final int killself) {
        m_context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                installAPP_Ex(filepath,killself);
            }
        });
    }
}

