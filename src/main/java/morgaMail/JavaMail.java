package morgaMail;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class JavaMail {
	// ------------------------ 基本資料
	// 第三方禁止使用，以防帳號密碼被盜用(即使密碼輸入正確)
	// ------------------------ 
	private final String PROPERTY_PATH = "C:/temp/Morga/PropertyDir/Mail.properties";
	private static final java.lang.String SENDERMAIL = "mail.username";
	private static final java.lang.String SENDERPWD = "mail.password";
	private static final java.lang.String RECIPIENTMAIL = "mail.customer";
	private static final java.lang.String SUBJECT = "mail.subject"
			+ "";
	private static final java.lang.String MAILTEXT = "mail.txt";
	
	public static void main(String[] args) {
		JavaMail mail = new JavaMail();
		mail.sendMail();
	}
	
	public void sendMail() {
		System.out.println("------------ Sending Mail ------------");
		try {
			Properties userProp = this.getUserProperty(PROPERTY_PATH);
			this._sendMail(userProp);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("------------ Closing Mail ------------");			
		}
	}
	
	private void _sendMail(Properties userProp) throws Exception{
		// ------------------------ 基本資料(我用properties檔作為configure)
		String userName = (String) userProp.get(SENDERMAIL); 	// 寄件者 email
		String password = (String) userProp.get(SENDERPWD); 	// 帳號者密碼(經email設定獲得)
		String customer = (String) userProp.get(RECIPIENTMAIL); // 收件者 email
		String subject = (String) userProp.get(SUBJECT); 		// 信件標題
		String txt = (String) userProp.get(MAILTEXT); 			// 信件內容
		
		// ------------------------------------------------
		// ------------  以下為gmail的連線設定  -------------
		// ------------------------------------------------
		
		Properties gmailProp = this.getGamilProperty();
		
		// 驗證可以透過實作Class的方式，在內部添加一些驗證的手法
		// ------------------------ 3.帳號驗證
		
		// 透過上述javaMail的prop設定，對gmail進行Session設定放入後，再透過伺服器的session進行發送設定即可完成
		// 由於Session API的說明內無Constructor，因此Session物件無法透過new方式獲得，只能使用內有的Method設定
		// gmail驗證嚴格，透過Session Static Method獲取需要再使用Author設定驗證
		// ------------------------ 4.Session java mail api 默認設定
		
//		// 1.匿名類別 <設定給PasswordAuthentication物件，透過這個類別給api使用，new Authenticator裡面使用> (右鍵)Source>Override設定
//		Session session = Session.getDefaultInstance(prop, new Authenticator() {
//			@Override
//			protected PasswordAuthentication getPasswordAuthentication() {
//				return new PasswordAuthentication(userName, password);
//			}
//		});
		
		// 2. 設定 新的Class去實作這個impletment
		Auth auth = new Auth(userName, password);
		Session session = Session.getDefaultInstance(gmailProp, auth);
		
		// ------------------------ 5.Message 放入基本資料
		MimeMessage mimeMessage = new MimeMessage(session);
		Transport transport = null;
		
		try {
			// 寄件者
			mimeMessage.setSender(new InternetAddress(userName));
			// 收件者
			// BCC / CC / TO 匿名收件/秘密收件/收件
			mimeMessage.setRecipient(RecipientType.TO, new InternetAddress(customer));
			// 標題
			mimeMessage.setSubject(subject);
			// 內容/格式
			// Object(Type) 設定為 html的title設定(類似)
			mimeMessage.setContent(txt, "text/html;charset=UTF-8");
			
			// ------------------------ 6.Transport 將 Message 傳出
			transport = session.getTransport();
			transport.send(mimeMessage);
			
		} catch (AddressException e) {
			System.out.println(e.getMessage());
			throw e;
		} catch (MessagingException e) {
			System.out.println(e.getMessage());
			throw e;
		} finally {
			if(transport != null) {
				transport.close();				
			}
		}
	}

	private Properties getUserProperty(String propertyPath) throws Exception {
		Properties prop = new Properties();
		InputStream inStream = null;
		InputStreamReader inReader = null;
		try {
			inStream = new FileInputStream(propertyPath);
			inReader = new InputStreamReader(inStream, StandardCharsets.UTF_8);
			prop.load(inReader);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw e;
		} finally {
			if(inStream != null) {
				inStream.close();				
			}
			if(inReader != null) {
				inReader.close();				
			}
		}
		return prop;
	}
	

	private Properties getGamilProperty() {
		// ------------------------ 1.連線設定
		Properties gmailProp = new  Properties();
		
		// ------------------------ 2.需求設定
		// 設定連線方式為smtp
		gmailProp.setProperty("mail.transport.protocol", "smtp");

		// host: smtp.gmail.com
		gmailProp.setProperty("mail.host", "smtp.gmail.com");
		
		// host port 465
		gmailProp.put("mail.smtp.port", "465");

		// 寄件者帳號驗證 : 是
		gmailProp.put("mail.smtp.auth", "true");
		
		// 我們不是透過gmail直接連線，而是對javaMail這們API實作再進行連線到gmail，因次我們要先設定javaMail的實作區域設定進行安全連線
		// 需要安全資料傳輸層 (SSL)：是
		gmailProp.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		
		// 安全資料傳輸層 (SSL) 通訊埠：465
		gmailProp.put("mail.smtp.socketFactory.port", "465");
		
		// 預設初始化資料，一般預設值為false
		gmailProp.put("mail.debug", "true");
		
		// ------------------------ 不需要設定 
		// 需要傳輸層安全性 (TLS)：是 (如果可用)
		// 傳輸層安全性 (TLS)/STARTTLS 通訊埠：587
		// ------------------------
		return gmailProp;
	}
}

class Auth extends Authenticator{
	private String userName;
	private String password;
	
	public Auth(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		PasswordAuthentication pa = new PasswordAuthentication(userName, password);
		return pa;
	}
}


