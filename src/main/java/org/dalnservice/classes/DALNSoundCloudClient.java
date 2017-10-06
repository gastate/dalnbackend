package org.dalnservice.classes;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.soundcloud.api.*;
import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by Shakib on 7/18/2016.
 *
 * This class handles the task of uploading an audio to SoundCloud and retrieving its download location.
 * The constructor extracts all needed values from the HashMap and places them into variables. The values
 * will be used as inputs for the upload.
 */
public class DALNSoundCloudClient
{

    private String fullTitle;
    private String assetID;
    private SoundCloud soundcloud;
    private Track track;
    private ApiWrapper wrapper;
    private DynamoDB dynamoDB;
    private JSONObject uploadFileResponse;
    private String permalinkUrl;
    private String uri;

    public DALNSoundCloudClient() throws IOException {
        EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
        AWSCredentials awsCreds = creds.getCredentials();

        dynamoDB = new DynamoDB(new AmazonDynamoDBClient(awsCreds));
        Table table = dynamoDB.getTable("DALN-Keys");
        Item key = table.getItem("key", "SoundCloudAccessToken");
        String tokenString = key.get("value").toString();
        Token token = new Token(tokenString, null, "non-expiring");
        //uploadFileResponse = new JSONObject();
        permalinkUrl = null;
        uri = null;

        wrapper = new ApiWrapper(System.getenv("SoundCloudClientID"), System.getenv("SoundCloudClientSecret"), null, token);
        System.out.println("SoundCloud connection successful");

        //Connect to SoundCloud
        /*boolean isSoundCloudConnected;
        do {
            isSoundCloudConnected = connectToSoundCloud();
            if(!isSoundCloudConnected)
                System.out.println("SoundCloud connection failed. Retrying..." );
            else
                System.out.println("SoundCloud connection successful.");
        }
        while(!isSoundCloudConnected);*/
    }

    public boolean generateSoundCloudToken()
    {
        Table table = dynamoDB.getTable("DALN-Keys");
        ApiWrapper wrapper = new ApiWrapper(System.getenv("SoundCloudClientID"), System.getenv("SoundCloudClientSecret"), null, null);
        Token token = null;
        try {
            token = wrapper.login(System.getenv("SoundCloudUser"), System.getenv("SoundCloudPassword"));
        } catch (IOException e) {
            return false;
        }
        String newAccessToken = token.access;
        Item updatedKey = new Item().withPrimaryKey("key", "SoundCloudAccessToken").withString("value", newAccessToken);
        table.putItem(updatedKey);

        return true;
    }

    /*public boolean connectToSoundCloud() throws IOException {
        //Connect to SoundCloud

        soundcloud = new SoundCloud(
                System.getenv("SoundCloudClientID"),
                System.getenv("SoundCloudClientSecret"));

        soundcloud.login(
                System.getenv("SoundCloudUser"),
                System.getenv("SoundCloudPassword")
        );

        try {
            if (soundcloud.getMe().toString() == null)
                return false;
        }
        catch(NullPointerException e)
        {
            return false;
        }
        return true;
    }*/

    public void initializeAndUpload(HashMap<String, String> assetDetails, File file) throws IOException {

        assetID = assetDetails.get("assetId");
        System.out.println("assetid: " + assetID);

        String fileName = assetDetails.get("assetName");
        String originalPostTitle = assetDetails.get("postTitle");

        track = new Track();

        String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
        fullTitle = originalPostTitle + " - " + fileNameNoExt;

        System.out.println("full title:" + fullTitle);
        uploadSound(file);
    }



    private void uploadSound(File file) throws IOException {

        System.out.println("Uploading " + file);
        try {
            HttpResponse resp = wrapper.post(Request.to(Endpoints.TRACKS)
                    .add(Params.Track.TITLE, fullTitle)
                    .add(Params.Track.TAG_LIST, assetID)
                    .withFile(Params.Track.ASSET_DATA, file)
                    // you can add more parameters here, e.g.
                    // .withFile(Params.Track.ARTWORK_DATA, file)) /* to add artwork */

                    // set a progress listener (optional)
                    .setProgressListener(amount -> System.err.print(".")));

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                System.out.println("\n201 Created " + resp.getFirstHeader("Location").getValue());
                // dump the representation of the new track
                //System.out.println("\n" + Http.getJSON(resp).toString(4));

                String jsonString = Http.getJSON(resp).toString(4);
                JSONParser parser = new JSONParser();
                org.json.simple.JSONObject respJson = null;
                try {
                    respJson = (org.json.simple.JSONObject) parser.parse(jsonString);
                } catch (ParseException e) {
                    e.printStackTrace();
                    System.out.println("Could not parse create response");
                }
                permalinkUrl = respJson.get("permalink_url").toString();
                uri = resp.getFirstHeader("Location").getValue();

            } else {
                System.err.println("Invalid status received: " + resp.getStatusLine());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
        try {
            Track newTrack = new Track(fullTitle, file.getCanonicalPath());
            newTrack.setTagList(assetID);
            track = soundcloud.postTrack(newTrack);
            System.out.println("track id:" + track.getId());

        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }*/

    }

    public boolean deleteSound(String trackId) throws IOException {
        HttpResponse response = wrapper.delete(
                Request.to("/tracks/"+trackId));


        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return true;
        }
        else if(response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND)
        {
            System.out.println("Sound not found or may have already been deleted");
            return true;
        }
        return false;
    }

    public String[] getSoundLocation()
    {
        String[] soundLocations = new String[2];
        //soundLocations[0] = uploadFileResponse.getString("permalink_url");
        /*String trackId = uri.substring(uri.lastIndexOf('/'));
        try {
            HttpResponse getResponse = wrapper.get(Request.to("/tracks/" + trackId ));
            System.out.println("\n" + Http.formatJSON(Http.getString(getResponse)));
            //for(Header header : getResponse.getAllHeaders())
              //  System.out.println("Header: " + header.getName() + ":" + header.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not find track");
            soundLocations[0] = null;
        }*/

        soundLocations[0] = permalinkUrl;
        System.out.println("Sound location 0: " + soundLocations[0]);
        //soundLocations[1] = uploadFileResponse.getString("uri");
        soundLocations[1] = uri;
        System.out.println("Sound location 1: " + soundLocations[1]);

        return soundLocations;
    }

}
