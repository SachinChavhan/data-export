package com.saama.sftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.opencsv.CSVWriter;
import com.saama.constants.DbWriteConstant;
import com.saama.model.ExportParameter;

public class FileWriter {

	static Logger logger = Logger.getLogger(FileWriter.class);

	public String csvFileWrite(ResultSet rs, ExportParameter params, String filePath) {

		CSVWriter csvWriter = null;

		long start = System.currentTimeMillis();
		String zipData = "";

		try {

			/*
			 * ResultSetMetaData metaData = rs.getMetaData();
			 * 
			 * int columnCount = metaData.getColumnCount();
			 * 
			 * String[] header = new String[columnCount];
			 * 
			 * for (int i = 0; i < columnCount; i++) {
			 * 
			 * header[i] = metaData.getColumnName(i + 1); }
			 */

			Path path = Paths.get(filePath);
			Path dir = Files.createDirectories(path.getParent());

			Writer writer = null;

			if (!Files.exists(path)) {

				Path writePath = Files.createFile(path);
				writer = Files.newBufferedWriter(writePath);

			} else {

				writer = Files.newBufferedWriter(path);
			}

			csvWriter = new CSVWriter(writer);

			// csvWriter.writeNext(header);

			// boolean rowCheck = true;

			/*
			 * while (rs.next()) {
			 * 
			 * List<String[]> entries = new ArrayList<>();
			 * 
			 * for (int j = 0; j < DbWriteConstant.BATCH_SIZE && rowCheck; j++) {
			 * 
			 * String[] row = new String[columnCount];
			 * 
			 * for (int i = 0; i < columnCount; i++) { String string = rs.getString(i + 1);
			 * if (string == null) { string = ""; } row[i] = string; } entries.add(row);
			 * rowCheck = rs.next();
			 * 
			 * }
			 * 
			 * csvWriter.writeAll(entries);
			 * 
			 * }
			 */

			csvWriter.writeAll(rs, true);
			/*
			 * Path downPath = Paths.get(filePath + "/../..").toRealPath(); Path zipPath =
			 * Paths.get(downPath.toString() + "/zipdata"); Path zipdir =
			 * Files.createDirectories(zipPath); zipData = zipdir.toString() +"/"+
			 * path.getFileName().toString(); String zipDataPath =
			 * Paths.get(zipData).toString(); zipData = zipDataPath.toString();
			 * zipFile(filePath, zipDataPath);
			 */

		} catch (SQLException e) {

			logger.error("execution error : ", e);

		} catch (IOException e) {

			logger.error("execution error : ", e);

		} finally {
			try {
				logger.info("file write time : " + params.getReportName() + " : " + (System.currentTimeMillis() - start)
						+ " ms");
				csvWriter.close();
			} catch (IOException e) {

				logger.error("execution error : ", e);
			}
		}
		return zipData;
	}

	public void zipFile(String filePath, String fileName) {

		FileOutputStream fos = null;
		ZipOutputStream zipOut = null;
		FileInputStream fis = null;
		try {
			File input = new File(filePath);
			fos = new FileOutputStream(fileName + ".zip");
			zipOut = new ZipOutputStream(new BufferedOutputStream(fos));
			fis = new FileInputStream(input);
			ZipEntry ze = new ZipEntry(input.getName());
			// System.out.println("Zipping the file: "+input.getName());
			zipOut.putNextEntry(ze);
			byte[] tmp = new byte[4 * 1024];
			int size = 0;
			while ((size = fis.read(tmp)) != -1) {
				zipOut.write(tmp, 0, size);
			}
			zipOut.flush();
			zipOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (fos != null)
					fos.close();
				if (fis != null)
					fis.close();
			} catch (Exception ex) {

			}
		}
	}

	public void createSuccessFile(String filePath) {
		Path path = Paths.get(filePath);
		try {
			Path writePath = Files.createFile(path);
		} catch (IOException e) {
			logger.info("Failed to create _SUCCESS file");
			e.printStackTrace();
		}
	}

	public String createDir(ExportParameter params) {

		String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

		String str_dir = params.getStoreLocation() + "/" + params.getReportName() + "/" + date ;

		Path path = Paths.get(str_dir);

		try {

			Path directory = Files.createDirectories(path);
			return directory.toString();

		} catch (IOException e) {

			logger.error("Failed to create directort : " + str_dir);
			e.printStackTrace();
		}
		return "";
	}
}
