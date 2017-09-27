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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
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

    public DALNSoundCloudClient() throws IOException {
        EnvironmentVariableCredentialsProvider creds = new EnvironmentVariableCredentialsProvider();
        AWSCredentials awsCreds = creds.getCredentials();

        dynamoDB = new DynamoDB(new AmazonDynamoDBClient(awsCreds));
        Table table = dynamoDB.getTable("DALN-Keys");
        Item key = table.getItem("key", "SoundCloudAccessToken");
        String tokenString = key.get("value").toString();
        Token token = new Token(tokenString, null, "non-expiring");

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


        System.out.println("initialize and upload");
        assetID = assetDetails.get("assetId");
        System.out.println("assetid: " + assetID);

        String fileName = assetDetails.get("assetName");
        System.out.println("file name: " + fileName);
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
                System.out.println("\n" + Http.getJSON(resp).toString(4));
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
        //soundLocations[0] = track.getPermalinkUrl();
        //soundLocations[1] = track.getUri();
        return soundLocations;
    }

}
