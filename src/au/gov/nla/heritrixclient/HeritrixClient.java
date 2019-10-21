package au.gov.nla.heritrixclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.net.ssl.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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

    private <T> T sendRequest(String method, URI uri, String[] keysAndValues, ObjectReader objectReader) {
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
                connection.setRequestProperty("User-Agent", "pandas-gatherer");
                connection.setRequestProperty("Accept", "application/xml");
                if (challenge != null) {
                    connection.setRequestProperty("Authorization", challenge.authorize(username, password, method, uri.getPath()));
                }

                if (keysAndValues != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), UTF_8))) {
                        for (int i = 0; i < keysAndValues.length; i += 2) {
                            if (i != 0) out.append('&');
                            out.append(URLEncoder.encode(keysAndValues[i], "utf-8"));
                            out.append('=');
                            out.append(URLEncoder.encode(keysAndValues[i + 1], "utf-8"));
                        }
                    }
                }
                try (InputStream body = connection.getInputStream()) {
                    if (connection.getResponseCode() == 303) {
                        URI location = uri.resolve(connection.getHeaderField("Location"));
                        return sendRequest("GET", location, null, objectReader);
                    }
                    return objectReader.readValue(body);
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
