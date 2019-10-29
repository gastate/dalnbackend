package org.dalnservice.controller;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.dalnservice.classes.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Path("/")
public class DALNService {

    static final Logger logger = Logger.getLogger(DALNService.class);

    private DALNDatabaseClient databaseClient;
    private DALNS3Client s3Client;
    private DALNSproutVideoClient sproutVideoClient;
    private DALNSoundCloudClient soundCloudClient;
    private DALNSESClient sesClient;
    private DALNSSHClient sshClient;

    public DALNService() throws IOException {

        // The constructors for the following classes authenticate their respective
        // services
        databaseClient = new DALNDatabaseClient();
        s3Client = DALNS3Client.getInstance();
        sproutVideoClient = new DALNSproutVideoClient();
        soundCloudClient = new DALNSoundCloudClient();
        sesClient = new DALNSESClient();
        sshClient = DALNSSHClient.getInstance();
    }

    /** /posts/ **/

    // To retrieve a single post
    @GET
    @Path("/posts/get/{postId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Post getPostWithPath(@PathParam("postId") String postId) throws IOException {
        // log.info("Getting post " + postId);
        Post post = databaseClient.getPost("DALN-Posts", postId);
        // databaseClient.destroy();

        return post;
    }

    @GET
    @Path("/posts/getdev/{postId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Post getPostDevWithPath(@PathParam("postId") String postId) throws IOException {
        // log.info("Getting post " + postId);
        Post post = databaseClient.getPost("DALN-Posts-Dev", postId);
        // databaseClient.destroy();

        return post;
    }

    // Retrieves all posts contained in the database
    @GET
    @Path("/posts/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> listAllPostsJSON() throws IOException {
        // log.info("Getting all posts");
        return databaseClient.getAllPosts();
    }

    // Retrieves a random set of posts limited by the number entered
    @GET
    @Path("/posts/random/{size}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> listSetOfPostsWithLimit(@PathParam("size") int size) throws IOException {
        // log.info("Getting all posts");
        return databaseClient.getRandomSet(size);
    }

    // Method to retrieve posts for pagination. Accepts page size and page number
    @GET
    @Path("/posts/size/{pageSize}/page/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Post> listPage(@PathParam("pageSize") int pageSize, @PathParam("page") int page) throws IOException {

        return databaseClient.getPageScan(pageSize, page);
    }

    // Returns search results
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
    public Hits paginatedSearch(@PathParam("query") String query, @PathParam("pageSize") long pageSize,
            @PathParam("start") long start) throws IOException, ParseException {

        logger.debug("Performing paginated simple search");
        return databaseClient.search(query, pageSize, start);
    }

    @GET
    @Path("/posts/search/{query}/{pageSize}/{start}/{field}/{order}")
    @Produces(MediaType.APPLICATION_JSON)
    public Hits paginatedSearch(@PathParam("query") String query, @PathParam("pageSize") long pageSize,
            @PathParam("start") long start, @PathParam("field") String fieldToSortBy, @PathParam("order") String order)
            throws IOException, ParseException {

        logger.debug("Performing paginated search with sorting");
        return databaseClient.search(query, pageSize, start, fieldToSortBy, order);
    }

    @GET
    @Path("/posts/search-engine-size")
    @Produces(MediaType.TEXT_PLAIN)
    public long getSearchEngineSize() throws ParseException {
        return databaseClient.getSearchEngineSize();
    }

    // To retrieve a single post using the HTML form
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
    @Produces(MediaType.TEXT_PLAIN)
    public String createPost(JSONObject input) {
        String tableName, title, email, license;
        try {
            tableName = input.get("tableName").toString();
            title = input.get("title").toString();
            email = input.get("email").toString();
            license = input.get("license").toString();
        } catch (NullPointerException e) {
            return "Values for tableName, title, email, and license are required";
        }
        String postId = "";
        try {
            postId = databaseClient.createPost(tableName, title, email, license);
        } catch (Exception e) {
            logger.error("create post error");
            e.printStackTrace();
        }

        try {
            databaseClient.updatePost(tableName, postId, input);
        } catch (Exception e) {
            logger.error("update post error");
            e.printStackTrace();
        }

        return postId;
    }

    // @POST
    // @Path("/posts/update")
    // @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePost(JSONObject input) {
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
        AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        String bucketName = "daln-file-staging-area";

        try {
            System.out.println("Generating pre-signed URL.");
            java.util.Date expiration = new java.util.Date();
            long milliSeconds = expiration.getTime();
            milliSeconds += 1000 * 60 * 60; // Add 1 hour.
            expiration.setTime(milliSeconds);

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName,
                    objectKey);
            generatePresignedUrlRequest.setMethod(HttpMethod.PUT);
            generatePresignedUrlRequest.setExpiration(expiration);

            URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);

            logger.debug("Pre-Signed URL = " + url.toString());
            return url.toString();
        } catch (AmazonServiceException exception) {
            logger.debug("Caught an AmazonServiceException, " + "which means your request made it "
                    + "to Amazon S3, but was rejected with an error response " + "for some reason.");
            logger.debug("Error Message: " + exception.getMessage());
            logger.debug("HTTP  Code: " + exception.getStatusCode());
            logger.debug("AWS Error Code:" + exception.getErrorCode());
            logger.debug("Error Type:    " + exception.getErrorType());
            logger.debug("Request ID:    " + exception.getRequestId());
        } catch (AmazonClientException ace) {
            logger.debug("Caught an AmazonClientException, " + "which means the client encountered "
                    + "an internal error while trying to communicate" + " with S3, "
                    + "such as not being able to access the network.");
            logger.debug("Error Message: " + ace.getMessage());
        }
        return "";

    }

    @GET
    @Path("/asset/read/{key}")
    @Produces(MediaType.TEXT_PLAIN)
    public String readAsset(@PathParam("key") String objectKey) throws IOException {
        AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        String bucketName = "daln-file-staging-area";

        try {
            // Stream an object from S3 to Tika to interpret a chunk of text
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, objectKey));
            InputStream objectData = object.getObjectContent();
            DocumentReader reader = new DocumentReader();
            String result = reader.parseFirstChunk(objectData);
            objectData.close();
            return result;
        } catch (AmazonServiceException exception) {
            logger.debug("Caught an AmazonServiceException, " + "which means your request made it "
                    + "to Amazon S3, but was rejected with an error response " + "for some reason.");
            logger.debug("Error Message: " + exception.getMessage());
            logger.debug("HTTP  Code: " + exception.getStatusCode());
            logger.debug("AWS Error Code:" + exception.getErrorCode());
            logger.debug("Error Type:    " + exception.getErrorType());
            logger.debug("Request ID:    " + exception.getRequestId());
        } catch (AmazonClientException ace) {
            logger.debug("Caught an AmazonClientException, " + "which means the client encountered "
                    + "an internal error while trying to communicate" + " with S3, "
                    + "such as not being able to access the network.");
            logger.debug("Error Message: " + ace.getMessage());
        } catch (SAXException se) {
            logger.debug("Caught a SAXException, " + "which means the client encountered "
                    + "an internal error while trying to read a document");
            logger.debug("Error Message: " + se.getMessage());

        } catch (TikaException te) {
            logger.debug("Caught a TikaException, " + "which means the client encountered "
                    + "an internal error while trying to read a document");
            logger.debug("Error Message: " + te.getMessage());
        }
        return "";
    }

    @POST
    @Path("/asset/s3uploader")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String s3Upload(JSONObject input) throws IOException {
        AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        String bucketName = "daln-file-staging-area";
        String objectKey = input.get("objectKey").toString();
        String contentType = input.get("contentType").toString();

        /*
         * if(contentType.contains("audio")) { new Thread(() -> { try {
         * 
         * } catch (IOException e) { e.printStackTrace(); } }).start(); }
         */

        try {
            System.out.println("Generating pre-signed URL.");
            java.util.Date expiration = new java.util.Date();
            long milliSeconds = expiration.getTime();
            milliSeconds += 1000 * 60 * 60; // Add 1 hour.
            expiration.setTime(milliSeconds);

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName,
                    objectKey);
            generatePresignedUrlRequest.setMethod(HttpMethod.PUT);
            generatePresignedUrlRequest.setExpiration(expiration);
            generatePresignedUrlRequest.setContentType(contentType);
            generatePresignedUrlRequest.addRequestParameter(Headers.S3_CANNED_ACL,
                    CannedAccessControlList.PublicRead.toString());

            URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);

            logger.debug("Pre-Signed URL = " + url.toString());
            return url.toString();
        } catch (AmazonServiceException exception) {
            logger.debug("Caught an AmazonServiceException,  " + "which means your request made it "
                    + "to Amazon S3, but was rejected with an error response " + "for some reason.");
            logger.debug("Error Message: " + exception.getMessage());
            logger.debug("HTTP  Code: " + exception.getStatusCode());
            logger.debug("AWS Error Code:" + exception.getErrorCode());
            logger.debug("Error Type:    " + exception.getErrorType());
            logger.debug("Request ID:    " + exception.getRequestId());
        } catch (AmazonClientException ace) {
            logger.debug("Caught an AmazonClientException, " + "which means the client encountered "
                    + "an internal error while trying to communicate" + " with S3, "
                    + "such as not being able to access the network.");
            logger.debug("Error Message: " + ace.getMessage());
        }
        return "Pre-signed URL not generated";

    }

    @POST
    @Path("/asset/test")
    @Consumes(MediaType.TEXT_PLAIN)
    public void test(String input) {
        logger.info(Response.status(200).entity(input).build());
    }

    @POST
    @Path("/asset/apiupload")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response assetUpload(JSONObject input) throws ParseException {
        AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

        // Store JSONinput into variables
        String stagingAreaBucketName = input.get("stagingAreaBucketName").toString();
        test(stagingAreaBucketName);
        String finalBucketName = input.get("finalBucketName").toString();
        String tableName = input.get("tableName").toString();
        String objectKey = input.get("key").toString();
        String postId = input.get("PostId").toString();
        String queueName = input.get("queueName").toString();

        GetQueueUrlRequest getQueueUrlRequest = new GetQueueUrlRequest(queueName);
        String queueURL = sqs.getQueueUrl(getQueueUrlRequest).getQueueUrl();

        String assetDescription = "None";
        try {
            assetDescription = input.get("assetDescription").toString();
        } catch (NullPointerException e) {
            assetDescription = "None";
        }

        // Begin upload process
        // try {
        // assetDetails is a hashmap that will contain all details about the asset being
        // uploaded
        HashMap<String, String> assetDetails = new HashMap<>();
        assetDetails.put("bucketName", finalBucketName);
        assetDetails.put("PostId", postId);

        // Load the post that the asset belongs to
        Post post = databaseClient.getPost(tableName, postId);
        String originalPostTitle = post.getTitle();
        assetDetails.put("postTitle", originalPostTitle);

        // Compile metadata of the file being uploaded
        String assetName = objectKey;
        // String assetNameNoExtension = assetName.substring(0,
        // assetName.lastIndexOf('.'));
        // String assetExtension =
        // assetName.substring(assetName.lastIndexOf('.')).toLowerCase();

        String assetType = checkFiletype(assetName);
        String assetId = UUID.randomUUID().toString();

        assetDetails.put("assetId", assetId);
        assetDetails.put("assetName", assetName);
        if (assetDescription.trim().equals(""))
            assetDetails.put("assetDescription", "None");
        else
            assetDetails.put("assetDescription", assetDescription);
        assetDetails.put("assetType", assetType);

        databaseClient.uploadAsset(tableName, postId, assetDetails);

        JSONObject assetDetailsJSON = new JSONObject(assetDetails);
        assetDetailsJSON.put("postId", postId);
        assetDetailsJSON.put("tableName", tableName);
        assetDetailsJSON.put("bucketName", finalBucketName);

        SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(queueURL)
                .withMessageBody(assetDetailsJSON.toJSONString());

        sqs.sendMessage(sendMessageRequest);
        /*
         * //Download object from staging area to temporary file
         * logger.info("Using TransferManager to download object from S3");
         * databaseClient.updateAssetStatus(tableName, postId, assetId, "Downloading");
         * 
         * // Initialize TransferManager. TransferManager tx = new TransferManager();
         * if(assetNameNoExtension.length() < 3) assetNameNoExtension += "___"; File
         * tempFile = File.createTempFile(assetNameNoExtension, assetExtension); //
         * Download the Amazon S3 object to a file. Download myDownload =
         * tx.download(stagingAreaBucketName, objectKey, tempFile);
         * 
         * // Blocking call to wait until the download finishes.
         * myDownload.waitForCompletion(); // If transfer manager will not be used
         * anymore, shut it down. tx.shutdownNow();
         * 
         * logger.info("Object has been downloaded");
         * 
         * 
         * 
         * //Move object from staging area to post folder for archival
         * databaseClient.updateAssetStatus(tableName, postId, assetId, "Transferring");
         * 
         * String fullObjectKey = "Posts/"+postId+"/"+objectKey;
         * s3Client.createFolder(postId, finalBucketName); CopyObjectRequest
         * copyObjectRequest = new CopyObjectRequest( stagingAreaBucketName, objectKey,
         * finalBucketName, fullObjectKey
         * ).withCannedAccessControlList(CannedAccessControlList.PublicRead);
         * s3.copyObject(copyObjectRequest);
         * 
         * //delete object from staging area s3.deleteObject(stagingAreaBucketName,
         * objectKey);
         * 
         * assetDetails.put("assetLocation", s3Client.getS3FileLocation(finalBucketName,
         * fullObjectKey)); assetDetails.put("assetEmbedLink",
         * s3Client.getS3FileLocation(finalBucketName, fullObjectKey));
         * assetDetails.put("assetS3Link", s3Client.getS3FileLocation(finalBucketName,
         * fullObjectKey)); logger.info("File: " + objectKey + "  uploaded to s3");
         * databaseClient.uploadAsset(tableName, postId, assetDetails);
         * 
         * //ASSET STATUS 2 - AFTER S3 DOWNLOAD
         * 
         * 
         * //Split into separate endpoint from here to below String assetLocation =
         * null; String assetEmbedLink = null;
         * 
         * databaseClient.updateAssetStatus(tableName, postId, assetId, "Uploading");
         * switch (assetType) { case "Audio/Video": logger.info("File: " + objectKey +
         * " will be uploaded to SproutVideo"); try {
         * sproutVideoClient.initializeAndUpload(assetDetails, tempFile); assetLocation
         * = sproutVideoClient.getSpoutVideoLocation(assetId)[0]; assetEmbedLink =
         * sproutVideoClient.getSpoutVideoLocation(assetId)[1];
         * 
         * assetDetails.put("assetLocation", assetLocation);
         * assetDetails.put("assetEmbedLink", assetEmbedLink);
         * databaseClient.uploadAsset(tableName, postId, assetDetails);
         * databaseClient.updateAssetStatus(tableName, postId, assetId, "Completed");
         * break; } catch(NullPointerException e) {
         * logger.error("SproutVideo video location not found"); } catch(Exception e) {
         * logger.error("SproutVideo Upload Error"); e.printStackTrace(); }
         * 
         * 
         * case "Audio": logger.debug("File: " + objectKey +
         * " will be uploaded to SoundCloud"); try {
         * soundCloudClient.initializeAndUpload(assetDetails, tempFile); assetLocation =
         * soundCloudClient.getSoundLocation()[0]; assetEmbedLink =
         * soundCloudClient.getSoundLocation()[1];
         * 
         * assetDetails.put("assetLocation", assetLocation);
         * assetDetails.put("assetEmbedLink", assetEmbedLink);
         * databaseClient.uploadAsset(tableName, postId, assetDetails);
         * databaseClient.updateAssetStatus(tableName, postId, assetId, "Completed");
         * break; } catch(NullPointerException e) {
         * logger.error("SoundCloud location not found"); } catch(Exception e) {
         * logger.error("SoundCloud Upload Error"); e.printStackTrace(); } default:
         * databaseClient.updateAssetStatus(tableName, postId, assetId, "Completed");
         * break;
         * 
         * } } catch (AmazonServiceException ase) {
         * //databaseClient.uploadAsset(tableName, postId, assetDetails);
         * 
         * System.out.println("Caught an AmazonServiceException, which" +
         * " means your request made it " +
         * "to Amazon S3, but was rejected with an error response" +
         * " for some reason."); System.out.println("Error Message:    " +
         * ase.getMessage()); System.out.println("HTTP Status Code: " +
         * ase.getStatusCode()); System.out.println("AWS Error Code:   " +
         * ase.getErrorCode()); System.out.println("Error Type:       " +
         * ase.getErrorType()); System.out.println("Request ID:       " +
         * ase.getRequestId()); return
         * Response.status(404).entity("File Not Found").build(); } catch
         * (AmazonClientException ace) {
         * System.out.println("Caught an AmazonClientException, which means"+
         * " the client encountered " + "an internal error while trying to " +
         * "communicate with S3, " + "such as not being able to access the network.");
         * System.out.println("Error Message: " + ace.getMessage()); } catch
         * (IOException | InterruptedException e) { e.printStackTrace(); }
         */
        return Response.status(200).entity("File added to queue").build();
    }

    /** /admin/ **/

    @POST
    @Path("/admin/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response approvePost(JSONObject input) throws IOException, ParseException {
        boolean status = databaseClient.enterPostIntoCloudSearch(input.get("postId").toString(),
                input.get("tableName").toString());

        if (status)
            return Response.status(200).entity("Post added to search engine").build();
        else
            return Response.status(200).entity("Post was not successfully added to search engine").build();
    }

    @POST
    @Path("/admin/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePostFromSearch(JSONObject input) throws IOException, ParseException {
        databaseClient.removePostFromCloudSearch(input.get("postId").toString(), input.get("tableName").toString());
        return Response.status(200).entity("Post removed from search engine").build();
    }

    @POST
    @Path("/admin/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePostAndAssets(JSONObject input) throws IOException {

        soundCloudClient = new DALNSoundCloudClient();

        String postId = input.get("PostId").toString();
        String bucketName = input.get("bucketName").toString();
        String tableName = input.get("tableName").toString();

        // Change the table to either the prod or dev table
        databaseClient.updateTableName(tableName);

        // Retrieve the post to delete
        logger.info("Deleting post " + postId);
        Post post = databaseClient.getPost(tableName, postId);
        if (post == null)
            return Response.status(200).entity("Post not found").build();

        // All assets must be deleted, even if they are stored in S3, SpoutVideo, or
        // SoundCloud
        // Iterate through the assetList and delete the asset according to where it's
        // stored
        List<HashMap<String, String>> assetList = post.getAssetList();
        if (assetList == null) {
            databaseClient.deletePost(tableName, postId);
            return Response.status(200).entity("This post did not have any assets. Post was removed from the database.")
                    .build();
        }
        boolean[] deleteCheck = new boolean[assetList.size()];
        int i = 0;
        for (HashMap<String, String> asset : assetList) {
            String assetId = asset.get("assetId");
            String assetName = asset.get("assetName");
            String assetEmbedLink = asset.get("assetEmbedLink");

            System.out.println("Asset Name: " + assetName + " Asset Id:" + assetId);
            String objectKey = "Posts/" + postId + "/" + assetName;
            if (assetEmbedLink.contains("s3.amazonaws.com")) {
                // objectKey is location in s3
                logger.info("Deleting s3 object");
                deleteCheck[i] = s3Client.deleteObject(bucketName, objectKey);
            } else if (assetEmbedLink.contains("videos.sproutvideo.com")) {
                logger.info("Deleting video from SproutVideo");
                deleteCheck[i] = s3Client.deleteObject(bucketName, objectKey);
                deleteCheck[i] = sproutVideoClient.deleteVideo(assetId);

            } else if (assetEmbedLink.contains("api.soundcloud.com")) {
                logger.info("Deleting audio from SoundCloud");
                deleteCheck[i] = s3Client.deleteObject(bucketName, objectKey);
                String trackId = assetEmbedLink.substring(assetEmbedLink.lastIndexOf('/') + 1);
                deleteCheck[i] = soundCloudClient.deleteSound(trackId);
            }

            i++;
        }
        // Delete now empty s3 folder if it exists
        s3Client.deleteObject(bucketName, "Posts/" + postId + "/");

        // Check if all files were deleted, then delete database entry
        for (boolean flag : deleteCheck)
            if (!flag) // if at least one file didn't get deleted, don't delete database entry
                return Response.status(200)
                        .entity("Not all assets were deleted from the post, so the database entry will remain").build();

        databaseClient.deletePost(tableName, postId);

        return Response.status(200).entity("Post removed from the database and its assets deleted").build();
    }

    @POST
    @Path("/admin/unapproved")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Post> getUnnapprovedPosts(JSONObject input) {
        return databaseClient.getUnapprovedPosts(input.get("tableName").toString());
    }

    // submitted, approved, rejected, awaiting
    @POST
    @Path("/admin/email")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendEmail(JSONObject input) {
        String tableName = input.get("tableName").toString();
        String postId = input.get("postId").toString();
        Post post = databaseClient.getPost(tableName, postId);

        String toEmail = input.get("email").toString();
        String emailType = input.get("emailType").toString();
        String rejectionReason = "None";
        if (input.get("reason").toString() != null)
            rejectionReason = input.get("reason").toString();

        switch (emailType) {
        case "submitted":
            sesClient.emailPostSubmitted(toEmail, post.getTitle());
            break;
        case "awaiting":
            sesClient.emailAdminForApproval(toEmail, post.getTitle(), postId);
            break;
        case "approved":
            sesClient.emailPostApproved(toEmail, post.getTitle(), postId);
            break;
        case "rejected":
            sesClient.emailPostRejected(toEmail, post.getTitle(), rejectionReason);
            break;
        default:
            return Response.ok("Email not sent. Email type must be either: submitted, awaiting, approved, rejected")
                    .build();
        }
        return Response.status(200).entity("Email sent").build();
    }

    // runs the specified command in EC2 worker. Returns last 100 lines of output
    @GET
    @Path("/admin/executeec2/{stage}/{command}")
    @Produces(MediaType.TEXT_PLAIN)
    public String runEC2Command(@PathParam("stage") String stage, @PathParam("command") String command)
            throws IOException {
        logger.debug("Start restart " + stage);
        return sshClient.runCommand(stage, command);
    }

    // To retrieve a single post
    @GET
    @Path("/posts/dalnold/{dalnId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPostWithDALNId(@PathParam("dalnId") String dalnId) throws IOException {
        return databaseClient.getPostIDFromTableUsingDALNId(dalnId);
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
            case ".mkv":
                return "Audio/Video";
            case ".htm":
            case ".html":
                return "Web";
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("This file does not have a file type. It cannot be uploaded.");
            System.exit(1);

        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase() + " File";
    }

}
