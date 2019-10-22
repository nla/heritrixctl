package au.gov.nla.heritrixctl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.EnumSet;

public class Job extends HeritrixResource {
    private String shortName;
    private JobState state;
    private URI configUrl;
    private Path configPath;
    private String url;
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

    @JsonProperty("crawlControllerState")
    public JobState getState() {
        populate();
        return state;
    }

    public UriTotalsReport getUriTotalsReport() {
        populate();
        return uriTotalsReport;
    }

    public SizeTotalsReport getSizeTotalsReport() {
        populate();
        return sizeTotalsReport;
    }

    public String getShortName() {
        populate();
        return shortName;
    }

    @JsonProperty("primaryConfigUrl")
    public URI getConfigUrl() {
        populate();
        return configUrl;
    }

    @JsonProperty("primaryConfig")
    public Path getConfigPath() {
        populate();
        return configPath;
    }

    public String getConfig() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try (InputStream stream = openConfigStream()) {
            while (true) {
                int n = stream.read(buf);
                if (n < 0) break;
                baos.write(buf, 0, n);
            }
            return baos.toString("utf-8");
        } catch (IOException e) {
            throw new HeritrixException(e);
        }
    }

    public InputStream openConfigStream() {
        return client.openStream(getConfigUrl());
    }

    public void setConfig(InputStream config) {
        client.openStream("PUT", getConfigUrl(), "application/xml", config);
    }

    public String getUrl() {
        populate();
        return url;
    }

    public void waitForState(EnumSet<JobState> target, EnumSet<JobState> allowed, long millis) {
        JobState initial = getState();
        long deadline = System.nanoTime() + millis * 1000000;
        while (true) {
            JobState state = getState();
            if (target.contains(state)) break;
            if (state != initial && !allowed.contains(state)) throw new IllegalStateException("Expected crawl state " + target + " but got " + state);
            if (millis > 0 && System.nanoTime() >= deadline) throw new HeritrixException("Timed out waiting for state " + state);
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                return;
            }
            refresh();
        }
    }
}
