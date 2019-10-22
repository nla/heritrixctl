package au.gov.nla.heritrixctl;

public class UriTotalsReport {
    private long downloadedUriCount;
    private long queuedUriCount;
    private long totalUriCount;
    private long futureUriCount;

    public long getDownloadedUriCount() {
        return downloadedUriCount;
    }

    public long getQueuedUriCount() {
        return queuedUriCount;
    }

    public long getTotalUriCount() {
        return totalUriCount;
    }

    public long getFutureUriCount() {
        return futureUriCount;
    }
}
