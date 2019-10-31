package au.gov.nla.heritrixctl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import static au.gov.nla.heritrixctl.JobState.*;

public class HeritrixCTL {

    private HeritrixClient client;

    public static void main(String[] args) throws IOException {
        System.exit(new HeritrixCTL().exec(args));
    }

    private int exec(String[] args) throws IOException {
        if (args.length == 0) {
            usage(System.out);
            return 0;
        }

        switch (args[0]) {
            case "run":
                run(Paths.get(args[1]));
                break;
            case "build":
                getClient().getJob(args[1]).build();
                break;
            case "launch":
                getClient().getJob(args[1]).launch();
                break;
            case "pause":
                getClient().getJob(args[1]).pause();
                break;
            case "teardown":
                getClient().getJob(args[1]).teardown();
                break;
            case "unpause":
                getClient().getJob(args[1]).unpause();
                break;
            default:
                System.err.println("heritrixctl: Unknown command: " + args[0]);
                System.err.println("Try: 'heritrixctl help'");
                return 1;
        }
        return 0;
    }

    private HeritrixClient getClient() throws IOException {
        if (client == null) {
            if (System.getenv("HERITRIX_URL") != null) {
                client = new HeritrixClient(System.getenv("HERITRIX_URL"), System.getenv("HERITRIX_USER"), System.getenv("HERITRIX_PASSWORD"));
            } else if (System.getenv("HERITRIX_HOME") != null) {
                HeritrixRunner runner = HeritrixRunner.downloadLatestSnapshot(Paths.get(System.getenv("HERITRIX_HOME")));
                if (System.getenv("HERITRIX_PORT") != null) {
                    runner.setWebPort(Integer.parseInt(System.getenv("HERITRIX_PORT")));
                }
                HeritrixProcess process = runner.start();
                Runtime.getRuntime().addShutdownHook(new Thread(process::close));
                client = process.waitForStartup().getClient();
            } else {
                throw new IllegalStateException("Either HERITRIX_HOME or HERITRIX_URL, HERITRIX_USER and HERITRIX_PASSWORD must be set");
            }
        }
        return client;
    }

    private void run(Path configFile) throws IOException {
        Job job = getClient().getEngine().createJob(configFile.getFileName().toString().replaceFirst("\\..*", ""));
        try (InputStream stream = Files.newInputStream(configFile)) {
            job.setConfig(stream);
        }
        job.build();
        job.waitForState(EnumSet.of(NASCENT), EnumSet.noneOf(JobState.class), 60000);
        job.launch();
        job.waitForState(EnumSet.of(PAUSED), EnumSet.of(PREPARING), 60000);
        job.unpause();
        job.waitForState(EnumSet.of(FINISHED, EMPTY), EnumSet.of(PAUSED, PAUSING, RUNNING, STOPPING), 0);
    }

    private static void usage(PrintStream out) {
        out.println("Usage: heritrixctl [subcommand]\n" +
                "\n" +
                "Subcommands:\n" +
                "  run [crawler-beans.cxml]\n" +
                "\n" +
                "Job control:\n" +
                "  build <job-name>\n" +
                "  launch <job-name>\n" +
                "  pause <job-name>\n" +
                "  teardown <job-name>\n" +
                "  unpause <job-name>\n");
    }
}
