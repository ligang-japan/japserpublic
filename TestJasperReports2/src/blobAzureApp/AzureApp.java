package blobAzureApp;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FilenameUtils;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import utilities.PropertyUtil;


/* *************************************************************************************************************************
* Summary: This application demonstrates how to use the Blob Storage service.
* It does so by creating a container, creating a file, then uploading that file, listing all files in a container,
* and downloading the file. Then it deletes all the resources it created
*
* Documentation References:
* Associated Article - https://docs.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java
* What is a Storage Account - http://azure.microsoft.com/en-us/documentation/articles/storage-whatis-account/
* Getting Started with Blobs - http://azure.microsoft.com/en-us/documentation/articles/storage-dotnet-how-to-use-blobs/
* Blob Service Concepts - http://msdn.microsoft.com/en-us/library/dd179376.aspx
* Blob Service REST API - http://msdn.microsoft.com/en-us/library/dd135733.aspx
* *************************************************************************************************************************
*/
public class AzureApp {
	/* *************************************************************************************************************************
	* Instructions: Start an Azure storage emulator, such as Azurite, before running the app.
	*    Alternatively, remove the "UseDevelopmentStorage=true;"; string and uncomment the 3 commented lines.
	*    Then, update the storageConnectionString variable with your AccountName and Key and run the sample.
	* *************************************************************************************************************************
	*/
//	public static final String StorageConnectionString =
//			"DefaultEndpointsProtocol=https;AccountName=zhupao;AccountKey=d0b0z0URvrbtxe1Jn4ozTD8qNQb2gLwhbdXw0Bk4oIxeTOAlguwKOE3sYyVgU6hPpfWh2nWy0AYIV7W8RWbqFw==;EndpointSuffix=core.windows.net";
//
//	public static String AccountName = "zhupao";
//	public static String AccountKey = "d0b0z0URvrbtxe1Jn4ozTD8qNQb2gLwhbdXw0Bk4oIxeTOAlguwKOE3sYyVgU6hPpfWh2nWy0AYIV7W8RWbqFw==";

	public static boolean isImportedContainerAvailable (){

		CloudStorageAccount storageAccount;
		CloudBlobClient blobClient = null;
		CloudBlobContainer container = null;
		boolean result =false;
		try {
			// Parse the connection string and create a blob client to interact with Blob storage
			storageAccount = CloudStorageAccount.parse(PropertyUtil.getProperty("storageConnectionString"));
			blobClient = storageAccount.createCloudBlobClient();
			container = blobClient.getContainerReference(PropertyUtil.getProperty("flatFilesContainer"));
			result=container.exists();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {

		}
		return result;
	}
	public static boolean isImportedFlatFilesAvailable (String blobname){

		CloudStorageAccount storageAccount;
		CloudBlobClient blobClient = null;
		CloudBlobContainer container = null;
		boolean result =false;
		try (InputStream input = AzureApp.class.getClassLoader().getResourceAsStream("config.properties")) {{
			if (input == null)
				return false;
				Properties prop = new Properties();
			prop.load(input);
			// Parse the connection string and create a blob client to interact with Blob storage
			storageAccount = CloudStorageAccount.parse(PropertyUtil.getProperty("storageConnectionString"));
			blobClient = storageAccount.createCloudBlobClient();
			container = blobClient.getContainerReference(prop.getProperty("flatFilesContainer"));
			//result = container.getBlockBlobReference("ANBOND.txt").exists() || container.getBlockBlobReference("ANPAYSND.txt").exists() ||container.getBlockBlobReference("ANUNIT.txt").exists();
			result = container.getBlockBlobReference(blobname).exists();
		}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {

		}
		return result;
	}
	public static String uploadToAzureBlobStorage(Path path, String filename) {
		File sourceFile, cloudFile;
		sourceFile = new File(path + filename + ".pdf");
		cloudFile = new File (path + filename +  new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf");
		String URL = null;
		CloudStorageAccount storageAccount;
		CloudBlobClient blobClient = null;
		CloudBlobContainer container = null;

		try {
			// Parse the connection string and create a blob client to interact with Blob storage
			storageAccount = CloudStorageAccount.parse(PropertyUtil.getProperty("storageConnectionString"));
			blobClient = storageAccount.createCloudBlobClient();
			container = blobClient.getContainerReference("vaprinting");

			// Create the container if it does not exist with public access.
			System.out.println("Creating container: " + container.getName());
			container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(),
					new OperationContext());

			//Getting a blob reference
			CloudBlockBlob blob = container.getBlockBlobReference(cloudFile.getName());

			//Creating blob and uploading file to it
			System.out.println("Uploading the pdf file ");
			blob.uploadFromFile(sourceFile.getAbsolutePath());

			URL =  GetFileSAS(PropertyUtil.getProperty("accountName"), PropertyUtil.getProperty("accountKey"),container.getUri() + "/"+cloudFile.getName());

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			return URL;
		} finally {

		}
		return URL;
	}

	public static void copyBlobToArchive(String source, String destination) {
		try {

		CloudStorageAccount storageAccount = CloudStorageAccount.parse(PropertyUtil.getProperty("storageConnectionString"));

		CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

		CloudBlobContainer sourceContainer = blobClient.getContainerReference(source);
		CloudBlobContainer targetContainer = blobClient.getContainerReference(destination);
		for (ListBlobItem blobItem : sourceContainer.listBlobs()) {
		    String sourceFileName = new File(blobItem.getUri().toString()).getName();
		    String sourceBaseFileName = FilenameUtils.getBaseName(sourceFileName);
		    String sourceExtensionFileName =  FilenameUtils.getExtension(sourceFileName);
		    String targetFileName = sourceBaseFileName + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "." + sourceExtensionFileName;
			CloudBlockBlob sourceBlob = sourceContainer.getBlockBlobReference(sourceFileName);
			CloudBlockBlob targetBlob = targetContainer.getBlockBlobReference(targetFileName);
			targetBlob.startCopy(sourceBlob);
			sourceBlob.deleteIfExists();
		}

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {

		}

	}
	public static String GetFileSAS(String accountName, String key, String resourceUrl){

        String start= DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
         Instant s =Instant.now().plus(720, ChronoUnit.HOURS);
        String expiry= DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
                 .withZone(ZoneOffset.UTC)
                 .format(s);

	    String azureApiVersion = "2019-02-02";

	    String stringToSign = accountName + "\n" +
	                "r\n" +
	                "b\n" +
	                "o\n" +
	                start + "\n" +
	                expiry + "\n" +
	                "\n" +
	                "https\n" +
	                azureApiVersion+"\n";

	    String signature = getHMAC256(key, stringToSign);
	    String sasToken = "";

	    try{

	        sasToken = "sv=" + azureApiVersion +
	            "&ss=b" +
	            "&srt=o" +
	            "&sp=r" +
	            "&se=" +expiry +
	            "&st=" + start +
	            "&spr=https" +
	            "&sig=" + URLEncoder.encode(signature, "UTF-8");


	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    }
	    if (resourceUrl=="")
	    	return sasToken;
		return resourceUrl+"?"+sasToken;
	}

	private static String getHMAC256(String accountKey, String signStr) {
	    String signature = null;
	    try {
	        SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(accountKey), "HmacSHA256");
	        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
	        sha256HMAC.init(secretKey);
	        signature = Base64.getEncoder().encodeToString(sha256HMAC.doFinal(signStr.getBytes("UTF-8")));
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return signature;
	}
}
