package au.gov.nla.heritrixctl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.EnumSet;

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

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path).sorted(Comparator.reverseOrder())
                .forEach(HeritrixClientTest::delete);
    }

    @Test
    void testEngine() {
        Engine engine = heritrix.getClient().getEngine();
        assertNotNull(engine.getHeritrixVersion());
    }

    @Test
    void testCreateJob() throws IOException {
        String testConfig = "hello";
        heritrix.getClient().debug = true;
        Engine engine = heritrix.getClient().getEngine();
        Job job = engine.createJob("myjob");
        try {
            job.build();
            job.waitForState(EnumSet.of(JobState.NASCENT), EnumSet.noneOf(JobState.class), 20000);
            assertEquals(JobState.NASCENT, job.getState());
            assertNotNull(job.getConfigUrl());
            assertTrue(Files.exists(job.getConfigPath()));
            job.setConfig(new ByteArrayInputStream(testConfig.getBytes(StandardCharsets.UTF_8)));
            assertEquals(testConfig, job.getConfig());
        } finally {
            try {
                job.teardown();
            } finally {
                deleteTree(Paths.get("target/heritrix/jobs/myjob"));
            }
        }
    }
}