/**  
 * 文 件 名:  MT_BootRestoreException.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  邓丽霞                    
 * 版    本:  1.0  
 * 创建时间:   2011-4-19
 * 重启还原异常
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-4-19       邓丽霞       1.0          1.0 Version  
 */        
package com.mogoo.launcher2.exception;

public class Mogoo_BootRestoreException extends RuntimeException {
	
	/**
     * 
     */
    private static final long serialVersionUID = 5398603839209608137L;
    public Mogoo_BootRestoreException()
	{
		super();
	}
	public Mogoo_BootRestoreException(String detailMessage)
	{
        super(detailMessage);
	}
	
}
