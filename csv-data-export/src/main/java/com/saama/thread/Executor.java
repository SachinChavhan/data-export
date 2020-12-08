package com.saama.thread;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.opencsv.CSVWriter;
import com.saama.constants.DbWriteConstant;
import com.saama.model.ExportParameter;

public class Executor {

	static Logger logger = Logger.getLogger(Executor.class);

	public void concurrentWriting(ResultSet rs, ExportParameter params) {

		ExecutorService pool = Executors.newFixedThreadPool(params.getWriteThreads());

		List<Future> future = new ArrayList<Future>();
		
		String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

		CSVWriter csvWriter = null;

		long start = System.currentTimeMillis();

		try {

			ResultSetMetaData metaData = rs.getMetaData();

			int columnCount = metaData.getColumnCount();

			String[] header = new String[columnCount];

			for (int i = 0; i < columnCount; i++) {

				header[i] = metaData.getColumnName(i + 1);
			}

			Path path = Paths.get(params.getStoreLocation() + "/" + params.getReportName().concat("_" + date) + "."
					+ params.getFileType());

			Writer writer = Files.newBufferedWriter(path);

			csvWriter = new CSVWriter(writer);

			csvWriter.writeNext(header);

			int rowCount = 0;

			if (rs.last()) {

				rowCount = rs.getRow();
				logger.info("total row count : " + rowCount);
			}
			rs.beforeFirst();

			if (rs.isBeforeFirst()) {
				//logger.info("moved pointer at first row");
			}

			int numOfLoop = rowCount / DbWriteConstant.BATCH_SIZE;

			while (numOfLoop > 0) {

				List<String[]> entries = new ArrayList<>();

				for (int j = 0; j < DbWriteConstant.BATCH_SIZE; j++) {

					rs.next();
					String[] row = new String[columnCount];

					for (int i = 0; i < columnCount; i++) {
						String string = rs.getString(i + 1);
						if (string == null) {
							string = "";
						}
						row[i] = string;
					}
					entries.add(row);

				}

				numOfLoop--;

				future.add(pool.submit(new RunImpl(entries, csvWriter)));
				//csvWriter.writeAll(entries);

			}

			List<String[]> entries = new ArrayList<>();

			while (rs.next()) {

				String[] row = new String[columnCount];

				for (int i = 0; i < columnCount; i++) {
					String string = rs.getString(i + 1);
					if (string == null) {
						string = "";
					}
					row[i] = string;
				}

				entries.add(row);
			}

			future.add(pool.submit(new RunImpl(entries, csvWriter)));
			//csvWriter.writeAll(entries);

			for (Future s : future) {
				try {
					s.get();
				} catch (InterruptedException e) {

					logger.error("Failed to get status of threads -  error : ", e);

				} catch (ExecutionException e) {

					logger.error("thread execution -  error : ", e);
				}
			}
			rs.close();

		} catch (SQLException e) {

			logger.error("execution error : ", e);

		} catch (IOException e) {

			logger.error("execution error : ", e);

		} finally {
			try {
				logger.info("file write time : "+params.getReportName()+" : " + (System.currentTimeMillis() - start) + " ms");
				csvWriter.close();
				//logger.info("closed csv file writer");
			} catch (IOException e) {

				logger.error("execution error : ", e);
			}
		}

		pool.shutdown();
	}

	private class RunImpl implements Runnable {

		private List<String[]> rows;
		private CSVWriter csvWriter;

		public RunImpl(List<String[]> rows, CSVWriter csvWriter) {
			super();
			this.rows = rows;
			this.csvWriter = csvWriter;
		}

		public void run() {
            csvWriter.writeAll(rows);
		}
	}
}
