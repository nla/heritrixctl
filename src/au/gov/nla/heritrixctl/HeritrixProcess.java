package au.gov.nla.heritrixctl;

import java.io.Closeable;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

public class HeritrixProcess implements Closeable {
    private final Process process;
    private final HeritrixClient client;

    HeritrixProcess(Process process, HeritrixClient client) {
        this.process = process;
        this.client = client;
    }

    /**
     * Shuts down Heritrix and waits for it to exit. Sends a kill signal if exiting takes longer than 60 seconds.
     */
    @Override
    public void close() {
        process.destroy();
        try {
            process.waitFor(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // interrupted
        }
        process.destroyForcibly();
    }

    public Process getProcess() {
        return process;
    }

    public HeritrixClient getClient() {
        return client;
    }

    public Engine getEngine() {
        return client.getEngine();
    }

    /**
     * Polls the Heritrix process until it is started and accepting requests.
     *
     * @throws HeritrixException if statup takes longer than 60 seconds or an unexpected error occurs
     */
    public HeritrixProcess waitForStartup() {
        long deadline = System.nanoTime() + 60000000000L;
        while (true) {
            if (!process.isAlive()) {
                throw new IllegalStateException("Heritrix process exited with value " + process.exitValue());
            }
            try {
                client.getEngine().getHeritrixVersion();
                break;
            } catch (HeritrixException e) {
                if (e.getCause() instanceof ConnectException && System.nanoTime() < deadline) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ex) {
                        throw new HeritrixException(ex);
                    }
                } else {
                    throw e;
                }
            }
        }
        return this;
    }
}
