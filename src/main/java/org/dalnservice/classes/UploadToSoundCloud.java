package org.dalnservice.classes;


import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Created by Shakib on 7/18/2016.
 *
 * This class handles the task of uploading an audio to SoundCloud and retrieving its download location.
 * The constructor extracts all needed values from the HashMap and places them into variables. The values
 * will be used as inputs for the upload.
 */
public class UploadToSoundCloud
{

    private String fullTitle, fileName, assetID, originalPostTitle;
    private SoundCloud soundcloud;
    private Track track;

    public UploadToSoundCloud() throws IOException {
        //Connect to SoundCloud
        soundcloud = new SoundCloud(
                System.getenv("SoundCloudClientID"),
                System.getenv("SoundCloudClientSecret"),
                System.getenv("SoundCloudUser"),
                System.getenv("SoundCloudPassword")
        );
    }

    public void initializeAndUpload(HashMap<String, String> assetDetails, File file) throws IOException {

        fileName = assetDetails.get("assetName");
        assetID = assetDetails.get("assetID");
        originalPostTitle = assetDetails.get("postTitle");
        track = new Track();

        String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
        fullTitle = originalPostTitle + " - " + fileNameNoExt;

        uploadSound(file);
    }



    public void uploadSound(File file)
    {
        try {
            Track newTrack = new Track(fullTitle, file.getCanonicalPath());
            newTrack.setTagList(assetID);
            track = soundcloud.postTrack(newTrack);
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    public String[] getSoundLocation()
    {
        String[] soundLocations = new String[2];
        soundLocations[0] = track.getPermalinkUrl();
        soundLocations[1] = track.getUri();
        return soundLocations;
    }

}
