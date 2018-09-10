package com.gx181.ftpclient.ftpclientnews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FTPUtils {
	private final static Logger LOGGER = LoggerFactory.getLogger(FTPUtils.class);
	// ftp服务器地址
	private String hostname;
	// ftp服务器端口号默认为21
	private Integer port;
	// ftp登录账号
	private String username;
	// ftp登录密码
	private String password;

	private FTPClient ftpClient = null;

	public FTPClient getFtpClient() {
		return ftpClient;
	}

	public FTPUtils(String hostname, Integer port, String username, String password) {
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	/**
	 * 初始化ftp服务器
	 */
	private void initFtpClient() {
		ftpClient = new FTPClient();
		//ftpClient.enterLocalActiveMode();
		try {
			LOGGER.info("连接ftp服务器....");
			ftpClient.connect(hostname, port); // 连接ftp服务器
			LOGGER.info("登录ftp服务器....");
			boolean isLoginSuccess = ftpClient.login(username, password);// 登录ftp服务器
			LOGGER.info("ftp服务器登录{}",isLoginSuccess);
			int reCode = ftpClient.getReplyCode(); // 是否成功登录服务器
			LOGGER.info("ftp服务器登录返回状态码:{}",reCode);
			ftpClient.setControlEncoding("utf-8");
			ftpClient.enterLocalPassiveMode();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		}
	}

	/**
	 * 上传文件
	 *
	 * @param pathname
	 *            ftp服务保存地址
	 * @param fileName
	 *            上传到ftp的文件名
	 * @param originfilename
	 *            待上传文件的名称（绝对地址） *
	 * @return
	 */
	public boolean uploadFile(String pathname, String fileName, String originfilename) {
		boolean flag = false;
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(new File(originfilename));
			initFtpClient();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			if (!existFile(pathname)) {
				createDirecroty(pathname);
			}
			ftpClient.changeWorkingDirectory(pathname);
			flag = ftpClient.storeFile(fileName, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ftpClient.logout();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != inputStream) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return flag;
	}

	/**
	 * 上传文件
	 *
	 * @param pathname
	 *            ftp服务保存地址
	 * @param fileName
	 *            上传到ftp的文件名
	 * @param inputStream
	 *            输入文件流
	 * @return
	 */
	public boolean uploadFile(String pathname, String fileName, InputStream inputStream) {
		boolean flag = false;
		try {
			initFtpClient();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			if (!existFile(pathname)) {
				createDirecroty(pathname);
			}
			ftpClient.changeWorkingDirectory(pathname);
			flag = ftpClient.storeFile(fileName, inputStream);
			inputStream.close();
			ftpClient.logout();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != inputStream) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return flag;
	}

	// 改变目录路径
	public boolean changeWorkingDirectory(String directory) {
		boolean flag = true;
		try {
			flag = ftpClient.changeWorkingDirectory(directory);
			if (flag) {
				System.out.println("进入文件夹" + directory + " 成功！");

			} else {
				System.out.println("进入文件夹" + directory + " 失败！开始创建文件夹");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return flag;
	}

	// 创建多层目录文件，如果有ftp服务器已存在该文件，则不创建，如果无，则创建
	public boolean createDirecroty(String remote) throws IOException {
		String directory = remote + "/";
		// 如果远程目录不存在，则递归创建远程服务器目录
		if (!directory.equalsIgnoreCase("/") && !changeWorkingDirectory(directory)) {
			int start = 0;
			int end = 0;
			if (directory.startsWith("/")) {
				start = 1;
			}
			end = directory.indexOf("/", start);
			String path = "";
			StringBuilder paths = new StringBuilder();
			while (true) {
				String subDirectory = new String(remote.substring(start, end).getBytes("GBK"), "iso-8859-1");
				path = path + "/" + subDirectory;
				if (!existFile(path)) {
					if (makeDirectory(subDirectory)) {
						changeWorkingDirectory(subDirectory);
					} else {
						System.out.println("创建目录[" + subDirectory + "]失败");
						changeWorkingDirectory(subDirectory);
					}
				} else {
					changeWorkingDirectory(subDirectory);
				}
				paths.append("/").append(subDirectory);
				start = end + 1;
				end = directory.indexOf("/", start);
				// 检查所有目录是否创建完毕
				if (end <= start) {
					break;
				}
			}
		}
		return true;
	}

	// 判断ftp服务器文件是否存在
	public boolean existFile(String path) throws IOException {
		boolean flag = false;
		FTPFile[] ftpFileArr = ftpClient.listFiles(path);
		if (ftpFileArr.length > 0) {
			flag = true;
		}
		return flag;
	}

	// 创建目录
	public boolean makeDirectory(String dir) {
		boolean flag = true;
		try {
			flag = ftpClient.makeDirectory(dir);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	/**
	 * 下载文件 *
	 *
	 * @param pathname
	 *            FTP服务器文件目录 *
	 * @param filename
	 *            文件名称 *
	 * @param localpath
	 *            下载后的文件路径 *
	 * @return
	 */
	public boolean downloadFile(String pathname, String fileNamePattern, String localpath) {
		boolean flag = false;
		OutputStream os = null;
		try {
			initFtpClient();
			// 切换FTP目录
			boolean changeDirSuccess = ftpClient.changeWorkingDirectory(pathname);
			LOGGER.info("切换目录{} 成功?{}",pathname,changeDirSuccess);
			
			if(!changeDirSuccess){
				LOGGER.error("切换目录失败!");
				return false;
			}
			FTPFile[] ftpFiles = ftpClient.listFiles();
			LOGGER.info("文件个数{}",ftpFiles.length);
			File localDir = new File(localpath);
			if(!localDir.exists()||!localDir.isDirectory()){
				localDir.mkdirs();
			}
			
			Pattern pat = Pattern.compile(fileNamePattern,Pattern.CASE_INSENSITIVE);
			Matcher m = null;
			for (FTPFile file : ftpFiles) {
				
				if(file.getName().startsWith("template")){//模板文件不下载
					continue;
				}
				LOGGER.info("下载配置文件{}",file.getName());
				m = pat.matcher(file.getName());
				if (m.matches()) {
					File localFile = new File(localpath + File.separator + file.getName());
					os = new FileOutputStream(localFile);
					ftpClient.retrieveFile(file.getName(), os);
					os.close();
				}
				m.reset();
			}
			ftpClient.logout();
			flag = true;
		} catch (Exception e) {
			flag = false;
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		} finally {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != os) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return flag;
	}

	/**
	 * 删除文件 *
	 *
	 * @param pathname
	 *            FTP服务器保存目录 *
	 * @param filename
	 *            要删除的文件名称 *
	 * @return
	 */
	public boolean deleteFile(String pathname, String filename) {
		boolean flag = false;
		try {
			initFtpClient();
			// 切换FTP目录
			ftpClient.changeWorkingDirectory(pathname);
			ftpClient.dele(filename);
			ftpClient.logout();
			flag = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return flag;
	}
}