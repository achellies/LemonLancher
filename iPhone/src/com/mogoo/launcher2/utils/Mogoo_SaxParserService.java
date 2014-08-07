/**  
 * 文 件 名:  MT_SaxParserService.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  邓丽霞                    
 * 版    本:  1.0  
 * 创建时间:   2011-4-18
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-4-18       邓丽霞       1.0          1.0 Version  
 */        
package com.mogoo.launcher2.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mogoo.launcher2.config.Mogoo_GlobalConfig;

import android.util.Log;

public class Mogoo_SaxParserService
{
	private SaxXmlHandler mSaxXmlHandler;
	private static String TAG = "Mogoo_SaxParserService";
	private static final boolean DEBUG = false;//Mogoo_GlobalConfig.LOG_DEBUG;
	
	/*
	 * InputStream inputStream xml文件输入流
	 * String keyNode 其name属性需作为映射表键的节点名
	 * String keySubNode 需获取其name属性值的keyNode的子节点名
	 */
	public Mogoo_SaxParserService(InputStream inputStream, String keyNode, String keySubNode)
	{
		try
		{
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			mSaxXmlHandler = new SaxXmlHandler(keyNode,keySubNode);
			parser.parse(inputStream, mSaxXmlHandler);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	/*
	 * 返回xml解析数据
	 */
	public HashMap<String,ArrayList<String>> getDataList()
	{
		return mSaxXmlHandler.mDataList;
	}
	public class SaxXmlHandler extends DefaultHandler
	{
		private HashMap<String,ArrayList<String>> mDataList = null;
		private ArrayList<String> mSubNodeData = null;
		private String mKeyNode;
		private String mKeySubNode;
		private String mPreKeyNodeName;	
		
		public SaxXmlHandler(String keyNode, String keySubNode)
		{
			mKeyNode = keyNode;
			mKeySubNode = keySubNode;
		}
		/*
		* 接收文档的开始的通知。
		*/
		@Override 
		public void startDocument() throws SAXException 
		{
			mDataList = new HashMap<String,ArrayList<String>>();
		} 
		/*
		* 接收元素开始的通知。
		* 参数意义如下：
		*    namespaceURI：元素的命名空间
		*    localName ：元素的本地名称（不带前缀）
		*    qName ：元素的限定名（带前缀）
		*    atts ：元素的属性集合
		*/
	
		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException 
		{
			if(DEBUG)
			{
				Log.d(TAG, "startElement namespaceURI : "+namespaceURI
						+" localName : "+localName
						+" qName :"+qName);
			}
			if(localName.equals(mKeyNode))
			{
				mSubNodeData = null;
				if(atts != null)
				{
				    mSubNodeData = new ArrayList<String>();
				    mPreKeyNodeName = atts.getValue("name");
					if(DEBUG)
					{
				        Log.d(TAG, "mPreKeyNodeName: "+mPreKeyNodeName);
					}
				}
			}
			else if(localName.equals(mKeySubNode))
			{
				if(atts != null && mSubNodeData != null)
				{
					String subNode = atts.getValue("name");
					mSubNodeData.add(subNode);
					if(DEBUG)
					{
					    Log.d(TAG, "subNode: "+subNode);
					}
				}
			}
		} 
		/*
		* 接收元素的结尾的通知。
		* 参数意义如下：
		*    uri ：元素的命名空间
		*    localName ：元素的本地名称（不带前缀）
		*    name ：元素的限定名（带前缀）
		* 
		*/
		@Override 
		public void endElement(String uri, String localName, String name) throws SAXException 
		{ 
			if(DEBUG)
			{
				Log.d(TAG, "endElement uri : "+uri
						+" localName : "+localName
						+" name :"+name);
			}
			if(localName.equals(mKeyNode))
			{
				if(mSubNodeData != null)
				{
					mDataList.put(mPreKeyNodeName, mSubNodeData);
				}
			}
		}
		/*
		* 接收字符数据的通知。
		*/
		@Override 
		public void characters(char[] ch, int start, int length) throws SAXException
		{ 
			if(DEBUG)
			{
				String str  = new String(ch, start, length); 
				Log.d(TAG, "characters : "+str);
			}
		}
		/*
		 * 接收文档的结尾的通知。
		 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
		 */
		public void endDocument()
		{
			if(DEBUG)
			{
				Log.d(TAG, "endDocument");
			}
		}
	}
}