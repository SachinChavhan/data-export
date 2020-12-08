package com.saama.model;

public class ExportParameter {
	
	private String dbName;
	private String user;
	private String password;
	private String hostname;
	private String query;
	private String storeLocation;
	private String port;
	private String reportName;
	private int threads;
	private boolean isStudyWise;
	private String usdmSchemaName;
	private String studyTableName;
	private String studyId;
	private int writeThreads;
	private int dbHitThreads;
	private int dbMaximumPoolSize;
	private int dbMinimumIdle;
	private int dbLeakDetectionThreshold;
	private String sftpUser;
	private String sftpPassword;
	private String sftpHost;
	private String sourcePath;
	private String sftpDest;
	
	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getSftpDestPath() {
		return sftpDest;
	}

	public void setSftpDestPath(String destPath) {
		this.sftpDest = destPath;
	}

	public String getSftpUser() {
		return sftpUser;
	}

	public void setSftpUser(String sftpUser) {
		this.sftpUser = sftpUser;
	}

	public String getSftpPassword() {
		return sftpPassword;
	}

	public void setSftpPassword(String sftpPassword) {
		this.sftpPassword = sftpPassword;
	}

	public String getSftpHost() {
		return sftpHost;
	}

	public void setSftpHost(String sftpHost) {
		this.sftpHost = sftpHost;
	}

	public int getDbMaximumPoolSize() {
		return dbMaximumPoolSize;
	}

	public void setDbMaximumPoolSize(int dbMaximumPoolSize) {
		this.dbMaximumPoolSize = dbMaximumPoolSize;
	}

	public int getDbMinimumIdle() {
		return dbMinimumIdle;
	}

	public void setDbMinimumIdle(int dbMinimumIdle) {
		this.dbMinimumIdle = dbMinimumIdle;
	}

	public int getDbLeakDetectionThreshold() {
		return dbLeakDetectionThreshold;
	}

	public void setDbLeakDetectionThreshold(int dbLeakDetectionThreshold) {
		this.dbLeakDetectionThreshold = dbLeakDetectionThreshold;
	}

	public String getUsdmSchemaName() {
		return usdmSchemaName;
	}

	public void setUsdmSchemaName(String usdmSchemaName) {
		this.usdmSchemaName = usdmSchemaName;
	}

	public String getStudyTableName() {
		return studyTableName;
	}

	public void setStudyTableName(String studyTableName) {
		this.studyTableName = studyTableName;
	}

	public boolean isStudyWise() {
		return isStudyWise;
	}

	public void setStudyWise(boolean isStudyWise) {
		this.isStudyWise = isStudyWise;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	private String fileType;
	
	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public ExportParameter() {
		
	}
	
	public ExportParameter(String dbName, String user, String password, String hostname, String query,
			String storeLocation,String port) {
		super();
		this.dbName = dbName;
		this.user = user;
		this.password = password;
		this.hostname = hostname;
		this.query = query;
		this.storeLocation = storeLocation;
		this.port = port;
	}
	
	public String getDbName() {
		return dbName;
	}
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public String getStoreLocation() {
		return storeLocation;
	}
	public void setStoreLocation(String storeLocation) {
		this.storeLocation = storeLocation;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public String getStudyId() {
		return studyId;
	}

	public void setStudyId(String studyId) {
		this.studyId = studyId;
	}

	public int getWriteThreads() {
		return writeThreads;
	}

	public void setWriteThreads(int writeThreads) {
		this.writeThreads = writeThreads;
	}

	public int getDbHitThreads() {
		return dbHitThreads;
	}

	public void setDbHitThreads(int dbHitThreads) {
		this.dbHitThreads = dbHitThreads;
	}

}
