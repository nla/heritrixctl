package au.gov.nla.heritrixclient;

import java.net.URI;

public class Job extends HeritrixResource {
    private State crawlControllerState;
    private UriTotalsReport uriTotalsReport;
    private SizeTotalsReport sizeTotalsReport;

    Job(HeritrixClient client, URI uri) {
        super(client, uri);
    }

    public void build() { action("build"); }
    public void launch() { action("launch"); }
    public void pause() { action("pause"); }
    public void unpause() { action("unpause"); }
    public void teardown() { action("teardown"); }

    private void action(String action) {
        client.POST(uri, this, "action", action);
    }

    public State getCrawlControllerState() {
        populate();
        return crawlControllerState;
    }

    public UriTotalsReport getUriTotalsReport() {
        populate();
        return uriTotalsReport;
    }

    public SizeTotalsReport getSizeTotalsReport() {
        populate();
        return sizeTotalsReport;
    }
}
