/**  
 * 文 件 名:  MT_BgRecognize.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-3-1
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-3-1        黄悦       1.0          1.0 Version  
 */

package com.mogoo.launcher2.utils;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.LauncherApplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Mogoo_BgRecognize {
    private static final int STEP_SIZE = 10;

    private static final int[] COMP_COLOR = {0xFF0099, 0xFF66};
    
    private static final int[] COOL_ICON = {R.drawable.mogoo_icon_b, R.drawable.mogoo_icon_g, R.drawable.mogoo_icon_gb, R.drawable.mogoo_icon_gr, R.drawable.mogoo_icon_db, R.drawable.mogoo_icon_dg, R.drawable.mogoo_icon_dgr};
    private static final int[] WARM_ICON = {R.drawable.mogoo_icon_v, R.drawable.mogoo_icon_r, R.drawable.mogoo_icon_bw, R.drawable.mogoo_icon_dv};
    private static final Random random =new Random();

    /**
     * 根据所给的图片获得对应的背景 假设透明像素数量小于1% 则返回null; @ author: 黄悦
     * 
     * @param bitmap
     * @return
     */
    public static BgRecognizeEntity recognize(Context cxt, Bitmap icon) {
        if (icon == null) {
            return null;
        }

//        int width = icon.getWidth();
//        int height = icon.getHeight();
//        int size = width * height;
//
//        int pixels[] = new int[size];
//        icon.getPixels(pixels, 0, width, 0, 0, width, height);
//        
//        float aphle = 0; 
//        int rColor = 0;
//        int gColor = 0;
//        int bColor = 0;
//        for (int i = 0; i < pixels.length; i++) {
//            if((pixels[i] & 0xff000000) == 0){
//                aphle ++ ;
//                continue;
//            }
//            
//            rColor = rColor + ((pixels[i] >> 16) & 0xFF);
//            gColor = gColor + ((pixels[i] >> 8) & 0xFF);
//            bColor = bColor + (pixels[i] & 0xFF);
//        }
        
        BgRecognizeEntity br = new BgRecognizeEntity();
//        br.aphlePoint = aphle / size;
//        
//        int count = pixels.length - (int)aphle;
//        
//        if(count == 0){
//            return null;
//        }
//        
//        br.bgIcon = getColor(cxt, rColor/count, gColor/count, bColor/count);
//        br.isTransparent = false;
//        if(br.aphlePoint < 0.05){
//            br.isTransparent = false;
//        } else {
//            br.isTransparent = true;
//        }
        br.bgIcon = getColor(cxt);
        br.isTransparent = true;
        
        return br;
    }
    
    private static Bitmap getColor(Context cxt)
    {
    	Random random = new Random();
    	int category = random.nextInt(11);
        
    	int resId = 0;
        if(category % 2 == 0){
            resId = WARM_ICON[random.nextInt(WARM_ICON.length)];
        }else{
            resId = COOL_ICON[random.nextInt(COOL_ICON.length)];
        }
        
        return ((LauncherApplication)(cxt.getApplicationContext())).getIconCache().getBitmap(resId);
    }

    private static Bitmap getColor(Context cxt, int avgR, int avgG, int avgB)
    {
        int color = COMP_COLOR[0];
       
        for(int i = 1; i < COMP_COLOR.length; i++)
        {
            color = mini(color, COMP_COLOR[i], avgR, avgG, avgB);
        }
        
        int resId = 0;
        
        if(color == 0xFF0099){
            resId = WARM_ICON[(avgR + avgG + avgB) % WARM_ICON.length];
        }else{
            resId = COOL_ICON[(avgR + avgG + avgB) % COOL_ICON.length];
        }
        
        return ((LauncherApplication)(cxt.getApplicationContext())).getIconCache().getBitmap(resId);
    }
    
    private static int  mini(int a, int b, int avgR, int avgG, int avgB){
        int rColor = (a >> 16) & 0xFF;
        int gColor = (a >> 8) & 0xFF;
        int bColor = a & 0xFF;
        
        int rColor1 = (b >> 16) & 0xFF; 
        int gColor1 = (b >> 8) & 0xFF;
        int bColor1 = b & 0xFF;
        
        double disA = Math.sqrt(Math.pow((rColor - avgR), 2) + Math.pow((gColor - avgG), 2) + Math.pow((bColor - avgB), 2));
        double disB = Math.sqrt(Math.pow((rColor1 - avgR), 2) + Math.pow((gColor1 - avgG), 2) + Math.pow((bColor1 - avgB), 2));
        
        if(disA < disB){
            return a; 
        }
        
        return b;
    }  
    
    static class BgRecognizeEntity{
        Bitmap bgIcon;
        float aphlePoint;
        boolean isTransparent;
    }
}
