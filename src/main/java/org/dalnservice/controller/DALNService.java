package org.dalnservice.controller;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.apache.log4j.Logger;
import org.dalnservice.classes.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.validation.constraints.Null;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Path("/")
public class DALNService {

    static final Logger logger = Logger.getLogger(DALNService.class);

    private DALNDatabaseClient databaseClient;
    private DALNS3Client s3Client;
    private DALNSproutVideoClient sproutVideoClient;
    private DALNSoundCloudClient soundCloudClient;
    private DALNCloudSearchClient cloudSearchClient;
    private DALNSESClient sesClient;

    public DALNService() throws IOException {
        //The constructors for the following classes authenticate their respective services
        databaseClient = new DALNDatabaseClient();
        s3Client = new DALNS3Client();
        sproutVideoClient = new DALNSproutVideoClient();
        soundCloudClient = new DALNSoundCloudClient();
        cloudSearchClient = new DALNCloudSearchClient();
        sesClient = new DALNSESClient();
    }


   /** /posts/ **/

    //To retrieve a single post
    @GET
    @Path("/posts/get/{postId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Post getPostWithPath(@PathParam("postId") String postId) throws IOException {
        //log.info("Getting post " + postId);
        Post post = databaseClient.getPost("DALN-Posts", postId);
        //databaseClient.destroy();

        return post;
    }

    @GET
    @Path("/posts/getdev/{postId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Post getPostDevWithPath(@PathParam("postId") String postId) throws IOException {
        //log.info("Getting post " + postId);
        Post post = databaseClient.getPost("DALN-Posts-Dev", postId);
        //databaseClient.destroy();

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
    public Hits search(@PathParam("query") String query) throws IOException, ParseException {

        logger.debug("Performing search");
        return databaseClient.search(query);
    }

    @GET
    @Path("/posts/search/{query}/{pageSize}/{start}")
    @Produces(MediaType.APPLICATION_JSON)
    public Hits paginatedSearch(@PathParam("query") String query, @PathParam("pageSize") long pageSize, @PathParam("start") long start)
            throws IOException, ParseException {

        logger.debug("Performing paginated simple search");
        return databaseClient.search(query, pageSize, start);
    }

    @GET
    @Path("/posts/search/{query}/{pageSize}/{start}/{field}/{order}")
    @Produces(MediaType.APPLICATION_JSON)
    public Hits paginatedSearch(@PathParam("query") String query, @PathParam("pageSize") long pageSize, @PathParam("start") long start,
                                      @PathParam("field") String fieldToSortBy, @PathParam("order") String order )
            throws IOException, ParseException  {

        logger.debug("Performing paginated search with sorting");
        return databaseClient.search(query, pageSize, start, fieldToSortBy, order);
    }

    @GET
    @Path("/posts/search-engine-size")
    @Produces(MediaType.TEXT_PLAIN)
    public long getSearchEngineSize() throws ParseException {
        return databaseClient.getSearchEngineSize();
    }


    //To retrieve a single post using the HTML form
    @POST
    @Path("/posts/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Post getPostWithForm(@FormParam("PostId") String postId) throws IOException {

        Post post = databaseClient.getPost("DALN-Posts", postId);

        return post;
    }

    @POST
    @Path("/posts/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPost(JSONObject input)
    {
        String tableName, title, email, license;
        try {
            tableName = input.get("tableName").toString();
            title = input.get("title").toString();
            email = input.get("email").toString();
            license = input.get("license").toString();
        }
        catch(NullPointerException e)
        {
            return Response.status(422).entity("Values for tableName, title, email, and license are required").build();
        }

        String postId = databaseClient.createPost(tableName, title, email, license);
        databaseClient.updatePost(tableName, postId, input);
        return Response.status(201).entity(postId).build();
    }

    @POST
    @Path("/posts/update")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePost(JSONObject input)
    {
        String tableName = input.get("tableName").toString();
        String postId = input.get("PostId").toString();

        databaseClient.updatePost(tableName, postId, input);

        return Response.status(201).entity("Post updated").build();
    }


    /** /asset/ **/

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

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey);
            generatePresignedUrlRequest.setMethod(HttpMethod.PUT);
            generatePresignedUrlRequest.setExpiration(expiration);

            URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);

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



    @POST
    @Path("/asset/apiupload")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response assetUpload(JSONObject input)
    {
        EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
        AWSCredentials awsCredentials = creds.getCredentials();
        AmazonS3 s3 = new AmazonS3Client(awsCredentials);
        String stagingAreabucketName = input.get("stagingAreaBucketName").toString();
        String finalBucketName = input.get("finalBucketName").toString();
        String tableName = input.get("tableName").toString();
        String objectKey = input.get("key").toString();
        String postId = input.get("PostId").toString();
        String assetDescription = "None";
        if (input.get("assetDescription").toString()!= null)
            assetDescription = input.get("assetDescription").toString();


        try {
            System.out.println("Downloading an object");

            String assetName = objectKey;
            String assetNameNoExtension = assetName.substring(0, assetName.lastIndexOf('.'));
            String assetExtension = assetName.substring(assetName.lastIndexOf('.')).toLowerCase();
            String assetType = checkFiletype(assetName);


            // Initialize TransferManager.
            TransferManager tx = new TransferManager();
            File tempFile = File.createTempFile(assetNameNoExtension, assetExtension);

            // Download the Amazon S3 object to a file.
            Download myDownload = tx.download(stagingAreabucketName, objectKey, tempFile);

            // Blocking call to wait until the download finishes.
            myDownload.waitForCompletion();

            // If transfer manager will not be used anymore, shut it down.
            tx.shutdownNow();


            /*
            //Another method to download files
            S3Object s3object = s3.getObject(new GetObjectRequest(stagingAreabucketName, objectKey));

            InputStream objectData = s3object.getObjectContent();
            File tempFile = File.createTempFile(assetNameNoExtension, assetExtension);

            tempFile.deleteOnExit();


            byte[] bytes = IOUtils.toByteArray(objectData);

            ByteSink sink = com.google.common.io.Files.asByteSink(tempFile);
            sink.write(bytes);*/


            //Compile details of the file being uploaded
            HashMap<String, String> assetDetails = new HashMap<>();

            Post post = databaseClient.getPost(tableName,postId);
            String originalPostTitle = post.getTitle();

            String assetId;
            do
                assetId = UUID.randomUUID().toString();
            while(databaseClient.checkIfUUIDExists(assetId));
            assetDetails.put("bucketName", finalBucketName);
            assetDetails.put("PostId", postId);
            assetDetails.put("postTitle", originalPostTitle);
            assetDetails.put("assetId",assetId);
            assetDetails.put("assetName", assetName);
            if(assetDescription.trim().equals("")) assetDetails.put("assetDescription", "None");
            else assetDetails.put("assetDescription", assetDescription);
            System.out.println("asset type:" + assetType);
            assetDetails.put("assetType", assetType);

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


            //PostId, postTitle, and bucketName attributes aren't needed as only the asset's details will be added to the database.
            //The post id and post title already exist in the database post entry.
            assetDetails.remove("PostId");
            assetDetails.remove("postTitle");
            assetDetails.remove("bucketName");

            databaseClient.uploadAsset(tableName, postId, assetDetails);

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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //log.info("File uploaded");
        return Response.ok("File uploaded successfully!").build();
    }


    /** /admin/ **/

    @POST
    @Path("/admin/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response approvePost(JSONObject input) throws IOException, ParseException {
        databaseClient.enterPostIntoCloudSearch(input.get("postId").toString(), input.get("tableName").toString());
        return Response.ok("Post added to search engine").build();
    }

    @POST
    @Path("/admin/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePostFromSearch(JSONObject input) throws IOException, ParseException {
        databaseClient.removePostFromCloudSearch(input.get("postId").toString(), input.get("tableName").toString());
        return Response.ok("Post removed from search engine").build();
    }

    @POST
    @Path("/admin/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePostAndAssets(JSONObject input) throws IOException {
        String postId = input.get("PostId").toString();
        String bucketName = input.get("bucketName").toString();
        String tableName = input.get("tableName").toString();

        //Change the table to either the prod or dev table
        databaseClient.updateTableName(tableName);

        //Retrieve the post to delete
        logger.info("Deleting post " + postId);
        Post post = databaseClient.getPost(tableName, postId);
        if(post == null)
            return Response.ok("Post not found").build();

        //All assets must be deleted, even if they are stored in S3, SpoutVideo, or SoundCloud
        //Iterate through the assetList and delete the asset according to where it's stored
        List<HashMap<String,String>> assetList = post.getAssetList();
        if(assetList==null)
        {
            databaseClient.deletePost(tableName, postId);
            return Response.ok("This post did not have any assets. Post was removed from the database.").build();
        }
        boolean[] deleteCheck = new boolean[assetList.size()];
        int i = 0;
        for(HashMap<String, String> asset : assetList)
        {
            String assetId = asset.get("assetId");
            String assetName = asset.get("assetName");
            String assetEmbedLink = asset.get("assetEmbedLink");


            System.out.println("Asset Name: " + assetName + " Asset Id:" + assetId);

            if(assetEmbedLink.contains("s3.amazonaws.com"))
            {
                //objectKey is location in s3
                String objectKey = "posts/"+postId+"/"+assetName;
                logger.info("Deleting s3 object");
                deleteCheck[i] = s3Client.deleteObject(bucketName, objectKey);
            }
            else if(assetEmbedLink.contains("videos.sproutvideo.com"))
            {
                logger.info("Deleting video from SproutVideo");
                deleteCheck[i] = sproutVideoClient.deleteVideo(assetId);

            }
            else if(assetEmbedLink.contains("api.soundcloud.com"))
            {
                logger.info("Deleting audio from SoundCloud");
                String trackId = assetEmbedLink.substring(assetEmbedLink.lastIndexOf('/') + 1);
                deleteCheck[i] = soundCloudClient.deleteSound(trackId);
            }

            i++;
        }
        //Delete now empty s3 folder if it exists
        s3Client.deleteObject(bucketName, "posts/" + postId + "/");

        //Check if all files were deleted, then delete database entry
        for(boolean flag : deleteCheck)
            if(!flag) //if at least one file didn't get deleted, don't delete database entry
                return Response.ok("Not all assets were deleted from the post, so the database entry will remain").build();

        databaseClient.deletePost(tableName, postId);

        return Response.ok("Post removed from the database and its assets deleted").build();
    }

    @POST
    @Path("/admin/unapproved")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Post> getUnnapprovedPosts(JSONObject input)
    {
        return databaseClient.getUnapprovedPosts(input.get("tableName").toString());
    }

    //submitted, approved, rejected, awaiting
    @POST
    @Path("/admin/email")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendEmail(JSONObject input)
    {
        String tableName = input.get("tableName").toString();
        String postId = input.get("postId").toString();
        Post post = databaseClient.getPost(tableName, postId);

        String toEmail = input.get("email").toString();
        String emailType = input.get("emailType").toString();
        String rejectionReason = "None";
        if (input.get("reason").toString()!= null)
            rejectionReason = input.get("reason").toString();

        switch(emailType)
        {
            case "submitted": sesClient.emailPostSubmitted(toEmail, post.getTitle()); break;
            case "awaiting": sesClient.emailAdminForApproval(toEmail, post.getTitle(), postId); break;
            case "approved": sesClient.emailPostApproved(toEmail, post.getTitle(), postId); break;
            case "rejected": sesClient.emailPostRejected(toEmail, post.getTitle(), rejectionReason); break;
            default:
                return Response.ok("Email not sent. Email type must be either: submitted, awaiting, approved, rejected").build();
        }
        return Response.ok("Email sent").build();
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


}
