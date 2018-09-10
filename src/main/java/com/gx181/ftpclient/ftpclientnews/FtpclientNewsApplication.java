package com.gx181.ftpclient.ftpclientnews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.log4j.PropertyConfigurator;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gx181.ftpclient.ftpclientnews.quartz.NewsInfoSummaryJob;

public class FtpclientNewsApplication {
	
	static {  
		String outSideJarPath = StringUtils.getPath2()+File.separator+"log4j.properties";
		File log4jConfigFile = new File(outSideJarPath);
		if(log4jConfigFile.exists()&&log4jConfigFile.isFile()){
			System.out.println("读取jar包外同目录下的log4j配置文件:"+outSideJarPath);
			System.getProperty("user.dir");
			try {
				 PropertyConfigurator.configure(new FileInputStream(log4jConfigFile));  
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("读取jar包内的log4j配置文件");
			PropertyConfigurator.configure(FtpclientNewsApplication.class.getResourceAsStream("log4j.properties")); 
		}
		
       
    } 
	
	private final static Logger LOGGER = LoggerFactory.getLogger(FtpclientNewsApplication.class);
	
	//创建调度器
    public  Scheduler getScheduler() throws SchedulerException{
    	LOGGER.debug("创建调度器");
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        return schedulerFactory.getScheduler();
    }
	
    public  void schedulerJob() throws SchedulerException{
        //创建任务
    	LOGGER.debug("创建任务 NewsInfoSummaryJob");
        JobDetail newsInfoSummaryJob = JobBuilder.newJob(NewsInfoSummaryJob.class)
        		.withIdentity("NewsInfoSummaryJob", "news")
        		.withDescription("国欣官网定时汇总生成新闻分页信息")
        		.requestRecovery()
        		.build();
        LOGGER.debug("创建触发器 每60分钟执行一次");
        Trigger newsInfoSummaryTrigger = TriggerBuilder.newTrigger().withIdentity("newsInfoSummaryTrigger", "news")
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(60).repeatForever())
                            .build();
        
        Scheduler scheduler = getScheduler();
        //将任务及其触发器放入调度器
        scheduler.scheduleJob(newsInfoSummaryJob, newsInfoSummaryTrigger);
        //调度器开始调度任务
        scheduler.start();
        
    }
    
	public static void main(String[] args) {
		System.out.println("####################国欣官网新闻汇总生成json文件信息调度服务启动....######################");
		try {
			FtpclientNewsApplication service = new FtpclientNewsApplication();
			service.schedulerJob();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
}
