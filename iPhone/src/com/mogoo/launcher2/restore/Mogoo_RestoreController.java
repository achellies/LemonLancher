/**  
 * 文 件 名:  MT_RestoreController.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  邓丽霞                    
 * 版    本:  1.0  
 * 创建时间:   2011-4-14
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-4-14       邓丽霞       1.0          1.0 Version  
 */        
package com.mogoo.launcher2.restore;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;

public class Mogoo_RestoreController {

	//private static MT_RestoreController mController = null;
	private static HashMap<String,ArrayList<String>> mPolicyList = null;
	private Context context;
	
	public Mogoo_RestoreController(Context context)
	{
	    this.context = context;
	}
	 /*
     * Function: loadPolicy
     * Description: 程序启动时，导入xml异常恢复机制
     * Context ctx  设备上下文
     * Return: void
   */
	public void loadPolicy()
	{
		try
		{
/*			if(mPolicyList == null)
			{
			    mPolicyList = new HashMap<String,ArrayList<String>>();
			}
			AssetManager  assetManager = context.getAssets();
			InputStream inputStream = assetManager.open("restore_policy.xml");
			MT_SaxParserService saxService = new MT_SaxParserService(inputStream,"exception","class");
			mPolicyList = saxService.getDataList();*/
			Mogoo_ParserPolicy parsePolicy = Mogoo_ParserPolicy.getInstance();
			mPolicyList = parsePolicy.getPolicyList(context);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	 /*
     * Function: clear
     * Description: 清除列表数据
     * 在 Launcher finish中调用
     * input void
     * Return: void
   */
	public void clear()
	{
		if(mPolicyList != null){
			mPolicyList.clear();
		}
	}
	 /*
     * Function: MT_RestoreController
     * Description: 给外部使用的获取MT_RestoreController的唯一实例
     * input void
     * Return: void
   */
//	public static MT_RestoreController getInstance()
//	{
//		if(mController == null)
//		{
//			mController = new MT_RestoreController();
//		}
//		return mController;
//	}
	 /*
     * Function: restoreData
     * Description: 根据产生的异常和异常机制恢复程序
     * Class cls  产生的异常类
     * Return: void
   */
	public void restoreData(Class<Exception> cls)
	{
		String key = cls.getName();
		ArrayList<String> subList = mPolicyList.get(key);
		if(subList != null && subList.size() > 0)
		{
			int len = subList.size();
			for(int i = 0; i < len; i++)
			{
				String className = subList.get(i);
				try 
				{
					Class<?> handler = Class.forName(className);
				    Object obj = handler.newInstance();
				    if(obj instanceof Mogoo_RestorePlolicy){
				        ((Mogoo_RestorePlolicy)obj).runPlolicy(context);
				    }
				} 
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
