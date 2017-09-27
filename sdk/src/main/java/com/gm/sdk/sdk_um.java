package com.gm.sdk;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.umeng.socialize.Config;
import com.umeng.socialize.PlatformConfig;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.controller.SocialRouter;
import com.umeng.socialize.media.UMEmoji;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareConfig;
import com.umeng.socialize.common.QueuedWork;
import com.umeng.socialize.media.UMWeb;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

public class sdk_um {
    //-----------------------------------------------------

    //-----------------------------------------------------
    public static Activity m_context = null;


    public static void init(Activity context, Map<String, String> cfg) {
        m_context = context;
        Config.DEBUG = true;
        UMShareAPI.get(m_context);
        Config.DEBUG = true;
        QueuedWork.isUseThreadPool = false;
        UMShareAPI.get(m_context);

        PlatformConfig.setWeixin(cfg.get(sdk.TOKEN_WX_APPKEY).toString(), cfg.get(sdk.TOKEN_WX_APPSECRET).toString());
    }

    public static boolean isinstall() {
        final UMShareAPI mShareAPI = UMShareAPI.get(m_context);
        return mShareAPI.isInstall(m_context,SHARE_MEDIA.WEIXIN);
    }

    public static void login(int type) {
        final UMShareAPI mShareAPI = UMShareAPI.get(m_context);
        final UMAuthListener umUserinfoListener = new UMAuthListener() {
            @Override
            public void onStart(SHARE_MEDIA platform) {}
            @Override
            public void onComplete(SHARE_MEDIA platform, int action, Map<String, String> data) {
                sdk_um.um_login_notify(0, data);
            }

            @Override
            public void onError(SHARE_MEDIA platform, int action, Throwable t) {
                sdk_um.um_login_notify(1, null);
            }

            @Override
            public void onCancel(SHARE_MEDIA platform, int action) {
                sdk_um.um_login_notify(2, null);
            }
        };
//		UMAuthListener umAuthListener = new UMAuthListener() {
//		    @Override
//		    public void onComplete(SHARE_MEDIA platform, int action, Map<String,String> data) {
//				mShareAPI.getPlatformInfo(m_context, SHARE_MEDIA.WEIXIN, umUserinfoListener);
//		    }
//		    @Override
//		    public void onError(SHARE_MEDIA platform, int action, Throwable t) {
//		        sdk_um.um_login_notify(1,null);
//		    }
//		    @Override
//		    public void onCancel(SHARE_MEDIA platform, int action) {
//		    	sdk_um.um_login_notify(2,null);
//		    }
//		};
//        mShareAPI.doOauthVerify(m_context, SHARE_MEDIA.WEIXIN, umAuthListener);
        mShareAPI.getPlatformInfo(m_context, SHARE_MEDIA.WEIXIN, umUserinfoListener);
    }
    public static void share(int type, String title, String text, String img, String url) {
        UMShareListener umShareListener = new UMShareListener() {
            @Override
            public void onStart(SHARE_MEDIA platform) {}
            @Override
            public void onResult(SHARE_MEDIA platform) {
                sdk_um.um_share_nofity(0);
            }

            @Override
            public void onError(SHARE_MEDIA platform, Throwable t) {
                sdk_um.um_share_nofity(1);
            }

            @Override
            public void onCancel(SHARE_MEDIA platform) {
                sdk_um.um_share_nofity(2);
            }
        };

        try {

            ShareAction shareact = new ShareAction(m_context);
            if (img.length() > 0) {
//bmp 分享
                Bitmap bmp_orgin = BitmapFactory.decodeFile(img);
                Bitmap bmp = Bitmap.createScaledBitmap(bmp_orgin, 960, 540, true);
                Bitmap thumb_bmp = Bitmap.createScaledBitmap(bmp_orgin, 128, 72, true);
                UMImage image = new UMImage(m_context, bmp);
                UMImage thumb = new UMImage(m_context, thumb_bmp);
                image.setTitle(title);
                image.setThumb(thumb);
                shareact.withText(text).withMedia(image);
//jpg 分享出来无法点开放大显示，尺寸太大???
//                UMImage image = new UMImage(m_context, img);
//                //image.compressStyle = UMImage.CompressStyle.SCALE;//大小压缩，默认为大小压缩，适合普通很大的图
//                image.compressStyle = UMImage.CompressStyle.QUALITY;//质量压缩，适合长图的分享
//                //image.compressFormat = Bitmap.CompressFormat.PNG;//用户分享透明背景的图片可以设置这种方式，
//                image.compressFormat = Bitmap.CompressFormat.JPEG;

            }
            else if (url.length() > 0) {
                UMWeb web = new UMWeb(url);
                web.setTitle(title);
                web.setDescription(text);
                shareact.withMedia(web);
                int iconid = getDrawableIconId();
                if(iconid>0)
                {
                    //Bitmap thumb = BitmapFactory.decodeResource(m_context.getResources(), iconid);
                    //int WX_THUMB_SIZE = 72;
                    //Bitmap thumbBmp = Bitmap.createScaledBitmap(thumb, WX_THUMB_SIZE, WX_THUMB_SIZE, true);
                    UMImage image = new UMImage(m_context, iconid);
                    //透明设置，否则icon黑边
                    image.compressFormat = Bitmap.CompressFormat.PNG;
                    web.setThumb(image);
                }
            }
            else{
                shareact.withText(text).withSubject(title);
            }
            shareact.setCallback(umShareListener);
            if (type == 0) {
                shareact.setDisplayList(SHARE_MEDIA.WEIXIN, SHARE_MEDIA.QQ, SHARE_MEDIA.SINA);
                shareact.open();
            } else if (type == 1) {
                shareact.setPlatform(SHARE_MEDIA.WEIXIN);
                shareact.share();
            } else if (type == 2) {
                shareact.setPlatform(SHARE_MEDIA.WEIXIN_CIRCLE);
                shareact.share();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void um_login_notify(int result, Map<String, String> data) {

        HashMap<String, Object> nmap = new HashMap<String, Object>();
        if (result == 0) {
            for (String key : data.keySet()) {
                nmap.put(key, data.get(key));
            }
            //convert to game
            nmap.put(sdk.SDK_NAME, data.get("name"));//screen_name
            nmap.put(sdk.SDK_ICONURL, data.get("iconurl"));//profile_image_url
            nmap.put(sdk.SDK_ACCESS_TOKEN, data.get("accessToken"));//access_token
            nmap.put(sdk.SDK_REFRESH_TOKEN, data.get("refreshToken"));//RefreshToken

            nmap.put(sdk.SDK_EVT, sdk.SDK_EVT_LOGIN);
            nmap.put(sdk.SDK_ERROR, Integer.valueOf(0));
            sdk.notifyEventByObject(nmap);
        } else {
            nmap.put(sdk.SDK_EVT, sdk.SDK_EVT_LOGIN);
            nmap.put(sdk.SDK_ERROR, Integer.valueOf(1));
            sdk.notifyEventByObject(nmap);
        }
    }

    public static void um_share_nofity(int result) {
        HashMap<String, Object> nmap = new HashMap<String, Object>();
        nmap.put(sdk.SDK_EVT, sdk.SDK_EVT_SHARE);
        nmap.put(sdk.SDK_ERROR, Integer.valueOf(result));
        sdk.notifyEventByObject(nmap);
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        UMShareAPI.get(m_context).onActivityResult(requestCode, resultCode, data);
    }

    public static int getResourceId(String pVariableName, String pResourcename)
    {
        try {
            String pPackageName = m_context.getPackageName();
            return m_context.getResources().getIdentifier(pVariableName, pResourcename, pPackageName);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    public static int getDrawableIconId()
    {
        return getResourceId("icon","drawable");
    }

}


