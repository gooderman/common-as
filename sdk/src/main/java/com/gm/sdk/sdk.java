package com.gm.sdk;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.io.File;

import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxHelper;
import org.cocos2dx.lib.Cocos2dxLuaJavaBridge;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;

import com.gm.sysinfo.sysinfo;
import com.gm.utils.Logger;
import com.umeng.socialize.UMShareAPI;

import com.czt.mp3recorder.MP3Recorder;
import com.czt.mp3recorder.RecorderStateListener;

import com.gm.baiduloc.BaiduLoc;
import com.gm.baiduloc.BaiduLocListener;

public class sdk implements RecorderStateListener,BaiduLocListener {
    //-----------------------------------------------------
    public static String wxkey = "wx3c59eadee6944071";
    public static String wxsecret = "a245fefb4b5d93ecd354232ba1c4e351";
    public static String wbkey = "";
    public static String wbsecret = "";
    public static String qqkey = "";
    public static String qqsecret = "";
    //-----------------------------------------------------
    public static String SDK_ERROR = "error";
    public static String SDK_EVT = "evt";
    public static String SDK_EVT_LOGIN = "login";
    public static String SDK_EVT_SHARE = "share";
    public static String SDK_OPENID = "openid";
    public static String SDK_NAME = "name";
    public static String SDK_ICONURL = "iconurl";
    public static String SDK_GENDER = "gender";
    public static String SDK_ACCESS_TOKEN = "accessToken";

    //----share----
    public static String SDK_SHARE_TYPE = "type";
    public static String SDK_SHARE_TITLE = "title";
    public static String SDK_SHARE_TEXT = "text";
    public static String SDK_SHARE_IMAGE = "image";
    public static String SDK_SHARE_URL = "url";
    //----wx-pay----
    public static String SDK_EVT_WXPAY = "wxpay";
    public static String SDK_PRICE = "price";

    // --- 录音 ----
    public static String SDK_EVT_RECORD = "record";
    public static String SDK_RECORD_FILENAME = "filename";
    public static String SDK_RECORD_STATE = "state";

    // --- 定位 ----
    public static String SDK_EVT_LOCATION = "locate";
    public static String SDK_LOCATION_LONGITUDE = "longitude";
    public static String SDK_LOCATION_LATITUDE = "latitude";
    public static String SDK_LOCATION_ADDRESS = "address";
    public static String SDK_LOCATION_ADDRESS_DESCRIBE = "discribe";
    public static String SDK_LOCATION_ADDRESS_COUNTY = "county";
    public static String SDK_LOCATION_ADDRESS_PROVINCE = "province";
    public static String SDK_LOCATION_ADDRESS_CITY = "city";
    public static String SDK_LOCATION_ADDRESS_DISTRICT = "district";
    public static String SDK_LOCATION_ADDRESS_STREET = "street";
    public static String SDK_LOCATION_ADDRESS_STREETNUMBER = "streetnumber";
    public static String SDK_LOCATION_ADDRESS_DETAIL = "detail";
    //-----------------------------------------------------
    // --- config ----
    public static HashMap<String, String> gMap;

    public static String TOKEN_UM_APPKEY = "umappkey";
    public static String TOKEN_WX_APPKEY = "wxappkey";
    public static String TOKEN_WX_APPSECRET = "wxappsecret";
    public static String TOKEN_BD_LOCKEY = "baidulockey";
    //-----------------------------------------------------
    // --- clipboard ---
    private static android.content.ClipboardManager m_ClipboardManager11;
    private static android.text.ClipboardManager m_ClipboardManager10;
    //-----------------------------------------------------

    public static Cocos2dxActivity m_context;

    public static MP3Recorder mRecorder;

    private static sdk instance;

    private sdk() {
        super();
        instance = this;
    }

    public static sdk getInstance() {
        if (instance == null) {
            instance = new sdk();
        }
        return instance;
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        sdk_um.onActivityResult(requestCode, resultCode, data);
    }

    public static void onDestroy() {
        if (mRecorder != null) {
            mRecorder.stop();
        }
    }


    @Override
    public void onRecorderState(String state, HashMap<String, Object> data) {
        String fileName = (String) data.get("filename");
        if (fileName == null) {
            fileName = "";
        }
        HashMap<String, Object> nmap = new HashMap<String, Object>();
        nmap.put(sdk.SDK_EVT, sdk.SDK_EVT_RECORD);
        nmap.put(sdk.SDK_RECORD_STATE, state);
        nmap.put(sdk.SDK_RECORD_FILENAME, fileName);
        sdk.notifyEventByObject(nmap);
    }

    @Override
    public void onLocationResult(int error,double longitude, double latitude, String address,
                                 String country,String province,String city,String district,String street,String streetnumb,
                                 String detail,String describe) {
        HashMap<String, Object> nmap = new HashMap<String, Object>();
        //convert to game
        nmap.put(sdk.SDK_EVT, sdk.SDK_EVT_LOCATION);
        nmap.put(sdk.SDK_LOCATION_LONGITUDE, longitude);
        nmap.put(sdk.SDK_LOCATION_LATITUDE, latitude);
        nmap.put(sdk.SDK_LOCATION_ADDRESS, address);
        nmap.put(sdk.SDK_LOCATION_ADDRESS_COUNTY, country);
        nmap.put(sdk.SDK_LOCATION_ADDRESS_PROVINCE, province);
        nmap.put(sdk.SDK_LOCATION_ADDRESS_CITY, city);
        nmap.put(sdk.SDK_LOCATION_ADDRESS_DISTRICT, district);
        nmap.put(sdk.SDK_LOCATION_ADDRESS_STREET, street);
        nmap.put(sdk.SDK_LOCATION_ADDRESS_STREETNUMBER, streetnumb);
        nmap.put(sdk.SDK_LOCATION_ADDRESS_DETAIL, detail);
        nmap.put(sdk.SDK_LOCATION_ADDRESS_DESCRIBE, describe);
        nmap.put(sdk.SDK_ERROR, Integer.valueOf(0));
        sdk.notifyEventByObject(nmap);
    }
    //-----------------------------------------------------
    //-----------------------------------------------------
    //-----------------------------------------------------
    public static void init(Cocos2dxActivity context) {
        getInstance();
        sysinfo.init(context);
        m_context = context;

        init_pasteboard();

        BaiduLoc.init(context, instance);
        BaiduLoc.reqPermission();
    }

    public static void initApplication(Application context) {
        getInstance();
        BaiduLoc.initApplication(context);
    }

    //-----------------------------------------------------
    public static int luaevthandler = 0;

    public static void notifyEventByObject(HashMap<String, Object> data) {
        try {
            JSONObject jsonObj = new JSONObject();
            for (String key : data.keySet()) {
                jsonObj.put(key, data.get(key));
            }
            notifyEvent(jsonObj.toString());
        } catch (JSONException je) {

        }
    }

    public static void notifyEvent(final String str) {
        if (luaevthandler > 0) {
            m_context.runOnGLThread(new Runnable() {
                public void run() {
                    Cocos2dxLuaJavaBridge.callLuaFunctionWithString(luaevthandler, str);
                }
            });
        }
    }

    public static void setEventHandler(int handle) {
        luaevthandler = handle;
    }

    public static void init(int handle) {
        luaevthandler = handle;
        //游戏传过来的数据key
        if(null==gMap)
        {
            Logger.d("sdk init error, no config");
            return;
        }
        sdk_um.init(m_context, gMap);
    }

    public static void config(HashMap<String, String> data) {
        if(null==gMap){
            gMap = new HashMap<String, String>();
        }
        gMap.clear();
        gMap = data;
    }

    public static void login(final int type) {
        m_context.runOnUiThread(new Runnable() {
            public void run() {
                sdk_um.login(type);
            }
        });
    }

    public static void share(final HashMap<String, String> data) {
        m_context.runOnUiThread(new Runnable() {
            public void run() {
                String type = "";
                String title = "";
                String text = "";
                String image = "";
                String url = "";
                if (data.containsKey(SDK_SHARE_TYPE)) {
                    type = (String) data.get(SDK_SHARE_TYPE);
                }
                if (data.containsKey(SDK_SHARE_TITLE)) {
                    title = (String) data.get(SDK_SHARE_TITLE);
                }
                if (data.containsKey(SDK_SHARE_TEXT)) {
                    text = (String) data.get(SDK_SHARE_TEXT);
                }
                if (data.containsKey(SDK_SHARE_IMAGE)) {
                    image = (String) data.get(SDK_SHARE_IMAGE);
                }
                if (data.containsKey(SDK_SHARE_URL)) {
                    url = (String) data.get(SDK_SHARE_URL);
                }
                try {
                    sdk_um.share(Integer.parseInt(type), title, text, image, url);
                } catch (Exception e) {

                }
            }
        });
    }

    /**
     * 支付功能，微信、支付宝、等
     *
     * @param data
     */
    public static void pay(final HashMap<String, String> data) {
        m_context.runOnUiThread(new Runnable() {
            public void run() {

            }
        });
    }

    /**
     * 开始录音
     *
     * @param data
     */
    public static boolean start_record(final HashMap<String, String> data) {
        stop_record();
        mRecorder = new MP3Recorder(instance);
        try {
            mRecorder.start(data.get(SDK_RECORD_FILENAME));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 停止录音
     */
    public static void stop_record() {
        if (mRecorder == null) {
            return;
        }
        mRecorder.stop();
        mRecorder = null;
    }

    public static int record_getVolume() {
        if (mRecorder == null) {
            return 0;
        }
        return mRecorder.getVolume();
    }
    //---------------------------------------
    //---------------------------------------
    public static void start_locate() {
        m_context.runOnUiThread(new Runnable() {
            public void run() {
                BaiduLoc.stop();
                BaiduLoc.start();
            }
        });
    }
    public static void stop_locate() {
        m_context.runOnUiThread(new Runnable() {
            public void run() {
                BaiduLoc.stop();
            }
        });
    }
    public static double get_distance(double alongitude,double alatitude,double blongitude,double blatitude) {
        return BaiduLoc.getDistance(alongitude,alatitude,blongitude,blatitude);
    }
    //---------------------------------------
    public static void init_pasteboard() {

        if (Build.VERSION.SDK_INT >= 11) {
            m_ClipboardManager11 = (android.content.ClipboardManager) m_context.getSystemService(Context.CLIPBOARD_SERVICE);
        } else {
            m_ClipboardManager10 = (android.text.ClipboardManager) m_context.getSystemService(Context.CLIPBOARD_SERVICE);
        }
    }
    public static String get_pasteboard() {

        try {
            if (android.os.Build.VERSION.SDK_INT >= 11) {
                String content = m_ClipboardManager11.getPrimaryClip().getItemAt(0).getText().toString().trim();
                return content;
            }

            if (android.os.Build.VERSION.SDK_INT < 11) {
                String content = m_ClipboardManager10.getText().toString().trim();
                return content;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
    public static void set_pasteboard(String str) {
        if(android.os.Build.VERSION.SDK_INT>=11)
        {
            m_ClipboardManager11.setPrimaryClip(ClipData.newPlainText(null,str));
            return;
        }
        if(android.os.Build.VERSION.SDK_INT<11)
        {
            m_ClipboardManager10.setText(str);
        }
    }

}


