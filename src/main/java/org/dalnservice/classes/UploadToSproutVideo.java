package org.dalnservice.classes;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Shakib on 7/16/2016.
 *
 * This class handles the task of uploading a video to SproutVideo and retrieving its download location.
 * The constructor extracts all needed values from the HashMap and places them into variables. The values
 * will be used as inputs for the upload.
 */
public class UploadToSproutVideo
{
    private HashMap<String,String> assetDetails;
    private String dalnId, originalLink, originalPostTitle,fileName, assetID, fullTitle;
    private CloseableHttpClient httpClient;
    private HttpPost uploadFile;
    private HttpGet getFile;

    public UploadToSproutVideo() throws IOException {
        /**Connect to SproutVideo**/
        httpClient = HttpClients.createDefault();
        uploadFile = new HttpPost("https://api.sproutvideo.com/v1/videos");
        uploadFile.addHeader("SproutVideo-Api-Key", System.getenv("SproutVideoApiKey"));
        getFile =  new HttpGet("https://api.sproutvideo.com/v1/videos?order_by=created_at&order_dir=desc");
        getFile.addHeader("SproutVideo-Api-Key", System.getenv("SproutVideoApiKey"));
    }

    public void initializeAndUpload(HashMap<String, String> assetDetails, File file) throws IOException {
        this.assetDetails = assetDetails;
        fileName = assetDetails.get("assetName");
        assetID = assetDetails.get("assetID");
        originalPostTitle = assetDetails.get("postTitle");
        String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
        fullTitle = originalPostTitle + " - " + fileNameNoExt;

        uploadVideo(file);
    }

    public void uploadVideo(File file)
    {
        //SproutVideo API uploads accept Multipart
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //For the video submission
        builder.addTextBody("title", fullTitle, ContentType.TEXT_PLAIN);
        builder.addTextBody("privacy", 2 + "", ContentType.TEXT_PLAIN); //2 = public
        builder.addTextBody("tag_names", assetID, ContentType.TEXT_PLAIN);
        builder.addBinaryBody("source_video", file, ContentType.APPLICATION_OCTET_STREAM, fileName);
        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);

        CloseableHttpResponse postResponse = null;
        try{
            postResponse = httpClient.execute(uploadFile);
       } catch (IOException e) {
            System.out.println("\n"+fileName + " could not be uploaded to SproutVideo.");
        }
    }



    public String[] getSpoutVideoLocation() {
        //The HTTP Response must be parsed to retrieve the location of the uploaded video

        String[] videoLocations = new String[2];
        //String uploadedVideoTitle = assetDetails.get("assetId").toString();

        CloseableHttpResponse getResponse = null;
        //The getResponse must be parsed as a JSON to retrieve the downloaded location
        try {
            getResponse = httpClient.execute(getFile);
            String jsonString = EntityUtils.toString(getResponse.getEntity());
            //System.out.println(jsonString);
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            JSONArray jsonArray = (JSONArray) jsonObject.get("videos");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject videoInfo = (JSONObject) jsonArray.get(i);
                String videoTitle = videoInfo.get("title").toString();
                if (videoTitle.equals(fullTitle)) {
                    String videoID = videoInfo.get("id").toString();
                    String embedCode = videoInfo.get("embed_code").toString();
                    Element iframe = Jsoup.parse(embedCode).select("iframe").first();
                    videoLocations[0] = "https://mwharker.vids.io/videos/"+videoID+"/"+videoTitle;
                    videoLocations[1] = iframe.attr("src");

                    return videoLocations;
                }
            }
        } catch (ParseException | IOException | NullPointerException e) {
           //log.error("Problem getting video location.");
            e.printStackTrace();
        }
        return videoLocations;
    }


}
