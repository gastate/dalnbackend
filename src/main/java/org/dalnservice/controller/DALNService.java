package org.dalnservice.controller;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor;
import com.sun.corba.se.spi.orbutil.fsm.Input;
import de.voidplus.soundcloud.SoundCloud;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.log4j.Logger;
import org.dalnservice.classes.*;
import org.glassfish.jersey.media.multipart.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import sun.nio.ch.IOUtil;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Path("/")
public class DALNService {

    static final Logger logger = Logger.getLogger(DALNService.class);

    static class Entity {
        public int id = 1;
        public String name;

        public Entity(String name) {
            this.name = name;
        }
    }


    private DALNDatabaseClient databaseClient;
    private UploadToS3 s3Client;
    private UploadToSproutVideo sproutVideoClient;
    private UploadToSoundCloud soundCloudClient;

    public DALNService() throws IOException {
        //The constructors for the following classes authenticate their respective services
        databaseClient = new DALNDatabaseClient();
        s3Client = new UploadToS3();
        sproutVideoClient = new UploadToSproutVideo();
        soundCloudClient = new UploadToSoundCloud();
    }

    @POST
    @Path("/posts/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPost(JSONObject input)
    {
        String title = (String) input.get("title");
        String postID = databaseClient.createPost(title);

        databaseClient.updatePost(postID, input);

        return Response.status(201)
                .entity(new Entity(postID))
                .build();
    }


    /*
    //The createPost method enters the post into the database with the specified title and returns a UUID
    //The UUID is used to create a folder in s3 for any documents
    @POST
    @Path("/posts/create")
    public Response createPost(@FormParam("title") String title) throws IOException {

        //log.info("Creating post...");
        String postID = databaseClient.createPost(title);
        s3Client.createFolder(postID);
        databaseClient.destroy();

        //log.info("Post " + postID + " created.");
        return Response.status(200)
                .entity("You've created a new post with the title: " + title + "." +
                        "\nThe post ID is " + postID + ". You will need this ID" +
                        " if you want to update the post later on.")
                .build();
    }*/

    //To retrieve a single post
    @GET
    @Path("/posts/get/{postId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Post getPostWithPath(@PathParam("postId") String postId) throws IOException {
        //log.info("Getting post " + postId);
        Post post = databaseClient.getPost(postId);
        databaseClient.destroy();

        return post;
    }

    //To retrieve a single post using the HTML form
    @POST
    @Path("/posts/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Post getPostWithForm(@FormParam("PostId") String postId) throws IOException {

        //log.info("Getting post " + postId);
        Post post = databaseClient.getPost(postId);
        databaseClient.destroy();

        return post;
    }

    //Retrieves all posts contained in the database
    @GET
    @Path("/posts/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> listAllPostsJSON() throws IOException {
        //log.info("Getting all posts");
        return databaseClient.getAllPosts();
    }

    //Retrieves posts from now until the number entered (number represents days). Created mainly to test scan capabilities
    @GET
    @Path("/posts/from/{daysAgo}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> listSetOfPostsFromDaysAgo(@PathParam("daysAgo") int daysAgo) throws IOException {
        //log.info("Getting all posts");
        return databaseClient.getPostsFromDaysAgoUntilNow(daysAgo);
    }

    //Retrieves a random set of posts limited by the number entered
    @GET
    @Path("/posts/random/{size}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> listSetOfPostsWithLimit(@PathParam("size") int size) throws IOException {
        //log.info("Getting all posts");
        return databaseClient.getRandomSet(size);
    }


    //Method to retrieve posts for pagination. Accepts page size and page number
    @GET
    @Path("/posts/size/{pageSize}/page/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> listPage(@PathParam("pageSize") int pageSize, @PathParam("page") int page) throws IOException {

        return databaseClient.getPageScan(pageSize, page);
    }

    //Returns search results
    @GET
    @Path("/posts/search/{query}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> search(@PathParam("query") String query) throws IOException, ParseException {

        logger.debug("Performing search");
        return databaseClient.search(query);
    }

    @GET
    @Path("/posts/search/{query}/{pageSize}/{start}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> paginatedSearch(@PathParam("query") String query, @PathParam("pageSize") long pageSize, @PathParam("start") long start)
            throws IOException, ParseException {

        logger.debug("Performing paginated simple search");
        return databaseClient.search(query, pageSize, start);
    }

    @GET
    @Path("/posts/search/{query}/{pageSize}/{start}/{field}/{order}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> paginatedSearch(@PathParam("query") String query, @PathParam("pageSize") long pageSize, @PathParam("start") long start,
                                      @PathParam("field") String fieldToSortBy, @PathParam("order") String order )
            throws IOException, ParseException  {

        logger.debug("Performing paginated search with sorting");
        return databaseClient.search(query, pageSize, start, fieldToSortBy, order);
    }



    @POST
    @Path("/asset/apiupload")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response assetUpload(JSONObject input)
    {
        EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
        AWSCredentials awsCredentials = creds.getCredentials();
        AmazonS3 s3 = new AmazonS3Client(awsCredentials);
        String bucketName = input.get("bucketName").toString();
        String objectKey = input.get("key").toString();
        String postId = input.get("PostId").toString();
        String assetDescription = input.get("assetDescription").toString();


        try {
            System.out.println("Downloading an object");
            S3Object s3object = s3.getObject(new GetObjectRequest(bucketName, objectKey));
            System.out.println("Content-Type: "  + s3object.getObjectMetadata().getContentType());

            InputStream objectData = s3object.getObjectContent();


            String assetName = objectKey;
            String assetNameNoExtension = assetName.substring(0, assetName.lastIndexOf('.'));
            String assetExtension = assetName.substring(assetName.lastIndexOf('.')).toLowerCase();
            String assetType = checkFiletype(assetName);

            //The file uploaded using the form will be transferred to tempFile

            File tempFile = File.createTempFile(assetNameNoExtension, assetExtension);
            OutputStream outStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            //read from is to buffer
            while((bytesRead = objectData.read(buffer)) !=-1){
                outStream.write(buffer, 0, bytesRead);
            }
            objectData.close();
            //flush OutputStream to write any buffered data to file
            outStream.flush();
            outStream.close();



            //Compile details of the file being uploaded
            HashMap<String, String> assetDetails = new HashMap<>();

            Post post = databaseClient.getPost(postId);
            String originalPostTitle = post.getTitle();

            String assetID;
            do
                assetID = UUID.randomUUID().toString();
            while(databaseClient.checkIfUUIDExists(assetID));

            assetDetails.put("PostId", postId);
            assetDetails.put("postTitle", originalPostTitle);
            assetDetails.put("assetID",assetID);
            assetDetails.put("assetName", assetName);
            if(assetDescription.trim().equals("")) assetDetails.put("assetDescription", "None");
            else assetDetails.put("assetDescription", assetDescription);
            assetDetails.put("assetType", assetType);

            System.out.println("asset type: " + assetType);
            switch (assetType) {
                case "Audio/Video":
                    sproutVideoClient.initializeAndUpload(assetDetails, tempFile);
                    assetDetails.put("assetLocation", sproutVideoClient.getSpoutVideoLocation()[0]);
                    assetDetails.put("assetEmbedLink", sproutVideoClient.getSpoutVideoLocation()[1]);
                    break;
                case "Audio":
                    soundCloudClient.initializeAndUpload(assetDetails, tempFile);
                    assetDetails.put("assetLocation", soundCloudClient.getSoundLocation()[0]);
                    assetDetails.put("assetEmbedLink", soundCloudClient.getSoundLocation()[1]);
                    break;
                default:
                    s3Client.initializeAndUpload(assetDetails, tempFile);
                    assetDetails.put("assetLocation", s3Client.getS3FileLocation());
                    assetDetails.put("assetEmbedLink", s3Client.getS3FileLocation());
                    break;
            }


            //PostId and postTitle attributes aren't needed as only the asset's details will be added to the database.
            //The post id and post title already exist in the database post entry.
            assetDetails.remove("PostId");
            assetDetails.remove("postTitle");

            databaseClient.uploadAsset(postId,assetDetails);

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which" +
                    " means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means"+
                    " the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //log.info("File uploaded");
        return Response.ok("File uploaded successfully!").build();
    }



    @GET
    @Path("/asset/s3upload/{key}")
    @Produces(MediaType.TEXT_PLAIN)
    public String s3Upload(@PathParam("key") String objectKey) throws IOException {
        EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
        AWSCredentials awsCredentials = creds.getCredentials();
        AmazonS3 s3 = new AmazonS3Client(awsCredentials);
        String bucketName = "daln-file-staging-area";

        try {
            System.out.println("Generating pre-signed URL.");
            java.util.Date expiration = new java.util.Date();
            long milliSeconds = expiration.getTime();
            milliSeconds += 1000 * 60 * 60; // Add 1 hour.
            expiration.setTime(milliSeconds);

            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, objectKey);
            generatePresignedUrlRequest.setMethod(HttpMethod.PUT);
            generatePresignedUrlRequest.setExpiration(expiration);

            URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);
            //UploadObject(url);

            logger.debug("Pre-Signed URL = " + url.toString());
            return url.toString();
        } catch (AmazonServiceException exception) {
            logger.debug("Caught an AmazonServiceException, " +
                    "which means your request made it " +
                    "to Amazon S3, but was rejected with an error response " +
                    "for some reason.");
            logger.debug("Error Message: " + exception.getMessage());
            logger.debug("HTTP  Code: "    + exception.getStatusCode());
            logger.debug("AWS Error Code:" + exception.getErrorCode());
            logger.debug("Error Type:    " + exception.getErrorType());
            logger.debug("Request ID:    " + exception.getRequestId());
        } catch (AmazonClientException ace) {
            logger.debug("Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            logger.debug("Error Message: " + ace.getMessage());
        }
        return "";

    }





    private String checkFiletype(String fileName) {
        try {
            switch (fileName.substring(fileName.lastIndexOf('.')).toLowerCase()) {
                case ".doc":
                case ".docx":
                case ".rtf":
                case ".txt":
                case ".pdf":
                case ".odt":
                    return "Text";
                case ".jpg":
                case ".jpeg":
                case ".gif":
                case ".png":
                case ".tiff":
                    return "Image";
                case ".mp3":
                case ".wav":
                case ".m4a":
                    return "Audio";
                case ".mp4":
                case ".mov":
                case ".avi":
                case ".wmv":
                case ".m4v":
                    return "Audio/Video";
                case ".htm":
                case ".html":
                    return "Web";
            }
        }catch(StringIndexOutOfBoundsException e)
        {
            System.out.println("This file does not have a file type. It cannot be uploaded.");
            System.exit(1);

        }
        return fileName.substring(fileName.lastIndexOf('.')+1).toUpperCase() + " File";
    }




    public static class NewEntityRequest {
        public String name;
    }

    /**
     * This controller uses automatically serialization of Request body to any POJO
     * @param requestEntity Request Entity
     * @return Response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/resource")
    public Response exampleSecondEndpointPost(NewEntityRequest requestEntity
    ) {

        logger.debug("Request got");
        return Response.status(201)
                .entity(new Entity(requestEntity.name))
                .build();
    }

}
