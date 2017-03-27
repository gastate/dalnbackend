package org.dalnservice.classes;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by Shakib on 7/16/2016.
 *
 * This class handles the task of uploading all files that aren't videos or audios to S3 and retrieving
 * its download location. The constructor extracts all needed values from the HashMap and places them into variables.
 * The values will be used as inputs for the upload. This class only handles single file uploads that are contained in
 * the original post. It does not create the post folder in S3 or upload its metadata.
 */
public class UploadToS3
{
    private AmazonS3Client s3Client;
    private HashMap<String, String> assetDetails;
    private String postID, fileName, assetID;
    private File file;

    public UploadToS3() throws IOException {
        EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
        AWSCredentials awsCredentials = creds.getCredentials();

        s3Client = new AmazonS3Client(awsCredentials);
    }

    public void initializeAndUpload(HashMap<String, String> assetDetails, File file)
    {
        this.assetDetails = assetDetails;
        postID = assetDetails.get("PostId");
        assetID = assetDetails.get("assetID");
        fileName = assetDetails.get("assetName");
        this.file = file;

        uploadFile();
    }

    public void createFolder(String postID)
    {
        //data for folder
        ObjectMetadata folderMetadata = new ObjectMetadata();
        folderMetadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        //PutObjectRequest used for creating an object to be uploaded
        PutObjectRequest putObjectRequest = new PutObjectRequest("daln",
                "Posts/" + postID + "/", emptyContent, folderMetadata);
        // send request to S3 to create folder
        s3Client.putObject(putObjectRequest);
    }

    public void uploadFile() {
        try {

            createFolder(postID);
            //Specifying the upload location of our in S3 and set it to public read
            s3Client.putObject(new PutObjectRequest("daln", "Posts/" + postID + "/" + fileName, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead));


        } catch (AmazonClientException ace)
        {
        }
    }
        /*
            System.out.println();
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());*/

    public String getS3FileLocation()
    {
        String location = s3Client.getResourceUrl("daln", "daln/Posts/"+postID+"/"+fileName);
        location = location.replace("https://daln.s3.", "https://s3-us-west-1.");
        return location;
       // return "https://s3-us-west-1.amazonaws.com/daln/Posts/" + dalnId + "/" + fileName;
    }
}
