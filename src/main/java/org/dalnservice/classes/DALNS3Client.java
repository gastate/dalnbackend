package org.dalnservice.classes;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by Shakib on 7/16/2016.
 *
 * This class handles the task of uploading all files that aren't videos or
 * audios to S3 and retrieving its download location. The constructor extracts
 * all needed values from the HashMap and places them into variables. The values
 * will be used as inputs for the upload. This class only handles single file
 * uploads that are contained in the original post. It does not create the post
 * folder in S3 or upload its metadata.
 */
public class DALNS3Client {
    private AmazonS3Client s3Client;
    private String postID, fileName, assetID, bucketName;
    private File file;
    private static DALNS3Client instance;

    private DALNS3Client() throws IOException {
        s3Client = new AmazonS3Client();
        s3Client.setRegion(Region.US_Standard.toAWSRegion());
    }

    public static DALNS3Client getInstance() throws IOException {
        if (instance == null) {
            instance = new DALNS3Client();
        }
        return instance;
    }

    public void initializeAndUpload(HashMap<String, String> assetDetails, File file) {
        postID = assetDetails.get("PostId");
        assetID = assetDetails.get("assetId");
        fileName = assetDetails.get("assetName");
        bucketName = assetDetails.get("bucketName");
        this.file = file;

        uploadFile();
    }

    public InputStream downloadFile(String bucketName, String key) {
        S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, key));

        InputStream objectData = s3object.getObjectContent();
        return objectData;
    }

    private void uploadFile() {
        try {

            createFolder(postID, bucketName);
            // Specifying the upload location of our in S3 and set it to public read
            s3Client.putObject(new PutObjectRequest(bucketName, "Posts/" + postID + "/" + fileName, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead));

        } catch (AmazonServiceException ase) {
            printASE(ase);
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " + "means the client encountered "
                    + "an internal error while trying to " + "communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    public void createFolder(String postID, String bucketName) {
        // data for folder
        ObjectMetadata folderMetadata = new ObjectMetadata();
        folderMetadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        // PutObjectRequest used for creating an object to be uploaded
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, "Posts/" + postID + "/", emptyContent,
                folderMetadata);
        // send request to S3 to create folder
        s3Client.putObject(putObjectRequest);
    }

    public String getS3FileLocation(String bucketName, String objectKey) {
        // String location = s3Client.getResourceUrl("daln",
        // "daln/Posts/"+postID+"/"+fileName);
        // location = location.replace("https://daln.s3.", "https://s3-us-west-1.");
        return s3Client.getResourceUrl(bucketName, objectKey);

        // return "https://s3-us-west-1.amazonaws.com/daln/Posts/" + dalnId + "/" +
        // fileName;
    }

    public boolean deleteObject(String bucketName, String key) {
        try {
            s3Client.deleteObject(bucketName, key);
        } catch (AmazonServiceException ase) {
            printASE(ase);
            return false;
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " + "means the client encountered "
                    + "an internal error while trying to " + "communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
            return false;
        }
        return true;
    }

    private void printASE(AmazonServiceException ase) {
        System.out.println();
        System.out.println("Caught an AmazonServiceException, which " + "means your request made it "
                + "to Amazon S3, but was rejected with an error response" + " for some reason.");
        System.out.println("Error Message:    " + ase.getMessage());
        System.out.println("HTTP Status Code: " + ase.getStatusCode());
        System.out.println("AWS Error Code:   " + ase.getErrorCode());
        System.out.println("Error Type:       " + ase.getErrorType());
        System.out.println("Request ID:       " + ase.getRequestId());
    }
}
