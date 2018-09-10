package com.gx181.ftpclient.ftpclientnews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyHelper {
	private final static Logger LOGGER = LoggerFactory.getLogger(PropertyHelper.class);
	public static Properties getPropertyFile(String ppName){
		String propertyName="config.properties";
		if(!StringUtils.isEmpty(ppName)){
			propertyName = ppName;
		}
		LOGGER.warn("读取配置文件:{}",propertyName);
		
		//获取配置文件
		Properties pp = new Properties();
		File fp = new File(StringUtils.getPath2()+File.separator+propertyName);
		if(fp.exists()&&fp.isFile()){
			LOGGER.warn("读取jar包外同目录下的配置文件:{}",fp.getPath());
			try {
				pp.load(new FileInputStream(fp));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			try {
				LOGGER.warn("读取jar包内的配置文件:{}",propertyName);
				pp.load(PropertyHelper.class.getResourceAsStream(propertyName));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return pp;
	}
}
