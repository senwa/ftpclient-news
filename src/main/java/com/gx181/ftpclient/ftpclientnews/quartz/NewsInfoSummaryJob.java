package com.gx181.ftpclient.ftpclientnews.quartz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gx181.ftpclient.ftpclientnews.FTPUtils;
import com.gx181.ftpclient.ftpclientnews.NewsConfigItem;
import com.gx181.ftpclient.ftpclientnews.PropertyHelper;
import com.gx181.ftpclient.ftpclientnews.StringUtils;

public class NewsInfoSummaryJob implements Job {
	private final static Logger LOGGER = LoggerFactory.getLogger(NewsInfoSummaryJob.class);
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		
		LOGGER.info("启动新闻汇总服务...");
		Properties pp = PropertyHelper.getPropertyFile("");
		
		String ftpServer_hostname = pp.getProperty("ftpserver.hostname");
		String ftpServer_port = pp.getProperty("ftpserver.port");
		String ftpServer_username = pp.getProperty("ftpserver.username");
		String ftpServer_password = pp.getProperty("ftpserver.password");
		
		
		String ftpServer_cpxwpath_configs = pp.getProperty("ftpserver.path.cpxw.configs");//产品新闻配置目录
		String ftpServer_cpxwpath_json = pp.getProperty("ftpserver.path.cpxw.json");//产品新闻json目录
		
		String ftpServer_hyxwpath_configs = pp.getProperty("ftpserver.path.hyxw.configs");//行业新闻配置目录
		String ftpServer_hyxwpath_json = pp.getProperty("ftpserver.path.hyxw.json");//行业新闻json目录
		
		String local_tempdir = pp.getProperty("local.tempdir");//本地临时目录,用于下载服务器上的文件临时存储
		
		String news_pagesize = pp.getProperty("news.pagesize");//新闻每页的大小
		
		LOGGER.debug("####ftp服务器配置信息####");
		LOGGER.debug("ftpserver.hostname={}",ftpServer_hostname);
		LOGGER.debug("ftpserver.port={}",ftpServer_port);
		LOGGER.debug("ftpserver.username={}",ftpServer_username);
		LOGGER.debug("ftpserver.password={}",ftpServer_password);
		
		LOGGER.debug("产品新闻:ftpserver.path.cpxw.configs={}",ftpServer_cpxwpath_configs);
		LOGGER.debug("产品新闻:ftpserver.path.cpxw.json={}",ftpServer_cpxwpath_json);
		
		LOGGER.debug("行业新闻:ftpserver.path.hyxw.configs={}",ftpServer_hyxwpath_configs);
		LOGGER.debug("行业新闻:ftpserver.path.hyxw.json={}",ftpServer_hyxwpath_json);
		
		LOGGER.debug("local.tempdir={}",local_tempdir);
		LOGGER.debug("news.pagesize={}",news_pagesize);
		
		FTPUtils ftpUtils = new FTPUtils(ftpServer_hostname, Integer.valueOf(ftpServer_port), ftpServer_username, ftpServer_password);
		
		ftpServer_cpxwpath_configs = ftpServer_cpxwpath_configs.replaceAll("#", "/");
		ftpServer_cpxwpath_json = ftpServer_cpxwpath_json.replaceAll("#", "/");
		ftpServer_hyxwpath_configs = ftpServer_hyxwpath_configs.replaceAll("#", "/");
		ftpServer_hyxwpath_json = ftpServer_hyxwpath_json.replaceAll("#", "/");
		
		
		//###############################################处理产品新闻
		doWork(ftpUtils,ftpServer_cpxwpath_configs,ftpServer_cpxwpath_json,local_tempdir,news_pagesize);
		
		//##############################################处理行业新闻
		doWork(ftpUtils,ftpServer_hyxwpath_configs,ftpServer_hyxwpath_json,local_tempdir,news_pagesize);
		
		LOGGER.info("新闻汇总服务结束");
	}
	
	/**
	 * @param FTPUtils ftpUtils ftp操作类
	 * @param String configsPath 服务器上存放新闻配置信息的文件夹
	 * @param String jsonPath 本地生成json信息后需要上传到服务器的这个文件夹
	 * @param String local_tempdir	本地临时文件夹路径
	 * @param String news_pagesize	分页大小
	 * */
	private void doWork(FTPUtils ftpUtils,String configsPath,String jsonPath,String local_tempdir,String news_pagesize){
		
		//清空文件夹
		LOGGER.info("清空文件夹={}",local_tempdir);
		File localDir = new File(local_tempdir);
		if(localDir.exists()&&localDir.isDirectory()){
			File[] files = localDir.listFiles();
			if(files!=null&&files.length>0){
				for(int i=0; i<files.length; i++){
					files[i].delete();
				}
			}
		}else{
			localDir.mkdirs();
		}
		
		boolean isDownLoadSuccess = ftpUtils.downloadFile(configsPath, "\\w+\\.properties", local_tempdir);
		LOGGER.info("下载文件:{}",isDownLoadSuccess);
		
		if(isDownLoadSuccess){
			LOGGER.info("从文件中提取json信息...");
			/*
			 * 生成思路:
			 * 考虑到后期可能会很多,把所有数据放到一个json文件中导致文件过于庞大下载慢,采用拆分的方法
			 * 一个json文件中只放4条数据,按照序号排,序号作为下面的页码
			 * 再生成一个json文件,记录总记录数,总的页码数...也可以记录阅读排行等信息
			 * */
			
			File localDirTemp = new File(local_tempdir);
			if(localDirTemp.exists()&&localDirTemp.isDirectory()){
				File[] configFiles = localDirTemp.listFiles(new FilenameFilter(){

					@Override
					public boolean accept(File dir, String name) {
						
						if(name!=null&&name.endsWith(".properties")){
							return true;
						}
						
						return false;
					}
					
				});
				if(configFiles!=null&&configFiles.length>0){
					
					Map<String,Object> newsBaseInfoMap = new HashMap<String,Object>();//记录新闻的基本信息
					
					Properties pConfig = new Properties();
					List<NewsConfigItem> jsonMapItemList = new ArrayList<NewsConfigItem>();//存放所有,根据时间排序
					NewsConfigItem newsConfigItemTemp = null;
					for(int i=0; i<configFiles.length; i++){
						LOGGER.debug("配置文件名称{},{}",configFiles[i].getPath(),configFiles[i].getName());
						try {
							pConfig.load(new FileReader(configFiles[i]));
							String time = pConfig.getProperty("time", "");//时间
							String title = pConfig.getProperty("title", "");//标题
							String icon = pConfig.getProperty("icon", "");//图片
							String abstractDesc = pConfig.getProperty("abstract", "");//摘要
							String filename = pConfig.getProperty("filename", "");//对应文件名
							
							if(StringUtils.isEmpty(time)){
								LOGGER.warn("配置文件:{}的{}为空,配置错误!",configFiles[i].getName(),"time");
								continue;
							}
							if(StringUtils.isEmpty(title)){
								LOGGER.warn("配置文件:{}的{}为空,配置错误!",configFiles[i].getName(),"title");
								continue;
							}
							if(StringUtils.isEmpty(icon)){
								LOGGER.warn("配置文件:{}的{}为空,配置错误!",configFiles[i].getName(),"icon");
								continue;
							}
							if(StringUtils.isEmpty(abstractDesc)){
								LOGGER.warn("配置文件:{}的{}为空,配置错误!",configFiles[i].getName(),"abstractDesc");
								continue;
							}
							if(StringUtils.isEmpty(filename)){
								LOGGER.warn("配置文件:{}的{}为空,配置错误!",configFiles[i].getName(),"filename");
								continue;
							}
							
							newsConfigItemTemp = new NewsConfigItem(time, title, icon, abstractDesc, filename);
							LOGGER.debug("新闻配置项:{}",newsConfigItemTemp.toString());
							jsonMapItemList.add(newsConfigItemTemp);
							pConfig.clear();
							
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					//按照时间倒排序
					LOGGER.debug("对新闻按时间倒排序");
					Collections.sort(jsonMapItemList);
					
					//生成json文件
					int newsPageSize=4;
					if(StringUtils.isNotEmpty(news_pagesize)){
						try{
							newsPageSize = Integer.parseInt(news_pagesize);
						}catch(NumberFormatException e){
							e.printStackTrace();
							newsPageSize=4;
						}
					}
					
					newsBaseInfoMap.put("news_totalnum", jsonMapItemList.size());//新闻总记录数
					newsBaseInfoMap.put("news_pagesize", newsPageSize);//分页大小
					
					File jsonFile = null;
					FileWriter fw = null;
					ObjectMapper mapper = new ObjectMapper();
					int pageNum	= 0;
					LOGGER.debug("开始生成新闻分页信息json文件...");
					Map<String,Map<String,String>> sibingInfoMap = new HashMap<String,Map<String,String>>();//记录当前新闻前后新闻的信息
					for(int i=0; i<jsonMapItemList.size(); i++){
						
						if(i%newsPageSize==0){
							pageNum++;
							//生成一个pagejson文件,这个文件中记录当前页显示的新闻内容,文件名就是下面第几页的序号
							jsonFile = new File(local_tempdir+File.separator+pageNum+".json");
							LOGGER.debug("创建新闻分页json文件{}",jsonFile.getAbsolutePath());
							try {
								jsonFile.createNewFile();
								fw = new FileWriter(jsonFile);//本地创建一下文件,可以核对是否生成的正确
								LOGGER.debug("写入json信息");
								fw.write(mapper.writeValueAsString(jsonMapItemList.subList(i, Math.min(i+newsPageSize,jsonMapItemList.size()))));
								fw.flush();
								boolean isSuccessUploadJsonPage = ftpUtils.uploadFile(jsonPath, jsonFile.getName(), new FileInputStream(jsonFile));
								LOGGER.info("上传分页json文件成功?{}",isSuccessUploadJsonPage);
							} catch (JsonProcessingException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}finally{
								try {
									fw.close();
									jsonFile = null;
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						
						//用于记录上一篇下一篇
						String fileName = jsonMapItemList.get(i).getFilename();
						sibingInfoMap.put(fileName, new HashMap<String,String>());
						
						if(i-1>=0){
							sibingInfoMap.get(fileName).put("pre_title", jsonMapItemList.get(i-1).getTitle());
							sibingInfoMap.get(fileName).put("pre_filename", jsonMapItemList.get(i-1).getFilename());
						}
						
						if(i+1<jsonMapItemList.size()){
							sibingInfoMap.get(fileName).put("next_title", jsonMapItemList.get(i+1).getTitle());
							sibingInfoMap.get(fileName).put("next_filename", jsonMapItemList.get(i+1).getFilename());
						}
					}
					
					newsBaseInfoMap.put("news_pagenum", pageNum);//总的页码个数
					
					//生成总的新闻信息,包括分页大小,总记录条数,总页数
					jsonFile = new File(local_tempdir+File.separator+"news_pagebean.json");
					LOGGER.debug("创建新闻分页总信息json文件{}",jsonFile.getAbsolutePath());
					try {
						jsonFile.createNewFile();
						fw = new FileWriter(jsonFile);//本地创建一下文件,可以核对是否生成的正确
						LOGGER.debug("写入新闻分页总信息json");
						
						fw.write(mapper.writeValueAsString(newsBaseInfoMap));
						fw.flush();
						boolean isSuccessUpload = ftpUtils.uploadFile(jsonPath, jsonFile.getName(), new FileInputStream(jsonFile));
						
						LOGGER.info("上传新闻分页总信息json文件成功?{}",isSuccessUpload);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}finally{
						try {
							fw.close();
							jsonFile = null;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					
					//生成上一篇下一篇数据字典,用于记录上一篇文章下一篇文章的jsonmap文件对象
					jsonFile = new File(local_tempdir+File.separator+"news_prenext_dic.json");//根据html文件的名字作为key查找
					LOGGER.debug("创建当前新闻上一篇下一篇数据字典信息json文件{}",jsonFile.getAbsolutePath());
					try {
						jsonFile.createNewFile();
						fw = new FileWriter(jsonFile);//本地创建一下文件,可以核对是否生成的正确
						LOGGER.debug("写入新闻分页总信息json");
						
						fw.write(mapper.writeValueAsString(sibingInfoMap));
						fw.flush();
						boolean isSuccessUpload = ftpUtils.uploadFile(jsonPath, jsonFile.getName(), new FileInputStream(jsonFile));
						
						LOGGER.info("上传新闻分页总信息json文件成功?{}",isSuccessUpload);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}finally{
						try {
							fw.close();
							jsonFile = null;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
	}

}
