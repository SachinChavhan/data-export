package com.saama.dao;

import java.sql.*;

import org.apache.log4j.Logger;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.postgresql.jdbc.PgConnection;

import com.saama.Application;
import com.saama.model.ExportParameter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariDataSourcePool {

	private static HikariConfig config = new HikariConfig();
	private static HikariDataSource ds;

	static Logger logger = Logger.getLogger(Application.class);

	public static void getDataSource(ExportParameter dbToFileParam) {

		logger.info("creating connection pool");

		String url = "jdbc:postgresql://" + dbToFileParam.getHostname() + ":" + dbToFileParam.getPort() + "/"
				+ dbToFileParam.getDbName();

		config.setJdbcUrl(url);
		config.setUsername(dbToFileParam.getUser());
		config.setPassword(dbToFileParam.getPassword());
		config.setMaximumPoolSize(dbToFileParam.getDbMaximumPoolSize());
		config.setMinimumIdle(dbToFileParam.getDbMinimumIdle());
		config.setLeakDetectionThreshold(dbToFileParam.getDbLeakDetectionThreshold());
		config.setDriverClassName("org.postgresql.Driver");
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		ds = new HikariDataSource(config);
	}

	public static Connection getConnection() throws SQLException {
		return ds.getConnection();
	}

	private HikariDataSourcePool() {
	}
}
