package org.dalnservice.classes;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClientBuilder;
import com.amazonaws.services.cloudsearchdomain.model.ContentType;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by Shakib on 4/17/2017.
 */
public class DALNCloudSearchClient {
    private DynamoDB dynamoDB;
    private AmazonCloudSearchDomain cloudSearchClient;

    public DALNCloudSearchClient() throws IOException {
        /** Authenticate clients **/
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        dynamoDB = new DynamoDB(client);

        cloudSearchClient = AmazonCloudSearchDomainClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(System.getenv("documentEndpoint"), "us-east-1"))
                .build();
    }

    /** Convert single post to SDF **/
    public JSONObject convertDynamoEntryToAddSDF(String postID, String tableName) throws ParseException, IOException {
        // Retrieve the post
        Table table = dynamoDB.getTable(tableName);
        Item post = table.getItem("PostId", postID);

        JSONParser parser = new JSONParser();
        JSONObject postAsJSON = (JSONObject) parser.parse(post.toJSON());
        JSONObject postAsSDF = new JSONObject();
        boolean failedAsset = false;

        postAsSDF.put("type", "add");
        postAsSDF.put("id", postID);
        JSONObject fields = new JSONObject();

        List assetList = post.getList("assetList");
        JSONArray assetNames = new JSONArray();
        JSONArray assetDescriptions = new JSONArray();
        JSONArray assetTypes = new JSONArray();
        JSONArray assetIDs = new JSONArray();
        JSONArray assetEmbedLinks = new JSONArray();
        JSONArray assetLocations = new JSONArray();

        try {
            for (int i = 0; i < assetList.size(); i++) {
                Map asset = (Map) assetList.get(i);
                assetNames.add(asset.get("assetName").toString());
                assetDescriptions.add(asset.get("assetDescription").toString());
                assetTypes.add(asset.get("assetType").toString());
                assetIDs.add(asset.get("assetId").toString());
                assetEmbedLinks.add(asset.get("assetEmbedLink").toString());
                assetLocations.add(asset.get("assetLocation").toString());
            }
        } catch (NullPointerException e) {
            failedAsset = true;
        }

        // if(post.getInt("areAllFilesUploaded") == null)
        fields.put("areallfilesuploaded", post.getInt("areAllFilesUploaded"));
        fields.put("assetdescription", assetDescriptions);
        fields.put("assetembedlink", assetEmbedLinks);
        fields.put("assetid", assetIDs);
        fields.put("assetlocation", assetLocations);
        fields.put("assetname", assetNames);
        fields.put("assettype", assetTypes);

        Iterator iterator = postAsJSON.keySet().iterator();
        while (iterator.hasNext()) {
            String attribute = (String) iterator.next();
            String sdfAttributeName = attribute.toLowerCase();

            if (attribute.equals("assetList") || attribute.equals("isPostNotApproved"))
                continue;

            Object value = post.get(attribute);
            if (value instanceof String) {
                fields.put(sdfAttributeName, value.toString());
            } else {
                fields.put(sdfAttributeName, value);
            }
        }

        postAsSDF.put("fields", fields);

        if (failedAsset)
            return null;
        else
            return postAsSDF;

    }

    public JSONObject convertDynamoEntryToDeleteSDF(String postID) throws ParseException, IOException {
        JSONObject postAsSDF = new JSONObject();

        postAsSDF.put("type", "delete");
        postAsSDF.put("id", postID);

        return postAsSDF;
    }

    public boolean uploadSingleDocument(JSONObject documentAsSDF) {
        JSONArray document = new JSONArray();
        document.add(documentAsSDF);
        byte[] bytes = document.toJSONString().getBytes();
        long contentLength = bytes.length;
        System.out.println("document length:" + bytes.length);

        InputStream inputStream = new ByteArrayInputStream(bytes);
        UploadDocumentsRequest uploadDocumentsRequest = new UploadDocumentsRequest();
        uploadDocumentsRequest.setDocuments(inputStream);
        uploadDocumentsRequest.setContentType(ContentType.Applicationjson);
        uploadDocumentsRequest.setContentLength(contentLength);
        UploadDocumentsResult uploadDocumentsResult = cloudSearchClient.uploadDocuments(uploadDocumentsRequest);
        System.out.println("Document upload status: " + uploadDocumentsResult.getStatus());
        if (uploadDocumentsResult.getStatus().equals("success"))
            return true;
        else
            return false;
    }

    /*
     * public void uploadDocumentBatch() throws IOException, ParseException {
     * 
     * int startingPostNumber = 6392; List<Post> allPosts = mapper.scan(Post.class,
     * new DynamoDBScanExpression().withProjectionExpression("PostId")); JSONArray
     * documentBatch = new JSONArray();
     * 
     * for(int i = startingPostNumber; i < (startingPostNumber+1000); i++) //this
     * will upload 1000 posts at a time { if(i==(startingPostNumber+1000)) //if it's
     * the last post, figure out where to start from next
     * System.out.println("Start from this post on next iteration:" +
     * allPosts.get(i+1).getPostId());
     * 
     * Post post = allPosts.get(i); if(post.equals(allPosts.get(allPosts.size()-1)))
     * //if the post is the last post in entire DB {
     * System.out.println(post.getPostId());
     * documentBatch.add(convertDynamoEntryToAddSDF(post.getPostId())); break; }
     * else { System.out.println(i); System.out.println(post.getPostId());
     * documentBatch.add(convertDynamoEntryToAddSDF(post.getPostId())); } }
     * 
     * //5242880 = 5mb byte[] bytes = documentBatch.toJSONString().getBytes(); long
     * contentLength = bytes.length;
     * 
     * InputStream inputStream = new ByteArrayInputStream(bytes);
     * UploadDocumentsRequest uploadDocumentsRequest = new UploadDocumentsRequest();
     * uploadDocumentsRequest.setDocuments(inputStream);
     * uploadDocumentsRequest.setContentType(ContentType.Applicationjson);
     * uploadDocumentsRequest.setContentLength(contentLength); UploadDocumentsResult
     * uploadDocumentsResult =
     * cloudSearchClient.uploadDocuments(uploadDocumentsRequest);
     * System.out.println(uploadDocumentsResult.getStatus()); }
     * 
     */
}
