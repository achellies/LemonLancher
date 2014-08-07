/**  
 * 文 件 名:  MT_BitmapUtils.java  
 * 描    述： 位图操作的工具类 
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者： 魏景春 黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2010-12-2
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2010-12-2       魏景春       1.0            创建BitmapUtils工具类  
 * 2010-12-2       黄悦         1.1           增加图标打圆角及立体化效果修改 
 * 2010-01-06      张永辉        1.2           生成桌面指示器图标
 */

package com.mogoo.launcher2.utils;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher2.Mogoo_FolderBubbleText;
import com.mogoo.launcher2.Mogoo_FolderInfo;
import com.mogoo.launcher2.ShortcutInfo;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BgRecognize.BgRecognizeEntity;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class Mogoo_BitmapUtils {

    private static final String TAG = "Launcher.Mogoo_BitmapUtils";

    private static final String KEY_INDICATOR_WIDTH_PORT = "indicator_width_port";

    private static final String KEY_INDICATOR_WIDTH_LAND = "indicator_width_land";
    
    private static final int SCALE_ICON_SIZE = 10;
    private static final float SCALE_CELL_SIZE = 16f;
    private static final float FOLDER_PADDING_SIZE = 6;
    private static final float SCALE_ICON_PADDING_SIZE = 2;
    
    private static final int ICON_MARGIN = 4;
    
    private static final Rect oldBounds = new Rect();
    
    static final Rect stdBounds = new Rect(0, 0, Mogoo_GlobalConfig.getIconWidth(),
            Mogoo_GlobalConfig.getIconHeight());
    
    private static final Rect stdFolderBounds = new Rect(0, 0, Mogoo_GlobalConfig.getIconWidth() + 2,
            Mogoo_GlobalConfig.getIconHeight() + 2);

    private static final Rect stdBoundsSmall = new Rect(0, 0, Mogoo_GlobalConfig.geticonScaleSize(),
            Mogoo_GlobalConfig.geticonScaleSize());

    private static final Paint paint = new Paint();

    // 存放横屏的屏幕指示器图片
    private static final Map<Integer, Bitmap> indicatorImagesLand = new HashMap<Integer, Bitmap>();

    // 存放竖屏的屏幕指示器图片
    private static final Map<Integer, Bitmap> indicatorImagesPort = new HashMap<Integer, Bitmap>();

    //存放第三方应用程序到图标
    //add by weijingchun 2011-12-8
    private static final Map<ComponentName, Bitmap> appiconmap = new HashMap<ComponentName, Bitmap>();
    private static final Map<ComponentName, String> apptitlemap = new HashMap<ComponentName, String>();

    private static final RoundRectShape shape;

    private static Bitmap modelShare;
    
//    private static Bitmap iconBottonLine;
    
    private static Bitmap iconBg; // denglixia modify

    private static Bitmap imageShadow;
    
    private static Bitmap mIconClickColor;

    static {
        float radii[] = {
                Mogoo_GlobalConfig.getRadii(), Mogoo_GlobalConfig.getRadii(), 
                Mogoo_GlobalConfig.getRadii(), Mogoo_GlobalConfig.getRadii(), 
                Mogoo_GlobalConfig.getRadii(), Mogoo_GlobalConfig.getRadii(), 
                Mogoo_GlobalConfig.getRadii(), Mogoo_GlobalConfig.getRadii()
        };
//        float radii[] = {
//                20, 20, 
//                20, 20, 
//                20, 20, 
//                20, 20, 
//        };
        shape = new RoundRectShape(radii, null, null);
        paint.setDither(true);
        paint.setAntiAlias(true);
        
        // stdBackGround = new ShapeDrawable(shape);
        // stdBackGround.getPaint().setColor(Color.WHITE);
        // stdBackGround.setBounds(stdBounds);

    }

    /**
     * 生成屏幕指示器图片 @ author: 张永辉
     * 
     * @param context 应用上下文
     * @param whichScreen 哪一屏
     * @param screenCount 屏幕总数
     * @return
     */
    public static Bitmap generateIndicatorImage(Mogoo_BitmapCache cache, int whichScreen,
            int screenCount) {
        // 如果当前系统支持横屏并且当前处于横屏时
        if (!Mogoo_GlobalConfig.isPortrait() && Mogoo_GlobalConfig.isSupportLandscape()) {
            if (indicatorImagesLand.containsKey(new Integer(whichScreen))) {
                return indicatorImagesLand.get(new Integer(whichScreen));
            } else {
                Bitmap normal = cache.getBitmap(R.drawable.mogoo_indicator_normal);
                Bitmap focus = cache.getBitmap(R.drawable.mogoo_indicator_focus);
                Bitmap normalSearch = cache.getBitmap(R.drawable.mogoo_indicator_normal_search);
                Bitmap focusSearch = cache.getBitmap(R.drawable.mogoo_indicator_focus_search);

                // 屏幕指示器背景图片
                Bitmap bg = cache.getBitmapByKey(KEY_INDICATOR_WIDTH_LAND + whichScreen,
                        Mogoo_GlobalConfig.getScreenWidth(), normal != null ? normal.getHeight() : 53,
                        Bitmap.Config.ARGB_8888);

                try {
                    int margin = 3;
                    int width = (bg.getWidth() - normal.getWidth() * (screenCount ) - margin
                            * (screenCount-1)) / 2;
                    Canvas c = new Canvas(bg);
                    int left = width;
                    int top = 0;
                    Paint paint = new Paint();

                    int len = Mogoo_GlobalConfig.getWorkspaceScreenType().length;

                    for (int i = 0; i < len; i++) {

                        if (Mogoo_GlobalConfig.getWorkspaceScreenType()[i] == Mogoo_GlobalConfig.SCREEN_TYPE_SEARCH) {
                            if (i == whichScreen) {
                                c.drawBitmap(focusSearch, left, top + 1, paint);
                            } else {
                                c.drawBitmap(normalSearch, left, top + 1, paint);
                            }
                        } else {
                            if (i == whichScreen) {
                                c.drawBitmap(focus, left, top, paint);
                            } else {
                                c.drawBitmap(normal, left, top, paint);
                            }
                        }
                        left = (i + 1) * (normal.getWidth() + margin) + width;
                    }

                    indicatorImagesLand.put(new Integer(whichScreen), bg);
                } catch (OutOfMemoryError e) {
                    System.gc();
                }

                // 当所有的指示器图片全生成完成后，清除所有在生成指示器图片中用到的材料图片
                if (indicatorImagesPort.size() == screenCount) {
                    // cache.recycle(KEY_INDICATOR_WIDTH_PORT);
                    if (indicatorImagesLand.size() == screenCount
                            || Mogoo_GlobalConfig.isSupportLandscape()) {
                        // cache.recycle(KEY_INDICATOR_WIDTH_LAND);
                        cache.recycle(R.drawable.mogoo_indicator_normal,
                                Mogoo_BitmapCache.RECYCLE_RESID_NOMAL);
                        cache.recycle(R.drawable.mogoo_indicator_focus,
                                Mogoo_BitmapCache.RECYCLE_RESID_NOMAL);
                        cache.recycle(R.drawable.mogoo_indicator_normal_search,
                                Mogoo_BitmapCache.RECYCLE_RESID_NOMAL);
                        cache.recycle(R.drawable.mogoo_indicator_focus_search,
                                Mogoo_BitmapCache.RECYCLE_RESID_NOMAL);
                    }
                }

                return bg;
            }
            // 当为竖屏时
        } else {
            if (indicatorImagesPort.containsKey(new Integer(whichScreen))) {
                return indicatorImagesPort.get(new Integer(whichScreen));
            } else {
                Bitmap normal = cache.getBitmap(R.drawable.mogoo_indicator_normal);
                Bitmap focus = cache.getBitmap(R.drawable.mogoo_indicator_focus);
                Bitmap normalSearch = cache.getBitmap(R.drawable.mogoo_indicator_normal_search);
                Bitmap focusSearch = cache.getBitmap(R.drawable.mogoo_indicator_focus_search);
                
                // 屏幕指示器背景图片
                Bitmap bg = cache.getBitmapByKey(KEY_INDICATOR_WIDTH_PORT + whichScreen,
                        Mogoo_GlobalConfig.getScreenWidth(), focusSearch != null ? focusSearch.getHeight()+2 : 53,
                        Bitmap.Config.ARGB_8888);

                if (Mogoo_GlobalConfig.LOG_DEBUG) {
                    Log.d(TAG, "ScreenWidth:" + Mogoo_GlobalConfig.getScreenWidth());
                    Log.d(TAG, "ScreenCount:" + Mogoo_GlobalConfig.getWorkspaceScreenCount());
                    Log.d(TAG, "whichScreen:" + whichScreen);
                    Log.d(TAG, "normal.getWidth:" + normal.getWidth());
                    Log.d(TAG, "bg.getWidth:" + bg.getWidth());
                }

                try {
                	//update by 袁业奔 2011-9-20
//                    int margin = 3;
                	int margin = Mogoo_GlobalConfig.getIndicatorMargin();
                	//end
                    int width = (bg.getWidth() - normal.getWidth() * (screenCount) - margin
                            * (screenCount-1)) / 2;
                    Canvas c = new Canvas(bg);
                    int left = width;
                    int top = 0;
                    Paint paint = new Paint();
                    //update by 袁业奔 2011-9-8
                    int len = screenCount;

//                    int len = Mogoo_GlobalConfig.getWorkspaceScreenType().length;

//                    for (int i = 0; i < len; i++) {
//
//                        if (Mogoo_GlobalConfig.getWorkspaceScreenType()[i] == Mogoo_GlobalConfig.SCREEN_TYPE_SEARCH) {
//                            if (i == whichScreen) {
//                                c.drawBitmap(focusSearch, left, top + 1, paint);
//                            } else {
//                                c.drawBitmap(normalSearch, left, top + 1, paint);
//                            }
//                        } else {
//                            if (i == whichScreen) {
//                                c.drawBitmap(focus, left, top, paint);
//                            } else {
//                                c.drawBitmap(normal, left, top, paint);
//                            }
//                        }
//                        left = (i + 1) * (normal.getWidth() + margin) + width;
//                    }
                    for (int i = 0; i < len; i++) {

                        if (i==0) {
                            if (i == whichScreen) {
                                c.drawBitmap(focusSearch, left, top + 1, paint);
                            } else {
                                c.drawBitmap(normalSearch, left, top + 1, paint);
                            }
                        } else {
                            if (i == whichScreen) {
                                c.drawBitmap(focus, left, top, paint);
                            } else {
                                c.drawBitmap(normal, left, top, paint);
                            }
                        }
                        left = (i + 1) * (normal.getWidth() + margin) + width;
                    }
                    //---------end
                    indicatorImagesPort.put(new Integer(whichScreen), bg);
                } catch (OutOfMemoryError e) {
                    System.gc();
                }

                // 当所有的指示器图片全生成完成后，清除所有在生成指示器图片中用到的材料图片
                if (indicatorImagesPort.size() == screenCount) {
                    // cache.recycle(KEY_INDICATOR_WIDTH_PORT);
                    if (indicatorImagesLand.size() == screenCount
                            || Mogoo_GlobalConfig.isSupportLandscape()) {
                        // cache.recycle(KEY_INDICATOR_WIDTH_LAND);
                        cache.recycle(R.drawable.mogoo_indicator_normal,
                                Mogoo_BitmapCache.RECYCLE_RESID_NOMAL);
                        cache.recycle(R.drawable.mogoo_indicator_focus,
                                Mogoo_BitmapCache.RECYCLE_RESID_NOMAL);
                        cache.recycle(R.drawable.mogoo_indicator_normal_search,
                                Mogoo_BitmapCache.RECYCLE_RESID_NOMAL);
                        cache.recycle(R.drawable.mogoo_indicator_focus_search,
                                Mogoo_BitmapCache.RECYCLE_RESID_NOMAL);
                    }
                }

                return bg;
            }
        }
    }
    
    public static void clearIndicatorImages(){
    	// 如果当前系统支持横屏并且当前处于横屏时
    	if(!Mogoo_GlobalConfig.isPortrait() && Mogoo_GlobalConfig.isSupportLandscape()){
        	Iterator<Integer> keys = indicatorImagesLand.keySet().iterator();
        	int key = -1;
        	Bitmap tempBitmap = null;
        	//清除原有数据
        	while(keys.hasNext()){
        		key = keys.next();
        		tempBitmap = indicatorImagesLand.remove(key);
        		if(tempBitmap != null){
        			tempBitmap.recycle();
        			tempBitmap = null;
        		}
        	}
    	}else{
        	Iterator<Integer> keys = indicatorImagesPort.keySet().iterator();
        	int key = -1;
        	Bitmap tempBitmap = null;
        	//清除原有数据
        	while(keys.hasNext()){
        		key = keys.next();
        		tempBitmap = indicatorImagesPort.remove(key);
        		if(tempBitmap != null){
        			tempBitmap.recycle();
        			tempBitmap = null;
        		}
        	}
    	}  	
    }

    /**
     * 获得图标上方数量指示图片 @ author: 黄悦
     * 
     * @param resId
     * @param canvas
     * @return
     */
    static Bitmap drawIconCountInfo(Drawable background, String count) {
        int w = background.getIntrinsicWidth();
        int h = background.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas canvasTemp = new Canvas(bitmap);
        Rect rect = new Rect();
        //update yeben 2011-11-3
        float textSize = paint.getTextSize();
        paint.setTextSize(Mogoo_GlobalConfig.getIconCountInfoTextSize());
        //end
        
        paint.getTextBounds(count, 0, count.length(), rect);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        background.setBounds(0, 0, w, h);
       
        
        canvasTemp.setBitmap(bitmap);
        background.draw(canvasTemp);
        canvasTemp.drawText(count, (w - rect.width()) / 2, (h + rect.height()) / 2 - 2, paint);

        canvasTemp.save();

        canvasTemp = null;
        //update yeben 2011-11-3
        paint.setTextSize(textSize);
        //end
        return bitmap;
    }
    
    /**
     * 
     * 获得图标文件夹的缩略图标
     * @ author: 黄悦
     *@param bubbleText
     *@return
     */
    public static Bitmap createFolderBitmap(Context context, Mogoo_FolderInfo info){
            if(info == null){
                return null;
            }
            
            ArrayList<ShortcutInfo> shortcutInfos = info.getContents();
            Drawable folderBg = context.getResources().getDrawable(R.drawable.mogoo_icon_folder);
            Mogoo_BitmapCache iconCache = ((LauncherApplication)(context.getApplicationContext())).getIconCache();
            int width = Mogoo_GlobalConfig.getIconWidth();
            int height = Mogoo_GlobalConfig.getIconHeight();
            
            Bitmap bitmap = Bitmap.createBitmap(width + ICON_MARGIN, height + ICON_MARGIN, Config.ARGB_8888);
            Canvas temp = new Canvas();
            
            temp.setBitmap(bitmap);
            temp.translate(4, 2);
            addFrame(width - 2, height - 2, temp);
            temp.translate(-2, 0);
            
            oldBounds.set(folderBg.getBounds());
            folderBg.setBounds(stdFolderBounds);
            folderBg.draw(temp);
            folderBg.setBounds(oldBounds);
            
            Bitmap tempIcon = null;
            Bitmap recyleIcon = null;
            Matrix matrix = new Matrix();
            //update by 袁业奔 2011-9-21
//            matrix.setScale(SCALE_ICON_SIZE, SCALE_ICON_SIZE);
            matrix.setScale(Mogoo_GlobalConfig.getScaleIconSize(), Mogoo_GlobalConfig.getScaleIconSize());
            //end
            int size = shortcutInfos.size() > 9 ? 9 : shortcutInfos.size();
            
            for(int i = 0; i < size; i++){
            	//update by 袁业奔 2011-9-21
//                tempIcon = Bitmap.createScaledBitmap(shortcutInfos.get(i).getIcon(iconCache), SCALE_ICON_SIZE, SCALE_ICON_SIZE, true);
            	tempIcon = Bitmap.createScaledBitmap(shortcutInfos.get(i).getIcon(iconCache), Mogoo_GlobalConfig.getScaleIconSize(), Mogoo_GlobalConfig.getScaleIconSize(), true);
            	//end
                float[] ds = getScaleIconTranslates(i);
                matrix.setTranslate(ds[0], ds[1]);
                temp.drawBitmap(tempIcon, matrix, paint);
                
                recyleIcon = tempIcon;
                tempIcon = null;
                recyleIcon.recycle();
                recyleIcon = null;
            }
            
            tempIcon = null;
            temp.save();
            temp = null;
            
            return bitmap;
    }
    
    /**
     * 创建制式的桌面图标 @ author: 黄悦
     * 
     * @param icon
     * @param context
     * @return
     */
    public static Bitmap createIconBitmap(Drawable icon, Context context, boolean isSystem) {
    	if(icon == null){
    		return null;
    	}
    	
        if (modelShare == null || modelShare.isRecycled()) {
            modelShare = decodeResource(context.getResources(), R.drawable.mogoo_model_share);
        }
        
//        if (iconBottonLine == null || iconBottonLine.isRecycled()){
//            iconBottonLine = decodeResource(context.getResources(), R.drawable.mogoo_icon_bottom);
//        }
        
        //denglixia add 2011.5.3
//        if(iconBg == null || iconBg.isRecycled())
//        {
//        	iconBg = decodeResource(context.getResources(), R.drawable.mogoo_model_image_background);
//        }
        //denglixia add end 2011.5.3

        int width = Mogoo_GlobalConfig.getIconWidth();
        int height = Mogoo_GlobalConfig.getIconHeight();
        Bitmap bitmap = Bitmap.createBitmap(width + ICON_MARGIN, height + ICON_MARGIN, Bitmap.Config.ARGB_8888);
        Canvas canvasTemp = new Canvas(bitmap);
//        Paint iconPaint = new Paint();
//        
//        iconPaint.setAntiAlias(true);  
//        iconPaint.setShadowLayer(1, 0, 1, 0xF0FFFFFF);
        // by test
        canvasTemp.translate(3, 4);
        addFrame(width - 2, height - 2, canvasTemp);
        canvasTemp.translate(-1, -2);
//        canvasTemp.drawBitmap(modelShare, -1, -1, paint);
        //end
        // 非系统图标
        if (!isSystem) {
            Bitmap iconTemp = null;
            BgRecognizeEntity br = null;
            // 大于等于标准图标时
            if (icon.getIntrinsicWidth() >= width) {
                iconTemp = drawable2Bitmap(icon, width, height, stdBounds);
                br = Mogoo_BgRecognize.recognize(context, iconTemp);
                // 当图标等于标准且不透明时 按系统图标操作
                if (icon.getIntrinsicWidth() == width && !br.isTransparent) {
                    drawBitmap(iconTemp, canvasTemp);
//                    canvasTemp.drawBitmap(iconBottonLine, -1, 2, paint);
                } else {
                    // 当图标大于等于标准透明时 加底板,小mark
                    if (br.isTransparent) {
                        canvasTemp.drawBitmap(br.bgIcon, 0, 0, paint);
                        drawBitmap(iconTemp, canvasTemp);
                        //addFrame(width, height, canvasTemp); 
                        canvasTemp.drawBitmap(modelShare, 0, 0, paint);
                        // 当图标大于标准不透明时 大mark
                    } else { 
                        drawBitmap(iconTemp, canvasTemp);
                        canvasTemp.drawBitmap(modelShare, 0, 0, paint);
                    } 
                }
                // 小于标准时
            } else {
            	iconTemp = drawable2Bitmap(icon, icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), stdBoundsSmall);
            	br = Mogoo_BgRecognize.recognize(context, iconTemp);
//                if (br.isTransparent) {
                	
				canvasTemp.drawBitmap(br.bgIcon, 0, 0, paint);
				int dx = (width - icon.getIntrinsicWidth()) / 2;
				int dy = (height - icon.getIntrinsicHeight()) / 2;
				canvasTemp.translate(dx, dy);
				icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
				icon.draw(canvasTemp);
				canvasTemp.translate(-dx, -dy);
//                    drawBitmap(iconTempB, canvasTemp);
//                } else {
////                    canvasTemp.drawBitmap(br.bgIcon, 0, 0, paint);
//                    drawBitmap(iconTemp, canvasTemp);
////                    canvasTemp.drawBitmap(iconTemp, (width - iconTemp.getWidth()) / 2,
////                            (height - iconTemp.getHeight()) / 2, paint);
//                }
//                addFrame(width, height, canvasTemp); 
                canvasTemp.drawBitmap(modelShare, 0, 0, paint);
            }
            if (iconTemp != null) {
                iconTemp.recycle();
            }
            iconTemp = null;
            br = null;

        } else {
//            if (Mogoo_GlobalConfig.DEAL_WITH_SYSTEM_ICON) {
//                Bitmap iconTemp = null;
//                iconTemp = drawable2Bitmap(icon, width, height, stdBounds);
//                BgRecognizeEntity br = Mogoo_BgRecognize.recognize(context, iconTemp);
//                canvasTemp.drawBitmap(br.bgIcon, 0, 0, paint);
//                drawBitmap(iconTemp, canvasTemp);
//                //addFrame(width, height, canvasTemp); 
////                canvasTemp.drawBitmap(modelShare, 0, 0, paint);
//                if (iconTemp != null) {
//                    iconTemp.recycle();
//                }
//                iconTemp = null;
//
//            } else {
                if (icon.getIntrinsicWidth() != width) {
                    oldBounds.set(icon.getBounds());
                    icon.setBounds(stdBounds);
                    icon.draw(canvasTemp);
                    icon.setBounds(oldBounds);
                } else {
                    icon.setBounds(stdBounds);
                    icon.draw(canvasTemp);
                }
//                canvasTemp.drawBitmap(iconBottonLine, -1, 2, paint);
//            }
        }
        //denglixia add 2011.5.3
//        canvasTemp.drawBitmap(iconBg, 0, 0, paint);
        canvasTemp.save();
        canvasTemp = null;
        
        return bitmap;
    }

    private static void addFrame(int width, int height, Canvas canvasTemp) {
        RectF rect = new RectF(0, 0, width-1, height-1);
        Paint paint3  = new Paint();  
        paint3.setAntiAlias(true);  
        paint3.setStyle(Style.STROKE);  
        paint3.setStrokeWidth(1);  
//        paint3.setColor(0x00909090);
//        paint3.setShadowLayer(1, 0, 0, 0xc0000000);
        paint3.setColor(Color.TRANSPARENT);
        paint3.setAlpha(30);
        
        canvasTemp.drawRoundRect(rect, Mogoo_GlobalConfig.getRadii(), Mogoo_GlobalConfig.getRadii(), paint3);
        canvasTemp.save();
    }

    /**
     * 获取图标背景阴影模板 @ author: 魏景春
     * 
     * @param context 上下文
     * @return 图标背景阴影
     */
    public static Bitmap getImageShadowBitmap(Context context) {
        if (imageShadow == null || imageShadow.isRecycled()) {
            imageShadow = decodeResource(context.getResources(), R.drawable.mogoo_image_shadow);
        }

        return imageShadow;
    }

    /**
     * 通过资源文件创建图标 @ author: 魏景春
     * 
     * @param res 资源上下文
     * @param resId 资源ID
     * @return
     */
    public static Drawable createFromResource(Resources res, int resId) {
        Rect pad = new Rect();
        InputStream is = null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        String file = "";
        byte[] np = null;
        try {
            Bitmap bm = decodeResource(res, resId);
            
//            Log.i(TAG, "BITMAP w = " + bm.getWidth() + " h = " + bm.getHeight());
            
            if (bm != null) {
                np = bm.getNinePatchChunk();
                if (np == null || !NinePatch.isNinePatchChunk(np)) {
                    np = null;
                    pad = null;
                }

                if (np != null) {
                    return new NinePatchDrawable(res, bm, np, pad, file);
                } else {
                    return new BitmapDrawable(res, bm);
                }
            }

        } catch (Exception e) {

        } finally {
            try {
                pad = null;
                opts = null;
                np = null;
                if (is != null)
                    is.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        return null;
    }
    
    /**
     * 获得点击变色效果
     * @author 黄悦
     * 2011-10-27
     * @param icon
     * @return
     */
    public static Bitmap getIconClickColor(ShortcutInfo info, Mogoo_BitmapCache iconCache){
    	if(mIconClickColor == null || mIconClickColor.isRecycled()){
    		int size = Mogoo_GlobalConfig.geticonScaleSize();
    		Drawable tempDrawable = iconCache.getContext().getResources().getDrawable(R.drawable.mogoo_icon_click_color);
    		mIconClickColor = Bitmap.createBitmap(size, size, Config.ARGB_8888);
    		Canvas canvas = new Canvas(mIconClickColor);
    		
    		tempDrawable.setBounds(0, 0, size, size);
    		tempDrawable.draw(canvas);
    		canvas.save();
    		
    		canvas = null;
    	}
    	
    	Bitmap icon = info.getIcon(iconCache).copy(Config.ARGB_8888, true);
    	
		Paint vPaint = new Paint();
		vPaint.setAlpha(120); 
    	
    	Canvas canvas = new Canvas(icon);
    	canvas.drawBitmap(mIconClickColor, 2, 2, vPaint);
    	canvas.save();
    	canvas = null;
    	vPaint = null;
    	
    	return icon;
    }

    /**
     * 通过资源文件创建位图 @ author: 魏景春
     * 
     * @param res
     * @param resId
     * @return
     */
    public static Bitmap decodeResource(Resources res, int resId) {
        Bitmap bm = null;

        if (bm == null || bm.isRecycled()) {
            InputStream is = null;
            BitmapFactory.Options opts = new BitmapFactory.Options();

            try {
                final TypedValue value = new TypedValue();
                is = res.openRawResource(resId, value);
                opts.inTargetDensity = value.density;
                bm = BitmapFactory.decodeResourceStream(res, value, is, null, opts);
            } catch (Exception e) {

            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        return bm;

    }

    /*
     * drawable 转 bitmap
     */
    public static Bitmap drawable2Bitmap(Drawable icon, int width, int height, Rect stdBounds) {
        Bitmap iconTemp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvasTemp = new Canvas(iconTemp);

        oldBounds.set(icon.getBounds());
        icon.setBounds(stdBounds);
        icon.draw(canvasTemp);
        icon.setBounds(oldBounds);

        canvasTemp = null;

        return iconTemp;
    }

     /**
     * 设置应用程序图标
     * @ author: weijingchun
     */
    public static void setApplicationIcon(ComponentName componentName,Bitmap icon)
    {
    	appiconmap.put(componentName, icon);
    }
    
    /**
     * 获取应用程序图标
     * @ author: weijingchun
     */
    public static Bitmap getApplicationIcon(ComponentName componentName)
    {
    	return appiconmap.get(componentName);    	
    }

    /**
     * 设置应用程序标题
     * @ author: weijingchun
     */
    public static void setApplicationTitle(ComponentName componentName,String title)
    {
 	   apptitlemap.put(componentName, title);
    }
    
    /**
     * 获取应用程序图标
     * @ author: weijingchun
     */
    public static String getApplicationTitle(ComponentName componentName)
    {
    		return apptitlemap.get(componentName);    	
    }
    
    /*
     * 将图标绘制到画布
     */
    private static void drawBitmap(Bitmap iconTemp, final Canvas canvasTemp) {
        ShapeDrawable s = new ShapeDrawable(shape);
        s.setBounds(stdBounds);
        Paint paint = s.getPaint();
        paint.setShader(new BitmapShader(iconTemp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        s.draw(canvasTemp);
        
        PointF mPoint1 = new PointF(0, 8);  
        PointF mPoint2 = new PointF(8, 0);  
        Path myPath1 = new Path();  
        Paint paint3  = new Paint();  
        paint3.setAntiAlias(true);  
        paint3.setStyle(Style.STROKE);  
        paint3.setStrokeWidth(2);  
        paint3.setColor(Color.BLACK);  
      
        myPath1 = drawCurve(canvasTemp, paint3, mPoint1, mPoint2);  
        canvasTemp.drawPath(myPath1, paint);  

        s = null;
    }
    
    private static Path drawCurve(Canvas canvas, Paint paint, PointF mPointa, PointF mPointb) {
        
        Path myPath = new Path();
        myPath.moveTo(0, 8f);
        myPath.quadTo(mPointa.x, mPointa.y, mPointb.x, mPointb.y);
        return myPath;  
    }

    /**
     * 制式桌面图标倒影 @ author:
     * 
     * @param icon
     * @return
     */
    static Bitmap createReflection(Bitmap icon) {
        if (icon != null) {
            return createReflection(icon, 0,
                    icon.getHeight() - Mogoo_GlobalConfig.getReflectionHeight(), icon.getWidth(),
                    Mogoo_GlobalConfig.getReflectionHeight());
        } else {
            return null;
        }
    }

    /**
     * 指定图标放大 @ author: 黄悦
     * 
     * @param icon
     * @param width
     * @param height
     * @return
     */
    static Bitmap createScale(Bitmap icon, int width, int height) {
        Bitmap bitmap = Bitmap.createScaledBitmap(icon, width, height, true);

        return bitmap;
    }

    /**
     * 产生指定位图的倒影 @ author:魏景春
     * 
     * @param bitmap 位图
     * @return 倒影位图
     */
    private static Bitmap createReflection(Bitmap bitmap, int reflectionX, int reflectionY,
            int reflectionWidth, int reflectionHeight) {
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Matrix matrix = new Matrix();
            matrix.preScale(1, -1);

            Bitmap reflectionImage = Bitmap.createBitmap(bitmap, reflectionX, reflectionY, width,
                    reflectionHeight, matrix, false);
            Bitmap bitmapWithReflection = Bitmap.createBitmap(width, height, Config.ARGB_8888);

            Canvas canvasTemp = new Canvas(bitmapWithReflection);

            Paint paint = new Paint();
            paint.setAlpha(90);

            canvasTemp.drawBitmap(reflectionImage, 0, 0, paint);
            reflectionImage.recycle();

            reflectionImage = null;
            matrix = null;
            paint = null;
            canvasTemp = null;

            return bitmapWithReflection;
        } else {
            return null;
        }

    }
    
    private static float[] getScaleIconTranslates(int index){
        float[] ds = new float[2];
        ds[0] = Mogoo_GlobalConfig.getFolderPaddingSize() + Mogoo_GlobalConfig.getScallIconPaddingSize() + (index % 3) * Mogoo_GlobalConfig.getScaleCellSize();
        ds[1] = Mogoo_GlobalConfig.getFolderPaddingSize() + Mogoo_GlobalConfig.getScallIconPaddingSize() + (index / 3) * Mogoo_GlobalConfig.getScaleCellSize();
        return ds;
    }
}
