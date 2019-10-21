package au.gov.nla.heritrixclient;

import java.net.URI;

public class Engine extends HeritrixResource {
    private String heritrixVersion;

    Engine(HeritrixClient client, URI uri) {
        super(client, uri);
    }

    public void addJobDir(String addpath) {
        client.POST(uri, this , "action", "add", "addpath", addpath);
    }

    public String getHeritrixVersion() {
        populate();
        return heritrixVersion;
    }
}
