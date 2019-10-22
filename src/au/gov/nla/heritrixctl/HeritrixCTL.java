package au.gov.nla.heritrixctl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
                run(Paths.get(args[0]));
                break;
            default:
                System.err.println("heritrixctl: Unknown command: " + args[0]);
                System.err.println("Try: 'heritrixctl help'");
                return 1;
        }
        return 0;
    }

    private HeritrixClient getClient() {
        if (client == null) {
            if (System.getenv("HERITRIX_URL") != null) {
                client = new HeritrixClient(System.getenv("HERITRIX_URL"), System.getenv("HERITRIX_USER"), System.getenv("HERITRIX_PASSWORD"));
            } else {
                throw new IllegalStateException("HERITRIX_URL, HERITRIX_USER and HERITRIX_PASSWORD must be set");
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
        job.waitForState(JobState.NASCENT);
        job.launch();
        job.waitForState(JobState.PAUSED);
        job.unpause();
    }

    private static void usage(PrintStream out) {
        out.println("Usage: heritrixctl [subcommand]\n" +
                "\n" +
                "Subcommands:" +
                "  run [crawler-beans.cxml]");
    }
}
