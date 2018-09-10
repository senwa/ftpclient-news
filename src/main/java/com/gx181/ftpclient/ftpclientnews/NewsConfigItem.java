package com.gx181.ftpclient.ftpclientnews;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NewsConfigItem implements Comparable<NewsConfigItem> {

	private String time;
	
	private String title;
	
	private String icon;
	
	private String abstractDesc;
	
	private String filename;
	
	
	public NewsConfigItem(String time, String title, String icon, String abstractDesc, String filename) {
		super();
		this.time = time;
		this.title = title;
		this.icon = icon;
		this.abstractDesc = abstractDesc;
		this.filename = filename;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((abstractDesc == null) ? 0 : abstractDesc.hashCode());
		result = prime * result + ((filename == null) ? 0 : filename.hashCode());
		result = prime * result + ((icon == null) ? 0 : icon.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NewsConfigItem other = (NewsConfigItem) obj;
		if (abstractDesc == null) {
			if (other.abstractDesc != null)
				return false;
		} else if (!abstractDesc.equals(other.abstractDesc))
			return false;
		if (filename == null) {
			if (other.filename != null)
				return false;
		} else if (!filename.equals(other.filename))
			return false;
		if (icon == null) {
			if (other.icon != null)
				return false;
		} else if (!icon.equals(other.icon))
			return false;
		if (time == null) {
			if (other.time != null)
				return false;
		} else if (!time.equals(other.time))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "NewsConfigItem [time=" + time + ", title=" + title + ", icon=" + icon + ", abstractDesc=" + abstractDesc
				+ ", filename=" + filename + "]";
	}


	public String getTime() {
		return time;
	}


	public void setTime(String time) {
		this.time = time;
	}


	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}


	public String getIcon() {
		return icon;
	}


	public void setIcon(String icon) {
		this.icon = icon;
	}


	public String getAbstractDesc() {
		return abstractDesc;
	}


	public void setAbstractDesc(String abstractDesc) {
		this.abstractDesc = abstractDesc;
	}


	public String getFilename() {
		return filename;
	}


	public void setFilename(String filename) {
		this.filename = filename;
	}


	@Override
	public int compareTo(NewsConfigItem o) {
		if(o!=null){
			String aTime = this.getTime();
			String bTime = o.getTime();
			
			if(StringUtils.isNotEmpty(aTime)&&StringUtils.isNotEmpty(bTime)){
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try{
					Date aDate = sdf.parse(aTime);
					Date bDate = sdf.parse(bTime);
					if(aDate.before(bDate)){
						return 1;
					}else if(aDate.after(bDate)){
						return -1;
					}else{
						return 0;
					}
					
				}catch(Exception e){
					e.printStackTrace();
					return 0;
				}
			}else{
				return -1;
			}
		}
		
		return -1;//如果比较的对象是个null,把null的放到前面
	}

}
