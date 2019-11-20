package org.dalnservice.classes;

/**
 * Created by Shakib on 8/4/2016.
 */

public class PresignedUrl {
    private String url = "";

    public PresignedUrl() {
    }

    public PresignedUrl(String url) {
        this.setUrl(url);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}