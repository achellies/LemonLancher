/**  
 * 文 件 名: PasswordEncryption.java  
 * 描    述：   
 * 版    权：Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司: 摩通科技 
 * 作    者：张永辉                     
 * 版    本: 1.0  
 * 创建时间: 2011-4-28
 *  
 * 修改历史：  
 * 时间                             作者                       版本                        描述  
 * ------------------------------------------------------------------  
 * 2011-4-28        张永辉                1.0          1.0 Version  
 */        

package com.mogoo.launcher2.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordEncryption {
	private static MessageDigest messageDigest = null;
	private static char[] hexDigit = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	
	public static String getMD5Password(String password){
		byte[] plainText = null;
		try {
			plainText = password.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		char str[] = null;
		try {
			if (messageDigest == null) {
				messageDigest = MessageDigest.getInstance("MD5");
			}
			
			messageDigest.update(plainText);
			byte[] md = messageDigest.digest();
			int j = md.length;
			str = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigit[byte0 >>> 4 & 0xf];
				str[k++] = hexDigit[byte0 & 0xf];
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return new String(str);
	}
	
	public static void main(String [] args){
		System.out.println(PasswordEncryption.getMD5Password("root").toUpperCase());
	}
}

