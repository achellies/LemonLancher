package com.mogoo.launcher2.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;

import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher.R;

/**
 * ���ڼ�����Ӧ���Ƿ��滻��
 * @author ������
 *
 */
public class CheckThirdAppUtils {
	
	public static final char[] hexChar = { '0', '1', '2', '3', '4', '5', '6','7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	public static final String[] hashTypes = new String[] { "MD5" };
	public static final String START_MARK = "/data/start_mark.bc";
	
	private static String[] PACKAGENAME_ARRAY;
	private static String[] PACKAGE_MD5_ARRAY;
	//缺少内置apk
	private static final int LOST_PACKAGE = 1;
	//不是我们的内置apk
	private static final int NOT_OUR_PACKAGE = 2;
	
	
	 /**
     * 
     * @param context
     */
    public static List<String> sortForStart(Context context){
		List<String> lostList = new ArrayList<String>();
		PACKAGENAME_ARRAY = context.getResources().getStringArray(R.array.sort_icons_name);
		PACKAGE_MD5_ARRAY = context.getResources().getStringArray(R.array.sort_icons_file);
		
		
		
		File file = new File(START_MARK);
		// 数据库没有的情况
		if (file.exists()) {
			return lostList;
		}

		PackageManager pm = context.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);

		List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
		ArrayList<String> temps = new ArrayList<String>();
		for (ResolveInfo app : apps) {
			temps.add(app.activityInfo.packageName);
		}

		for (String str : PACKAGENAME_ARRAY) {
			if (str != null && !temps.contains(str.toLowerCase())) {
				lostList.add(str + "_" + LOST_PACKAGE);
			} else if (str != null && temps.contains(str.toLowerCase())) {
				String hash = getAppHash(context, str);

		        Log.d("CheckThirdAppUtils", str+":"+hash.toLowerCase());

				if (hash == null || !contains(hash)) {
					lostList.add(str + "_" + NOT_OUR_PACKAGE);
				}
			}
		}

		if(lostList.size() == 0){
			writeMark(context);
		} else if(lostList.size() > 0){
			LauncherApplication app = (LauncherApplication) context.getApplicationContext();
			app.setFilter(true);
		}
		
		return lostList;
    }
    
    private static boolean contains(String hash){
    	hash = hash.toLowerCase();
    	for (String str : PACKAGE_MD5_ARRAY){
    		if(hash.equals(str.toLowerCase())){
    			return true;
    		}
    	}
    	
    	return false;
    }
    

	private static void writeMark(Context context) {
		File file = new File(START_MARK) ;
	    	try{
	    		if(!file.exists()){
	    			if(!file.createNewFile()){
					return ;
		    		}
	    		}
	    	}catch(Exception e){
	    		e.printStackTrace() ;
	    	}

		if(file!=null){
	    		FileOutputStream fos = null ;
	    		try{
	    			fos = new FileOutputStream(file) ;				
				fos.write(0) ;					
	    		}catch(Exception e){
				e.printStackTrace() ;
    			}finally{
				if(fos!=null){
					try{
						fos.close() ;
					}catch(Exception e){
						//
					}
					
				}			
			}
    		}
	}
	
	/**
	 * ������Ӧ���Ƿ��޸Ĺ�
	 * @param appConfigHashs ��ʼ����Ӧ�ð��HASHֵ������ֲ��Ա����
	 * @param packageNames Ҫ����ĵ������
	 * @return
	 */
	private static boolean check(Context context,List<String> appConfigHashs,List<String> packageNames){
		boolean result = true ;
		
		if(packageNames==null){
			return false ;
		}
		
		for(String packageName:packageNames){
			String hash = getAppHash(context,packageName) ;
			if(TextUtils.isEmpty(hash)){
				return false ;
			}else{
				if(!appConfigHashs.contains(hash)){
					return false ;
				}
			}
		}
		
		return result ;
	}
	
	
	/**
	 * ȡ��Ӧ�ð��HASHֵ
	 * @param packageName
	 * @return
	 */
	private static String getAppHash(Context context,String packageName){
		if(TextUtils.isEmpty(packageName)){
			return null ;
		}
		
		PackageManager pm = context.getPackageManager();
		ApplicationInfo info = null ;
		try {
			info = pm.getApplicationInfo(packageName, 0) ;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return null ;
		}
		
		if(info!=null){
			return getHash(info.sourceDir) ;
		}
		
		return null ;
	}
	
	
	/**
	 * ȡ���ļ���HASHֵ
	 * @param fileName
	 * @return
	 */
	private static String getHash(String fileName) {
		List<MessageDigest> mds = new ArrayList<MessageDigest>();
		try {
				for (int i = 0; i < hashTypes.length; i++) {
					MessageDigest md = MessageDigest.getInstance(hashTypes[i]);
					mds.add(md);
				}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		InputStream fis = null;
		try {
			fis = new FileInputStream(fileName);
			byte[] buffer = new byte[1024];
			int numRead = 0;
			MessageDigest md = null;
			for (int i = 0; i < mds.size(); i++) {
				md = (MessageDigest) mds.get(i);
				while ((numRead = fis.read(buffer)) > 0) {
					md.update(buffer, 0, numRead);
					}
				}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
						e.printStackTrace();
				}
			}
		}
		MessageDigest md = null;
		StringBuffer hash = new StringBuffer("");
		for (int i = 0; i < mds.size(); i++) {
			md = (MessageDigest) mds.get(i);
			hash.append(toHexString(md.digest()));
		}
		return hash.toString();
	}
	
	private static String toHexString(byte[] b) {
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
		}
		return sb.toString();
	}
	
}