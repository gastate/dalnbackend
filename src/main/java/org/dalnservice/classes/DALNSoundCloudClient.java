package org.dalnservice.classes;


import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Request;
import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

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

    public DALNSoundCloudClient() throws IOException {
        //Connect to SoundCloud
        soundcloud = new SoundCloud(
                System.getenv("SoundCloudClientID"),
                System.getenv("SoundCloudClientSecret"),
                System.getenv("SoundCloudUser"),
                System.getenv("SoundCloudPassword")
        );
        System.out.println(soundcloud.getMe());
    }

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



    private void uploadSound(File file)
    {
        try {
            Track newTrack = new Track(fullTitle, file.getCanonicalPath());
            newTrack.setTagList(assetID);
            track = soundcloud.postTrack(newTrack);
            System.out.println("track id:" + track.getId());



        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteSound(String trackId) throws IOException {
        ApiWrapper wrapper = new ApiWrapper(System.getenv("SoundCloudClientID"), System.getenv("SoundCloudClientSecret"), null, null);
        wrapper.login(System.getenv("SoundCloudUser"), System.getenv("SoundCloudPassword"));
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
        soundLocations[0] = track.getPermalinkUrl();
        soundLocations[1] = track.getUri();
        return soundLocations;
    }

}
