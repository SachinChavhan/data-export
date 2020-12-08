package com.saama.dao;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.postgresql.copy.CopyManager;
import org.postgresql.jdbc.PgConnection;
import com.saama.model.ExportParameter;
import com.saama.sftp.FileWriter;

public class PostgresCopyDao {

	private ExportParameter params;

	static Logger logger = Logger.getLogger(DbOperation.class);

	public PostgresCopyDao(ExportParameter params) {
		super();
		this.params = params;
	}

	public PgConnection getPgConnection() {

		Connection connection = null;

		PgConnection copyConnection = null;

		try {

			String url = "jdbc:postgresql://" + params.getHostname() + ":" + params.getPort() + "/"
					+ params.getDbName();

			logger.info("connecting to database ");

			connection = DriverManager.getConnection(url, params.getUser(), params.getPassword());

			copyConnection = connection.unwrap(PgConnection.class);

			return copyConnection;

		} catch (SQLException e) {

			logger.error("failed to establish databse connection : " + e);
		}
		return copyConnection;
	}

	public void copyToFile() {

		try (FileOutputStream fileOutputStream = new FileOutputStream(
				new File(params.getStoreLocation() + "/" + params.getReportName() + ".csv"));
				PgConnection conn = getPgConnection();) {

			CopyManager copyManager = new CopyManager(conn);

			copyManager.copyOut("COPY (" + params.getQuery() + " ) TO STDOUT WITH (FORMAT CSV, HEADER)",
					fileOutputStream);

		} catch (SQLException | IOException e) {

			e.printStackTrace();
		}
	}

	public void copyToFileStudyParallel() {

		ExecutorService pool = Executors.newFixedThreadPool(params.getDbHitThreads());

		List<Future> future = new ArrayList<Future>();

		FileWriter fwriter = new FileWriter();

		ResultSet rsData = null;
		Statement stmt = null;
		Connection con = null;

		try {
			con = HikariDataSourcePool.getConnection();

			logger.info("getting distinct studyids");

			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			long studyFetchstart = System.currentTimeMillis();

			rsData = stmt.executeQuery("select distinct studyid from " + params.getUsdmSchemaName() + "."
					+ params.getStudyTableName() + ";");

			logger.info("distinct study id fetch time : " + (System.currentTimeMillis() - studyFetchstart) + " ms");

			while (rsData.next()) {

				String studyid = rsData.getString("studyid");
				String query = getStudyWiseQuery(studyid);

				FileOutputStream fos = new FileOutputStream(new File(fwriter.createDir(params) + "/" + studyid));
				future.add(pool.submit(new PgCopyThreadRun(query, fos, studyid)));
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
		} catch (Exception e) {

			logger.error("failed to get data : " + e);
			e.printStackTrace();

		} finally {
			try {
				rsData.close();
				stmt.close();
				con.close();
				pool.shutdown();
				// FileTransfer.sftpClose();
				logger.info("database connection closed");
			} catch (SQLException e) {
				logger.info("failed to close database connection");
			}
		}

	}

	public void copyToFileStudySequential() {

		FileWriter fwriter = new FileWriter();

		ResultSet rsData = null;
		Statement stmt = null;
		Connection con = null;
		PgConnection pgconn = null;

		try {
			con = getConnection();

			pgconn = getPgConnection();
			
			CopyManager copyManager = new CopyManager(pgconn);
			
			logger.info("getting distinct studyids");

			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			long studyFetchstart = System.currentTimeMillis();

			rsData = stmt.executeQuery("select distinct studyid from " + params.getUsdmSchemaName() + "."
					+ params.getStudyTableName() + ";");

			logger.info("distinct study id fetch time : " + (System.currentTimeMillis() - studyFetchstart) + " ms");

			while (rsData.next()) {

				String studyid = rsData.getString("studyid");
				String query = getStudyWiseQuery(studyid);

				try (FileOutputStream fos = new FileOutputStream(new File(fwriter.createDir(params) + "/" + studyid));) {

					copyManager.copyOut("COPY (" + query + " ) TO STDOUT WITH (FORMAT CSV, HEADER)", fos);
				}
			}

		} catch (Exception e) {

			logger.error("failed to get data : " + e);
			e.printStackTrace();

		} finally {
			try {
				rsData.close();
				stmt.close();
				con.close();
				pgconn.close();
				// FileTransfer.sftpClose();
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
			query = m.replaceFirst(" WHERE ( studyid = '" + studyId + "') AND ");
		} else {
			query = query + " WHERE ( studyid = '" + studyId + "') ";
		}

		return query;
	}

	public CopyManager getCopyManager(Connection conn) {

		try {
			PgConnection pgcon = conn.unwrap(PgConnection.class);
			return new CopyManager(pgcon);
		} catch (SQLException e) {

			logger.error("Failed to get pg connection : " + e);
			e.printStackTrace();
		}
		return null;
	}

	private class PgCopyThreadRun implements Runnable {

		private String query;
		private FileOutputStream fos;
		private String studyid;

		public PgCopyThreadRun(String query, FileOutputStream fos, String studyid) {

			this.query = query;
			this.fos = fos;
			this.studyid = studyid;

		}

		public void run() {

			long start = System.currentTimeMillis();
			Connection connection = null;
			try {
				connection = HikariDataSourcePool.getConnection();
				CopyManager cm = getCopyManager(connection);
				cm.copyOut("COPY (" + query + " ) TO STDOUT WITH (FORMAT CSV, HEADER)", fos);

			} catch (SQLException | IOException e) {
				logger.error("Failed to handle file - " + e);
				e.printStackTrace();

			} finally {
				try {
					connection.close();
					fos.close();
					logger.info("Time to copy and write data studyid - " + studyid + " - "
							+ (System.currentTimeMillis() - start) + " ms");

				} catch (SQLException | IOException e) {
					logger.error("resources(db or file) closer error - " + e);
					e.printStackTrace();
				}
			}
		}
	}
	
	public Connection getConnection() {

		try {

			String url = "jdbc:postgresql://" + params.getHostname() + ":" + params.getPort() + "/"
					+ params.getDbName();

			logger.info("connecting to database ");

			Connection connection = DriverManager.getConnection(url, params.getUser(), params.getPassword());
			
			return connection;

		} catch (SQLException e) {

			logger.error("failed to establish databse connection : " + e);
		}

		return null;
	}
}
