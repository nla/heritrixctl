package au.gov.nla.heritrixctl;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Runs a Heritrix process. Also includes utility methods for downloading and installing Heritrix.
 *
 * Important: By default a random admin password is generated for each Heritrix process. You may set a known password
 * using {@link HeritrixRunner#setAdminPassword(String)}.
 */
public class HeritrixRunner {
    private static final String LASTEST_SNAPSHOT_URL = "http://builds.archive.org/maven2/org/archive/heritrix/heritrix/3.4.0-SNAPSHOT/heritrix-3.4.0-SNAPSHOT-dist.zip";
    private final Path home;
    private String javaCommand = "java";
    private String adminUsername = "admin";
    private String adminPassword;
    private List<String> jvmOptions = Arrays.asList("-Xmx256m");
    private int webPort = 8443;

    /**
     * Downloads and unpacks the latest Heritrix snapshot from builds.archive.org.
     *
     * @param destDir directory to unpack heritrix into
     */
    public static HeritrixRunner downloadLatestSnapshot(Path destDir) throws IOException {
        download(LASTEST_SNAPSHOT_URL, destDir);
        return new HeritrixRunner(destDir);
    }

    /**
     * Downloads and unpacks a Heritrix dist zip.
     *
     * @param distZipUrl the URL of a heritrix dist zip file
     * @param destDir directory to unpack heritrix into
     */
    public static HeritrixRunner download(String distZipUrl, Path destDir) throws IOException {
        Path flagFile = destDir.resolve(".unpacked");
        if (!Files.exists(flagFile)) {
            System.out.println("Downloading " + distZipUrl);
            try (ZipInputStream zis = new ZipInputStream(new URL(distZipUrl).openStream())) {
                for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                    String name = entry.getName().replaceFirst("[^/]+/", "");
                    Path path = destDir.resolve(name);
                    if (!path.startsWith(destDir)) throw new IllegalArgumentException("zip entry outside target dir");
                    Files.createDirectories(path.getParent());
                    if (entry.isDirectory()) {
                        Files.createDirectories(path);
                    } else {
                        Files.copy(zis, path, REPLACE_EXISTING);
                    }
                }
            }
            Files.createFile(flagFile);
        }
        return new HeritrixRunner(destDir);
    }

    /**
     * Creates a new HeritrixRunner from a Heritrix install in a local directory.
     *
     * @param heritrixHome is the path Heritrix is installed in (contents of dist zip)
     */
    public HeritrixRunner(Path heritrixHome) {
        this.home = heritrixHome;
    }

    /**
     * Starts a new Heritrix process.
     */
    public HeritrixProcess start() throws IOException {
        String password = adminPassword;
        if (password == null) {
            password = UUID.randomUUID().toString();
        }
        List<String> command = new ArrayList<>();
        command.add(javaCommand);
        command.add("-cp");
        command.add("lib/*");
        command.addAll(jvmOptions);
        command.add("org.archive.crawler.Heritrix");
        command.add("-a");
        command.add(adminUsername + ":" + adminPassword);
        command.add("-p");
        command.add(Integer.toString(webPort));

        Process process = new ProcessBuilder(command)
                .directory(home.toFile())
                .inheritIO()
                .start();
        HeritrixClient client = new HeritrixClient("https://localhost:" + webPort + "/engine", adminUsername, adminPassword);
        client.ignoreCertificateErrors(); // FIXME: should we try to read adhoc.keystore?
        return new HeritrixProcess(process, client);
    }

    public HeritrixRunner setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
        return this;
    }

    public HeritrixRunner setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
        return this;
    }

    public HeritrixRunner setJvmOptions(List<String> jvmOptions) {
        this.jvmOptions = jvmOptions;
        return this;
    }

    public HeritrixRunner setJvmOptions(String... jvmOptions) {
        return setJvmOptions(Arrays.asList(jvmOptions));
    }

    public HeritrixRunner setWebPort(int webPort) {
        this.webPort = webPort;
        return this;
    }

    public HeritrixRunner setJavaCommand(String javaCommand) {
        this.javaCommand = javaCommand;
        return this;
    }
}
