package au.gov.nla.heritrixctl;

import java.net.URI;

class HeritrixResource {
    protected final HeritrixClient client;
    final URI uri;
    private boolean populated = false;

    HeritrixResource(HeritrixClient client, URI uri) {
        this.client = client;
        this.uri = uri;
    }

    void populate() {
        if (!populated) {
            refresh();
        }
    }

    public void refresh() {
        client.GET(uri, this);
        populated = true;
    }
}
