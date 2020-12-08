package com.saama;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

import com.saama.constants.DbWriteConstant;
import com.saama.dao.HikariDataSourcePool;
import com.saama.dao.PostgresCopyDao;
import com.saama.dao.DbOperation;
import com.saama.model.ExportParameter;
import com.saama.sftp.FileTransfer;
import com.saama.sftp.FileWriter;
import com.saama.thread.Executor;

public class Application {

	static Logger logger = Logger.getLogger(Application.class);

	public static void main(String[] args) throws IOException {

		long start = System.currentTimeMillis();

		ExportParameter params = new ExportParameter();

		String propertiesPath = args[0].split("=")[1];

		FileInputStream fis = null;

		try {
			fis = new FileInputStream(propertiesPath);
			Properties prop = new Properties();
			prop.load(fis);
			params.setDbName(prop.getProperty(DbWriteConstant.DB_NAME_VARIABLE));
			params.setUser(prop.getProperty(DbWriteConstant.DB_USER_VARIABLE));
			params.setPort(prop.getProperty(DbWriteConstant.DB_PORT_VARIABLE));
			params.setHostname(prop.getProperty(DbWriteConstant.DB_HOSTNAME_VARIABLE));
			params.setPassword(prop.getProperty(DbWriteConstant.DB_PASSWORD_VARIABLE));
			params.setStoreLocation(prop.getProperty(DbWriteConstant.STORE_DIR_VARIABLE));
			params.setReportName(prop.getProperty(DbWriteConstant.FILE_NAME_VARIABLE));
			params.setQuery(prop.getProperty(DbWriteConstant.QUERY_NAME_VARIABLE));
			params.setFileType(prop.getProperty(DbWriteConstant.FILE_TYPE_VARIABLE));
			params.setWriteThreads(Integer.parseInt(
					prop.getProperty(DbWriteConstant.WRITE_THREAD_NAME_VARIABLE, DbWriteConstant.FILE_WRITE_THREADS)));
			params.setDbHitThreads(Integer.parseInt(
					prop.getProperty(DbWriteConstant.DB_HIT_THREAD_NAME_VARIABLE, DbWriteConstant.DB_HIT_THREADS)));
			params.setStudyWise(
					Boolean.parseBoolean(prop.getProperty(DbWriteConstant.IS_STUDY_WISE_VARIABLE, "false")));
			params.setUsdmSchemaName(prop.getProperty(DbWriteConstant.SCHEMA_VARIABLE, "cqs"));
			params.setStudyTableName(prop.getProperty(DbWriteConstant.STUDY_TABLE_VARIABLE, "study"));
			params.setDbMaximumPoolSize(Integer.parseInt(prop.getProperty("db.maxpoolsize", "10")));
			params.setDbMinimumIdle(Integer.parseInt(prop.getProperty("db.minidle", "30")));
			params.setDbLeakDetectionThreshold(Integer.parseInt(prop.getProperty("db.leakdetectionthreshold", "30")));
			DbWriteConstant.isDbHitParallelForStudy = Boolean.parseBoolean(prop.getProperty("isdbhitparallel", "false"));
			DbWriteConstant.DB_HIT_THREADS = prop.getProperty(DbWriteConstant.DB_HIT_THREAD_NAME_VARIABLE, DbWriteConstant.DB_HIT_THREADS);
			params.setSftpUser(prop.getProperty(DbWriteConstant.SFTP_USER));
			params.setSftpPassword(prop.getProperty(DbWriteConstant.SFTP_PASSWORD));
			params.setSftpHost(prop.getProperty(DbWriteConstant.SFTP_HOST));
			params.setSftpDestPath(prop.getProperty(DbWriteConstant.SFTP_DEST));
		} catch (FileNotFoundException e) {
			logger.error("Unable to find properties file on location : " + propertiesPath + " error " + e);
		} catch (IOException ioe) {
			logger.error("Failed to read properties file  error " + ioe);
		} finally {
			fis.close();
		}
		

		//DbOperation ops = new DbOperation(params);
		PostgresCopyDao pcd = new PostgresCopyDao(params);
		
		if (params.isStudyWise() && DbWriteConstant.isDbHitParallelForStudy) {

			logger.info("processing started with studywise db hit parallel");
			HikariDataSourcePool.getDataSource(params);
			//FileTransfer.createSftpSession(params.getSftpUser(), params.getSftpPassword(), params.getSftpHost());
			//FileTransfer.mkdir(params);
			//ops.getStudyWiseDataParallel();
			pcd.copyToFileStudyParallel();

		} else if(params.isStudyWise()){
			
			logger.info("processing started with studywise db hit sequential");
			//ops.getStudyWiseDataSequential();
			pcd.copyToFileStudySequential();
		
		} else {
			
            logger.info("processing started with whole data");
			//ResultSet rs = ops.getDbData();
			//Executor exe = new Executor();
			//exe.concurrentWriting(rs, params);
			/*FileWriter fw = new FileWriter();
			String filePath = params.getStoreLocation() + "/" + params.getReportName().concat("_2020") +"/"+ params.getReportName()+ "."
					+ params.getFileType();
			fw.csvFileWrite(rs, params, filePath);*/
            pcd.copyToFile();
		}

		//PostgresCopy pgcp = new PostgresCopy(params);
		//pgcp.getData();
		logger.info("Total time taken to fetch and write : " + (System.currentTimeMillis() - start) + " ms");

	}
}
