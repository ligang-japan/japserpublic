package run;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import blobAzureApp.AzureApp;
import email.SendEmail;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import pdfMerge.PDFMerge;
import utilities.PropertyUtil;

/**
* JasperReportのサンプル
*
* .jasperファイルを読み込んでPDFに出力する
*/

public class GenerateReports {

	public static void main(String[] args) throws Exception {

		//zeroReportsは各帳票ごとのデータ数を格納するための配列です。
		int zeroReports[] = new int[5];
		for (int i = 0; i < 5; i++)
			zeroReports[i] = 0;

		if (!AzureApp.isImportedContainerAvailable()) {
			System.out.println("the imported flat files container is unavailable.");
			SendEmail.sendEmail(null, zeroReports);
			return;
		}
		if (!(AzureApp.isImportedFlatFilesAvailable("ANBOND.txt")
				|| AzureApp.isImportedFlatFilesAvailable("ANPAYSND.txt")
				|| AzureApp.isImportedFlatFilesAvailable("ANUNIT.txt"))) {
			System.out.println("Flat files are unavailable.");
			SendEmail.sendEmail(null, zeroReports);
			return;
		}
		// コンパイル済みファイルのパス
		Path output = Files.createTempDirectory("outputfolder");

		Connection con = null;
		CallableStatement stmt = null;
		String[] url = new String[5];
		List<String> data = new ArrayList();
		try {

			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy/MM/dd");//dd/MM/yyyy
			Date today = new Date();
			String strDate = sdfDate.format(today);
			// パラメータの作成
			Map paramMap = new HashMap();
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			String sasToken = AzureApp.GetFileSAS(PropertyUtil.getProperty("accountName"),
					PropertyUtil.getProperty("accountKey"), "");
			// データソースの生成
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			con = DriverManager.getConnection(
					PropertyUtil.getProperty("jdbcURL"), PropertyUtil.getProperty("account"),
					PropertyUtil.getProperty("passWord"));
			stmt = con.prepareCall("{Call [dbo].BulkInsertBlob(?,?,?,?,?)}");
			stmt.setString(1, "ANBOND.txt");
			stmt.setString(2, sasToken);
			stmt.setString(3, "https://zhupao.blob.core.windows.net/importedflatfiles");
			stmt.setString(4, "T_ANBOND_csv");
			stmt.setString(5, "T_TEMP");
			stmt.execute();
			stmt.close();

			stmt = con.prepareCall("{Call [dbo].BulkInsertBlob(?,?,?,?,?)}");
			stmt.setString(1, "ANPAYSND.txt");
			stmt.setString(2, sasToken);
			stmt.setString(3, "https://zhupao.blob.core.windows.net/importedflatfiles");
			stmt.setString(4, "T_ANPAYSND_csv");
			stmt.setString(5, "T_TEMP");
			stmt.execute();
			stmt.close();

			stmt = con.prepareCall("{Call [dbo].BulkInsertBlob(?,?,?,?,?)}");
			stmt.setString(1, "ANUNIT.txt");
			stmt.setString(2, sasToken);
			stmt.setString(3, "https://zhupao.blob.core.windows.net/importedflatfiles");
			stmt.setString(4, "T_ANUNIT_csv");
			stmt.setString(5, "T_TEMP");
			stmt.execute();
			stmt.close();
			AzureApp.copyBlobToArchive("importedflatfiles", "archivedflatfiles");
			stmt = con.prepareCall("{Call [dbo].[SplitRawData](?)}");

			// 表示形式を指定
			SimpleDateFormat spFormat = new SimpleDateFormat("yyyy/MM/dd");
			stmt.setString(1, spFormat.format(today));
			stmt.execute();
			stmt.close();

			stmt = con.prepareCall("{Call [dbo].[SplitRawDataBOND](?)}");

			stmt.setString(1, spFormat.format(today));
			stmt.execute();
			stmt.close();

			stmt = con.prepareCall("{Call [dbo].[SplitRawDataANUNIT](?)}");

			stmt.setString(1, spFormat.format(today));
			stmt.execute();
			stmt.close();

			//下記のストアドは完了通知書用のみ
			stmt = con.prepareCall("{Call [dbo].[GetPerTemplateRecordCount](?,?,?,?)}");
			stmt.registerOutParameter(1, Types.INTEGER);
			stmt.registerOutParameter(2, Types.INTEGER);
			stmt.registerOutParameter(3, Types.INTEGER);
			stmt.registerOutParameter(4, Types.INTEGER);
			stmt.execute();
			int count[] = new int[5];
			for (int i = 1; i < 5; i++) {
				count[i] = stmt.getInt(i);
			}
			stmt.close();
			zeroReports[0] = count[1] + count[2] + count[3] + count[4];
			//下記のストアドは年金証書など残った帳票用です。
			stmt = con.prepareCall("{Call [dbo].[GetOtherPerReportRecordCount](?,?,?)}");
			stmt.registerOutParameter(1, Types.INTEGER);
			stmt.registerOutParameter(2, Types.INTEGER);
			stmt.registerOutParameter(3, Types.INTEGER);
			stmt.execute();

			zeroReports[1] = stmt.getInt(1); //年金証書
			zeroReports[2] = stmt.getInt(2); //ID Password
			zeroReports[3] = stmt.getInt(3); //Unit
			stmt.execute();

			//下記のストアドは年金証書用のみ
			stmt = con.prepareCall("{Call [dbo].[GetPerTemplateRecordCountforBond](?,?,?,?)}");
			stmt.registerOutParameter(1, Types.INTEGER);
			stmt.registerOutParameter(2, Types.INTEGER);
			stmt.registerOutParameter(3, Types.INTEGER);
			stmt.registerOutParameter(4, Types.INTEGER);
			stmt.execute();
			int bondCount[] = new int[5];
			for (int i = 1; i < 5; i++) {
				bondCount[i] = stmt.getInt(i);
			}
			stmt.close();

			if ((zeroReports[0] + zeroReports[1] + zeroReports[2] + zeroReports[3]) != 0) {
				String jrxml = "reportSummary.jrxml";
				String destPath = output + "/reportSummary.pdf";
				InputStream input = GenerateReports.class.getResourceAsStream(jrxml);
				JasperDesign jd = JRXmlLoader.load(input);
				JasperReport reporte = JasperCompileManager.compileReport(jd);

				//（4）データの動的バインド
				//JasperPrint print = JasperFillManager.fillReport(jasperPath1, paramMap, con);
				JasperPrint print = JasperFillManager.fillReport(reporte, paramMap, con);

				//（5）PDFへ出力
				JasperExportManager.exportReportToPdfFile(print, destPath);
			}

			if (zeroReports[0] != 0) {
				for (int i = 1; i < 5; i++) {
					if (count[i] != 0) {
						// 出力するPDFファイルのパス
						String destPath = output + "/completionNoticeC" + i + ".pdf";
						String jrxml = "completionNoticeC" + i + ".jrxml";
						InputStream input = GenerateReports.class.getResourceAsStream(jrxml);
						JasperDesign jd = JRXmlLoader.load(input);
						JasperReport reporte = JasperCompileManager.compileReport(jd);

						//（4）データの動的バインド
						//JasperPrint print = JasperFillManager.fillReport(jasperPath1, paramMap, con);
						JasperPrint print = JasperFillManager.fillReport(reporte, paramMap, con);

						//（5）PDFへ出力
						JasperExportManager.exportReportToPdfFile(print, destPath);
					}
				}
				try {
					//Prepare input pdf file list as list of input stream.
					List<InputStream> inputPdfList = new ArrayList<InputStream>();
					for (int i = 1; i < 5; i++) {
						if (count[i] != 0) {
							inputPdfList.add(new FileInputStream(output + "/completionNoticeC" + i + ".pdf"));
						}
					}
					//Prepare output stream for merged pdf file.
					OutputStream outputStream = new FileOutputStream(output + "/completionNoticemerged.pdf");

					//call method to merge pdf files.
					PDFMerge.mergePdfFiles(inputPdfList, outputStream);
				} catch (Exception e) {
					e.printStackTrace();
				}

				//				writeToFile(data, output + "/dataForCheck.txt");
				//				rs.close();
				//				st.close();

				//AzureApp.copyBlobToArchive("vaprinting", "archivedpdfs");
				//				if (zeroReports[1] != 0) {
				//					String jrxml = "anBond.jrxml";
				//					String destPath = output + "/Bond.pdf";
				//					InputStream input = GenerateReports.class.getResourceAsStream(jrxml);
				//					JasperDesign jd = JRXmlLoader.load(input);
				//					JasperReport reporte = JasperCompileManager.compileReport(jd);
				//
				//					//（4）データの動的バインド
				//
				//					JasperPrint print = JasperFillManager.fillReport(reporte, paramMap, con);
				//
				//					//（5）PDFへ出力
				//					JasperExportManager.exportReportToPdfFile(print, destPath);
				//
				//				}

				if (zeroReports[1] != 0) {
					for (int i = 1; i < 5; i++) {
						if (bondCount[i] != 0) {
							// 出力するPDFファイルのパス
							String destPath = output + "/anBond" + i + ".pdf";
							String jrxml = "anBond" + i + ".jrxml";
							InputStream input = GenerateReports.class.getResourceAsStream(jrxml);
							JasperDesign jd = JRXmlLoader.load(input);
							JasperReport reporte = JasperCompileManager.compileReport(jd);

							//（4）データの動的バインド
							//JasperPrint print = JasperFillManager.fillReport(jasperPath1, paramMap, con);
							JasperPrint print = JasperFillManager.fillReport(reporte, paramMap, con);

							//（5）PDFへ出力
							JasperExportManager.exportReportToPdfFile(print, destPath);
						}
					}
					try {
						//Prepare input pdf file list as list of input stream.
						List<InputStream> inputPdfList = new ArrayList<InputStream>();
						for (int i = 1; i < 5; i++) {
							if (bondCount[i] != 0) {
								inputPdfList.add(new FileInputStream(output + "/anBond" + i + ".pdf"));
							}
						}
						//Prepare output stream for merged pdf file.
						OutputStream outputStream = new FileOutputStream(output + "/anBond.pdf");

						//call method to merge pdf files.
						PDFMerge.mergePdfFiles(inputPdfList, outputStream);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (zeroReports[2] != 0) {
					String jrxml = "userIDPassword.jrxml";
					String destPath = output + "/idPassword.pdf";
					InputStream input = GenerateReports.class.getResourceAsStream(jrxml);
					JasperDesign jd = JRXmlLoader.load(input);
					JasperReport reporte = JasperCompileManager.compileReport(jd);

					//（4）データの動的バインド
					//JasperPrint print = JasperFillManager.fillReport(jasperPath1, paramMap, con);
					JasperPrint print = JasperFillManager.fillReport(reporte, paramMap, con);

					//（5）PDFへ出力
					JasperExportManager.exportReportToPdfFile(print, destPath);
				}

				if (zeroReports[3] != 0) {
					String jrxml = "unitReport.jrxml";
					String destPath = output + "/unitReport.pdf";
					InputStream input = GenerateReports.class.getResourceAsStream(jrxml);
					JasperDesign jd = JRXmlLoader.load(input);
					JasperReport reporte = JasperCompileManager.compileReport(jd);

					//（4）データの動的バインド
					JasperPrint print = JasperFillManager.fillReport(reporte, paramMap, con);

					//（5）PDFへ出力
					JasperExportManager.exportReportToPdfFile(print, destPath);
				}
			}
			if ((zeroReports[0] + zeroReports[1] + zeroReports[2] + zeroReports[3]) != 0) {
				url[0] = AzureApp.uploadToAzureBlobStorage(output, "/reportSummary");
			}
			if (zeroReports[0] != 0) {
				url[1] = AzureApp.uploadToAzureBlobStorage(output, "/completionNoticemerged");
			}
			if (zeroReports[1] != 0) {
				url[2] = AzureApp.uploadToAzureBlobStorage(output, "/anBond");
			}
			if (zeroReports[2] != 0) {
				url[3] = AzureApp.uploadToAzureBlobStorage(output, "/idPassword");
			}
			if (zeroReports[3] != 0) {
				url[4] = AzureApp.uploadToAzureBlobStorage(output, "/unitReport");
			}
			SendEmail.sendEmail(url, zeroReports);
		} catch (Exception ex) {
			url = null;
			SendEmail.sendEmail(null, zeroReports);
			ex.printStackTrace();
		} finally {
			try {
				FileUtils.deleteDirectory(new File(output.toString()));
				con.close();
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void writeToFile(java.util.List list, String path) {
		BufferedWriter out = null;
		try {
			File file = new File(path);
			out = new BufferedWriter(new FileWriter(file, true));
			for (Object s : list) {
				out.write((String) s);
				out.newLine();

			}
			out.close();
		} catch (IOException e) {
		}
	}

}
