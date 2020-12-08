package com.saama.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.postgresql.jdbc.PgConnection;

import com.saama.model.ExportParameter;
import com.saama.sftp.FileTransfer;
import com.saama.sftp.FileWriter;
import com.saama.thread.Executor;

public class DbOperation {

	private ExportParameter params;
	private Connection connection;

	static Logger logger = Logger.getLogger(DbOperation.class);

	public DbOperation(ExportParameter dbWriteParam) {
		super();
		this.params = dbWriteParam;
	}

	public Connection getConnection() {

		try {

			String url = "jdbc:postgresql://" + params.getHostname() + ":" + params.getPort() + "/"
					+ params.getDbName();

			logger.info("connecting to database ");

			connection = DriverManager.getConnection(url, params.getUser(), params.getPassword());
			
			return connection;

		} catch (SQLException e) {

			logger.error("failed to establish databse connection : " + e);
		}

		return connection;
	}

	public ResultSet getDbData() {

		try {
			Connection connection = getConnection();
			connection.setAutoCommit(false);
			logger.info("executing query using createStatement");
			Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			// stmt.setFetchSize(2000);
			long start = System.currentTimeMillis();
			ResultSet rs = stmt.executeQuery(params.getQuery() + ";");
			logger.info("Time taken to execute query : " + (System.currentTimeMillis() - start) + " ms");
			return rs;

		} catch (Exception e) {

			logger.error("failed to get data : " + e);
			return null;

		} finally {
			try {
				connection.close();
				logger.info("database connection closed");
			} catch (SQLException e) {
				logger.info("failed to close database connection");
			}
		}

	}

	public void getStudyWiseDataSequential() {

		Statement stmt = null;
		ResultSet rsStudy = null;
		ResultSet rsData = null;
		Connection connection = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			logger.info("getting distinct study id from db");
			stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			long studyFetchstart = System.currentTimeMillis();
			rsStudy = stmt.executeQuery("select distinct studyid from " + params.getUsdmSchemaName() + "."
					+ params.getStudyTableName());
			logger.info("distinct study id fetch time : " + (System.currentTimeMillis() - studyFetchstart) + " ms");
			while (rsStudy.next()) {
				String studyid = rsStudy.getString("studyid");
				String query = getStudyWiseQuery(studyid);
				logger.info("fetching data for study : " + studyid);
				long dataFetchstart = System.currentTimeMillis();
				rsData = stmt.executeQuery(query + ";");
				logger.info("data fetch time for study id : " + studyid + " : "
						+ (System.currentTimeMillis() - dataFetchstart) + " ms");
				Executor exe = new Executor();
				params.setReportName(studyid);
				exe.concurrentWriting(rsData, params);
			}

		} catch (Exception e) {

			logger.error("failed to get data : " + e);

		} finally {
			try {
				rsStudy.close();
				stmt.close();
				rsData.close();
				connection.close();
				logger.info("database connection closed");
			} catch (SQLException e) {
				logger.info("failed to close database connection");
			}
		}

	}

	public void getStudyWiseDataParallel() {

		ExecutorService pool = Executors.newFixedThreadPool(params.getDbHitThreads());

		List<Future> future = new ArrayList<Future>();

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rsStudy = null;
		try {
			conn = HikariDataSourcePool.getConnection();
			conn.setAutoCommit(false);
			logger.info("getting distinct study id from db");
			stmt = conn.prepareStatement(
					"select distinct studyid from " + params.getUsdmSchemaName() + "."
							+ params.getStudyTableName(),
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			long studyFetchstart = System.currentTimeMillis();
			rsStudy = stmt.executeQuery();
			logger.info("distinct study id fetch time : " + (System.currentTimeMillis() - studyFetchstart) + " ms");
			while (rsStudy.next()) {
				String studyid = rsStudy.getString("studyid");
				String query = getStudyWiseQuery(studyid);
				future.add(pool.submit(new RunImpl(query, studyid, params)));
			}

			for (Future s : future) {
				try {
					s.get();
				} catch (InterruptedException e) {

					logger.error("Failed to get status of threads -  error : ", e);

				} catch (ExecutionException e) {

					logger.error("thread execution -  error : ", e);
				}
			}

			/*FileWriter fw = new FileWriter();
			
			String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			String filePath = params.getStoreLocation() + "\\"
					+ params.getReportName().concat("_" + date) + "\\" + "_SUCCESS";
			fw.createSuccessFile(filePath);
			FileTransfer.uploadFile(filePath, params.getSftpDestPath());*/
		} catch (Exception e) {

			logger.error("failed to get data : " + e);
			e.printStackTrace();

		} finally {
			try {
				rsStudy.close();
				stmt.close();
				conn.close();
				pool.shutdown();
				//FileTransfer.sftpClose();
				logger.info("database connection closed");
			} catch (SQLException e) {
				logger.info("failed to close database connection");
			}
		}

	}

	public String getStudyWiseQuery(String studyId) {

		String query = params.getQuery();

		if (query.toLowerCase().contains("where")) {
			Pattern p = Pattern.compile("where", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(query);
			// query = m.replaceFirst(
			// " WHERE (" + dbToFileParam.getStudyTableName() + "_.studyid = '" + studyId +
			// "') AND ");
			query = m.replaceFirst(" WHERE ( studyid = '" + studyId + "') AND ");
		} else {
			// query = query + " WHERE (" + dbToFileParam.getStudyTableName() + "_.studyid =
			// '" + studyId + "')";
			query = query + " WHERE ( studyid = '" + studyId + "') ";
		}

		return query;
	}

	private class RunImpl implements Runnable {

		private String query;
		private String studyId;
		private ExportParameter dbToFileParam;

		public RunImpl(String query, String studyId, ExportParameter dbToFileParam) {
			super();
			this.query = query;
			this.studyId = studyId;
			this.dbToFileParam = dbToFileParam;
		}

		public void run() {

			ResultSet rsData = null;
			Statement stmt = null;
			Connection con = null;
			String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

			try {
				logger.info("fetching data for study : " + studyId);
				long dataFetchstart = System.currentTimeMillis();
				con = HikariDataSourcePool.getConnection();

				stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

				// stmt = con.prepareStatement(query + ";", ResultSet.TYPE_SCROLL_INSENSITIVE,
				// ResultSet.CONCUR_READ_ONLY);
				// rsData = stmt.executeQuery();
				stmt.setFetchSize(2000);
				rsData = stmt.executeQuery(query + ";");
				logger.info("data fetch time for study id : " + studyId + " : "
						+ (System.currentTimeMillis() - dataFetchstart) + " ms");
				/*
				 * Executor exe = new Executor(); dbToFileParam.setReportName(studyId);
				 * exe.concurrentWriting(rsData, dbToFileParam);
				 */

				FileWriter fw = new FileWriter();
				String filePath = dbToFileParam.getStoreLocation() + "\\"
						+ dbToFileParam.getReportName().concat("_" + date) + "\\" + studyId;//  + "."+ dbToFileParam.getFileType();

				String sftppath = fw.csvFileWrite(rsData, dbToFileParam, filePath);
				//FileTransfer.uploadFile(sftppath+".zip", dbToFileParam.getSftpDestPath());

			} catch (Exception e) {
				logger.error("failed to get data : " , e);
				e.printStackTrace();

			} finally {
				//logger.info("closing stmt : ");
				try {
					stmt.close();
					rsData.close();
					con.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}
}
