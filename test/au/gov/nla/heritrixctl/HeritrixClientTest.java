package au.gov.nla.heritrixctl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

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
    void testEngine() {
        Engine engine = heritrix.getClient().getEngine();
        assertNotNull(engine.getHeritrixVersion());
    }

    @Test
    void testCreateJob() {
        String testConfig = "hello";
        heritrix.getClient().debug = true;
        Engine engine = heritrix.getClient().getEngine();
        Job job = engine.createJob("myjob");
        job.build();
        job.setConfig(new ByteArrayInputStream(testConfig.getBytes(StandardCharsets.UTF_8)));
        assertEquals(JobState.NASCENT, job.getState());
        assertNotNull(job.getConfigUrl());
        assertTrue(Files.exists(job.getConfigPath()));
        assertEquals(testConfig, job.getConfig());
    }
}