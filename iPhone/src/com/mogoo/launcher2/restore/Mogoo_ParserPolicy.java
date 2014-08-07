package com.mogoo.launcher2.restore;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.res.AssetManager;

import com.mogoo.launcher2.utils.Mogoo_SaxParserService;

public class Mogoo_ParserPolicy {
	
	private static HashMap<String,ArrayList<String>> mPolicyList = null;
	private static Mogoo_ParserPolicy mMT_ParserPolicy = null;
	
	public static Mogoo_ParserPolicy getInstance()
	{
		if(mMT_ParserPolicy == null)
		{
			mMT_ParserPolicy = new Mogoo_ParserPolicy();
		}
		return mMT_ParserPolicy;
	}
	
	private void loadPolicy(Context context)
	{
		 try 
		 {
			 mPolicyList = new HashMap<String,ArrayList<String>>();		 
		     AssetManager  assetManager = context.getAssets();
		     InputStream inputStream = assetManager.open("restore_policy.xml");
			 Mogoo_SaxParserService saxService = new Mogoo_SaxParserService(inputStream,"exception","class");
			 mPolicyList = saxService.getDataList();
		 } 
		 catch (Exception e) 
		 {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public HashMap<String,ArrayList<String>> getPolicyList(Context context)
	{
		if(mPolicyList == null)
		{
			loadPolicy(context);
		}
		return mPolicyList;
	}
}
