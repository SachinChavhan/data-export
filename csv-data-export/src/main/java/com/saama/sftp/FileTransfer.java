package com.saama.sftp;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.saama.model.ExportParameter;

public class FileTransfer {

	private static ChannelSftp sftpChannel;
	private static Session sftpSession;

	static Logger logger = Logger.getLogger(FileTransfer.class);

	public static void uploadFile(String sourcePath, String remotePath) {

		long start = System.currentTimeMillis();
		try {
			sftpChannel.put(sourcePath, remotePath);
		} catch (SftpException e) {
			logger.error("failed to upload file : " + sourcePath);
			e.printStackTrace();
		}finally {
			logger.info("Time taken to upload file : " +sourcePath+" : " + (System.currentTimeMillis() - start ) + " ms");
		}
	}

	public static void createSftpSession(String userName, String password, String hostname) {

		logger.info("creating sftp session");
		JSch jsch = new JSch();
		Session session;
		try {
			session = jsch.getSession(userName, hostname);
			session.setPassword(password);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			ChannelSftp sftpChan = (ChannelSftp) session.openChannel("sftp");
			sftpChan.connect();
			sftpChannel = sftpChan;
			sftpSession = session;

		} catch (Exception e) {
			logger.error("failed to create sftp session");
			e.printStackTrace();
		}
	}

	public static void mkdir(ExportParameter params) {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String completePath = params.getSftpDestPath()+"/"+params.getReportName()+"/"+date;
		params.setSftpDestPath(completePath);
		SftpATTRS attrs = null;
		try {
			attrs = sftpChannel.stat(completePath);
		} catch (Exception e) {
			logger.info("directory does not exist : " + completePath);
		}

		if (attrs != null) {
			//System.out.println("Directory exists IsDir=" + attrs.isDir());
		} else {
			logger.info("Creating dir " + completePath);
			try {
				String[] complPath = completePath.split("/");
	            sftpChannel.cd("/");
	            for (String folder : complPath) {
	                if (folder.length() > 0) {
	                    try {
	                        //System.out.println("Current Dir : " + sftpChannel.pwd());
	                        sftpChannel.cd(folder);
	                    } catch (SftpException e2) {
	                    	sftpChannel.mkdir(folder);
	                    	sftpChannel.cd(folder);
	                    }
	                }
	            }
	            sftpChannel.cd("/");
			} catch (SftpException e) {
				// TODO Auto-generated catch block
				logger.error("failed to create directory : " + completePath);
				e.printStackTrace();
			}
		}
	}
	
	public static void sftpClose() {
		sftpChannel.exit();
		sftpSession.disconnect();
	}
}
