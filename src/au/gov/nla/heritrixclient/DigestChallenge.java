package au.gov.nla.heritrixclient;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

class DigestChallenge {
    private static Pattern AUTH_RE = Pattern.compile("(\\w+)=(?:\"([^\"]*)\"|([^\",]*))(?:, *|$)");

    final String realm;
    final String nonce;
    final String algorithm;

    DigestChallenge(String realm, String nonce, String algorithm) {
        this.realm = realm;
        this.nonce = nonce;
        this.algorithm = algorithm;
    }

    static DigestChallenge parse(String challenge) {
        Matcher m = AUTH_RE.matcher(challenge.substring("Digest ".length()));
        String realm = null;
        String nonce = null;
        String algorithm = null;
        while (m.lookingAt()) {
            String key = m.group(1);
            String value = m.group(2);
            if (value == null) value = m.group(3);
            if (key.equalsIgnoreCase("realm")) {
                realm = value;
            } else if (key.equalsIgnoreCase("nonce")) {
                nonce = value;
            } else if (key.equalsIgnoreCase("algorithm")) {
                algorithm = value;
            }
            m.region(m.end(), m.regionEnd());
        }
        return new DigestChallenge(realm, nonce, algorithm);
    }

    String authorize(String username, String password, String method, String uri) throws IOException {
        if (!algorithm.equalsIgnoreCase("md5")) {
            throw new IOException("Unsupported digest algorithm: " + algorithm);
        }
        String ha1 = md5(username + ":" + realm + ":" + password);
        String ha2 = md5(method + ":" + uri);
        String response = md5(ha1 + ":" + nonce + ":" + ha2);
        return "Digest username=\"" + username + "\", realm=\"" + realm + "\", uri=\"" + uri + "\", nonce=\"" + nonce +
                "\", response=\"" + response + "\"";
    }

    private static String md5(String s) {
        try {
            return hexEncode(MessageDigest.getInstance("md5").digest(s.getBytes(UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String hexEncode(byte[] bytes) {
        String alphabet = "0123456789abcdef";
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            chars[i * 2] = alphabet.charAt((bytes[i] & 0xFF) >>> 4);
            chars[i * 2 + 1] = alphabet.charAt(bytes[i] & 0xf);
        }
        return new String(chars);
    }
}
