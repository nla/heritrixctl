package au.gov.nla.heritrixctl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HeritrixClient {
    private final URI uri;
    private final XmlMapper xmlMapper = new XmlMapper();
    private SSLSocketFactory sslSocketFactory;
    private final AtomicReference<DigestChallenge> lastChallenge = new AtomicReference<>();
    private final String username;
    private final String password;
    boolean debug = false;

    public HeritrixClient(String url, String username, String password) {
        if (!url.endsWith("/")) {
            url += "/";
        }
        this.uri = URI.create(url);
        this.username = username;
        this.password = password;
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public HeritrixClient ignoreCertificateErrors() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }}, null);
            this.sslSocketFactory = sslContext.getSocketFactory();
            return this;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    InputStream openStream(String method, URI uri, String requestType, InputStream requestBody) {
        try {
            DigestChallenge challenge = lastChallenge.get();
            for (int tries = 0; tries < 3; tries++) {
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                if (sslSocketFactory != null && connection instanceof HttpsURLConnection) {
                    HttpsURLConnection https = (HttpsURLConnection) connection;
                    https.setHostnameVerifier((hostname, session) -> true);
                    https.setSSLSocketFactory(sslSocketFactory);
                }
                connection.setInstanceFollowRedirects(false); // automatic redirect handling won't preserve auth header
                connection.setRequestMethod(method);
                connection.setRequestProperty("User-Agent", "heritrix-client");
                connection.setRequestProperty("Accept", "application/xml");
                if (challenge != null) {
                    connection.setRequestProperty("Authorization", challenge.authorize(username, password, method, uri.getPath()));
                }

                if (requestBody != null) {
                    connection.setDoOutput(true);
                    if (requestType != null) {
                        connection.setRequestProperty("Content-Type", requestType);
                    }
                    try (OutputStream out = connection.getOutputStream()) {
                        byte[] buf = new byte[4096];
                        while (true) {
                            int n = requestBody.read(buf);
                            if (n < 0) break;
                            out.write(buf, 0, n);
                        }
                    }
                }

                try {
                    InputStream body = connection.getInputStream();
                    if (connection.getResponseCode() == 303) {
                        body.close();
                        URI location = uri.resolve(connection.getHeaderField("Location"));
                        return openStream("GET", location, null, null);
                    }
                    return body;
                } catch (IOException e) {
                    String authenticate = connection.getHeaderField("WWW-Authenticate");
                    if (connection.getResponseCode() == 401 && authenticate != null && authenticate.startsWith("Digest ")) {
                        challenge = DigestChallenge.parse(authenticate);
                        lastChallenge.set(challenge);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            throw new HeritrixException(e);
        }
        throw new HeritrixException("Authentication failed: " + method + " " + uri);
    }

    InputStream openStream(URI uri) {
        return openStream("GET", uri, null, null);
    }

    private <T> T sendRequest(String method, URI uri, String[] keysAndValues, ObjectReader objectReader) {
        String requestType = null;
        InputStream requestBody = null;
        if (keysAndValues != null) {
            requestType = "application/x-www-form-urlencoded";
            StringBuilder out = new StringBuilder();
            try {
                for (int i = 0; i < keysAndValues.length; i += 2) {
                    if (i != 0) out.append('&');
                    out.append(URLEncoder.encode(keysAndValues[i], "utf-8"));
                    out.append('=');
                    out.append(URLEncoder.encode(keysAndValues[i + 1], "utf-8"));
                }
            } catch (UnsupportedEncodingException e) {
                throw new HeritrixException(e);
            }
            requestBody = new ByteArrayInputStream(out.toString().getBytes(UTF_8));
        }

        try (InputStream body = openStream(method, uri, requestType, requestBody)) {
            InputStream debugBody = debug ? new DebugInputStream(body) : body;
            return objectReader.readValue(debugBody);
        } catch (IOException e) {
            throw new HeritrixException(e);
        }
    }

    <T> T POST(URI uri, Object objectToUpdate, String... keysAndValues) {
        return sendRequest("POST", uri, keysAndValues, xmlMapper.readerForUpdating(objectToUpdate));
    }

    <T> T GET(URI uri, Object objectToUpdate) {
        return sendRequest("GET", uri, null, xmlMapper.readerForUpdating(objectToUpdate));
    }

    public Job getJob(String jobName) {
        return new Job(this, uri.resolve("job/" + jobName));
    }

    public Engine getEngine() {
        return new Engine(this, uri);
    }
}
