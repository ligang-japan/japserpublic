package email;

import java.util.Date;
import java.util.Properties;

//import javax.mail.Message;
//import javax.mail.MessagingException;
//import javax.mail.PasswordAuthentication;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.InternetHeaders;
//import javax.mail.internet.MimeBodyPart;
//import javax.mail.internet.MimeMessage;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendEmail {

	public static void sendEmail(String[] url, int[] zeroReports) {

		final String username = "NPA001VAPRINTING@nngroup.onmicrosoft.com";
		final String password = "Xano8808";
		final String toAddress = "gang.li@nnlife.co.jp";

		int reportCount =0;
		for (int i=0; i<5;i++) {
			reportCount= reportCount + zeroReports[i];
		}

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.office365.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.socketFactory.port", "587");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");


		try {
			Authenticator auth = new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			};

			Session session = Session.getInstance(prop, auth);

			// creates a new e-mail message
			Message message = new MimeMessage(session);

			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("gang.li@nnlife.co.jp"));
			message.setSubject("[VA Printing]出力PDFファイル作成完了のお知らせ(POC)");
			message.setSentDate(new Date());
			String msg = "";
			if(reportCount==0) {
				msg = "本日出力対象データはありませんでした。";
			} else if ((url== null || url[0] == null || url[0].isEmpty()) || (url[1] == null || url[1].isEmpty())) {
				msg = "エラーが発生しました。アプリケーションサポート担当者にご連絡ください";

			} else {
				msg = "ご担当者様<BR><BR>下記本日分の出力です。<BR><BR>";
				msg =  msg + "<a href='" + url[0] + "'>本日分の帳票出力サマリ</a><BR>";
				if (url[1] != null) {
				msg =  msg + "<a href='" + url[1] + "'>本日分の支払完了通知書PDFファイル</a><BR>";
				}
				//をクリックして、ダウンロードしてください。
				if (url[2]!= null) {
					msg =msg +  "<a href='" + url[2] + "'>本日分の年金証書PDFファイル</a> <BR>";
				}
				if (url[3]!= null) {
					msg =msg + "<a href='" + url[3] + "'>本日分のIDパスワードPDFファイル</a> <BR>";
				}
				if (url[4]!= null) {
					msg =msg + "<a href='" + url[4] +"'>本日分ユニット取引報告書PDFファイル</a> <BR>";
				}

			}
			  msg = msg + "<BR><BR>VA Printingサポートチーム";

			// sends the e-mail
			// set plain text message
			message.setContent(msg, "text/html; charset=utf-8");
			message.setHeader("Content-Transfer-Encoding", "base64");
			Transport.send(message);

			System.out.println("Done");

		}catch(

	MessagingException e)
	{
		e.printStackTrace();
	}
}

}
