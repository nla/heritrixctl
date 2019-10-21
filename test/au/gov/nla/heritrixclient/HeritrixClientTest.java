package au.gov.nla.heritrixclient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeritrixClientTest {

    private static HeritrixProcess heritrix;

    @BeforeAll
    static void startHeritrix() throws IOException {
        Path destDir = Paths.get("target/heritrix");
        heritrix = HeritrixRunner.downloadLatestSnapshot(destDir)
                .setWebPort(12443)
                .start().waitForStartup();
    }

    @AfterAll
    static void stopHeritrix() {
        heritrix.close();
    }

    @Test
    void testEngine() throws IOException, InterruptedException {
        Engine engine = heritrix.getClient().getEngine();
        assertNotNull(engine.getHeritrixVersion());
    }

}