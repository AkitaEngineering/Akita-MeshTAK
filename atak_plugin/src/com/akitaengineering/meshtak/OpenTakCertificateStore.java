package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Imports a TAK client PKCS#12 bundle into app-private storage and exposes
 * certificate metadata. Passwords are never written to logs.
 */
public final class OpenTakCertificateStore {

    public static final String PREF_P12_PATH = "opentakserver_client_p12_path";
    public static final String PREF_P12_PASSWORD = "opentakserver_client_p12_password";
    public static final String PREF_IMPORTED_ALIAS = "opentakserver_client_p12_alias";

    private static final String TAG = "OpenTakCertStore";
    private static final String STORE_FILE_NAME = "opentak-client.p12";
    private static final String PASSWORD_FILE_NAME = "opentak-client.pass";

    private OpenTakCertificateStore() {
    }

    public static File storeFile(Context context) {
        return new File(context.getApplicationContext().getNoBackupFilesDir(), STORE_FILE_NAME);
    }

    public static synchronized CertificateMetadata importPkcs12(Context context, File source, char[] password)
            throws GeneralSecurityException, IOException {
        if (source == null || !source.isFile()) {
            throw new IllegalArgumentException("PKCS#12 file is missing.");
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("PKCS#12 password is required.");
        }

        byte[] bytes = readAllBytes(source);
        KeyStore keyStore = loadKeyStore(bytes, password);
        CertificateMetadata metadata = metadataFromKeyStore(keyStore);
        if (metadata == null) {
            throw new GeneralSecurityException("PKCS#12 does not contain an X.509 certificate.");
        }

        File destination = storeFile(context);
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create certificate storage directory.");
        }
        try (FileOutputStream output = new FileOutputStream(destination)) {
            output.write(bytes);
            output.flush();
        }
        writePassword(context, password);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        preferences.edit()
                .putString(PREF_P12_PATH, source.getAbsolutePath())
                .putString(PREF_IMPORTED_ALIAS, metadata.alias)
                .apply();
        return metadata;
    }

    public static synchronized boolean hasImportedCertificate(Context context) {
        return storeFile(context).isFile() && passwordFile(context).isFile();
    }

    public static synchronized KeyStore loadImportedKeyStore(Context context)
            throws GeneralSecurityException, IOException {
        File store = storeFile(context);
        if (!store.isFile()) {
            throw new IllegalStateException("No imported OpenTAKServer client certificate.");
        }
        char[] password = readPassword(context);
        if (password == null || password.length == 0) {
            throw new IllegalStateException("Imported certificate password is unavailable.");
        }
        try {
            return loadKeyStore(readAllBytes(store), password);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    public static synchronized javax.net.ssl.SSLContext createSslContext(Context context)
            throws GeneralSecurityException, IOException {
        char[] password = readPassword(context);
        if (password == null || password.length == 0) {
            throw new IllegalStateException("Imported certificate password is unavailable.");
        }
        try {
            KeyStore keyStore = loadKeyStore(readAllBytes(storeFile(context)), password);
            javax.net.ssl.KeyManagerFactory keyManagerFactory =
                    javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);
            javax.net.ssl.TrustManagerFactory trustManagerFactory =
                    javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),
                    new java.security.SecureRandom());
            return sslContext;
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    public static synchronized CertificateMetadata getImportedMetadata(Context context) {
        try {
            if (!hasImportedCertificate(context)) {
                return null;
            }
            return metadataFromKeyStore(loadImportedKeyStore(context));
        } catch (Exception exception) {
            Log.w(TAG, "Unable to read imported certificate metadata");
            return null;
        }
    }

    public static String describe(Context context) {
        CertificateMetadata metadata = getImportedMetadata(context);
        if (metadata == null) {
            return "No client certificate imported";
        }
        return metadata.summary();
    }

    static KeyStore loadKeyStore(byte[] pkcs12Bytes, char[] password)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (ByteArrayInputStream input = new ByteArrayInputStream(pkcs12Bytes)) {
            keyStore.load(input, password);
        }
        return keyStore;
    }

    static CertificateMetadata metadataFromKeyStore(KeyStore keyStore) throws GeneralSecurityException {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate instanceof X509Certificate) {
                return CertificateMetadata.from((X509Certificate) certificate, alias);
            }
        }
        return null;
    }

    private static File passwordFile(Context context) {
        return new File(context.getApplicationContext().getNoBackupFilesDir(), PASSWORD_FILE_NAME);
    }

    private static void writePassword(Context context, char[] password) throws IOException {
        byte[] utf8 = new String(password).getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream output = new FileOutputStream(passwordFile(context))) {
            output.write(utf8);
            output.flush();
        } finally {
            java.util.Arrays.fill(utf8, (byte) 0);
        }
    }

    private static char[] readPassword(Context context) throws IOException {
        File file = passwordFile(context);
        if (!file.isFile()) {
            return null;
        }
        byte[] utf8 = readAllBytes(file);
        try {
            return new String(utf8, StandardCharsets.UTF_8).toCharArray();
        } finally {
            java.util.Arrays.fill(utf8, (byte) 0);
        }
    }

    private static byte[] readAllBytes(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream input = new FileInputStream(file)) {
            int offset = 0;
            while (offset < buffer.length) {
                int read = input.read(buffer, offset, buffer.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
        }
        return buffer;
    }

    public static final class CertificateMetadata {
        public final String alias;
        public final String subject;
        public final String issuer;
        public final Date notBefore;
        public final Date notAfter;
        public final String fingerprintSha256;

        private CertificateMetadata(String alias,
                                    String subject,
                                    String issuer,
                                    Date notBefore,
                                    Date notAfter,
                                    String fingerprintSha256) {
            this.alias = alias;
            this.subject = subject;
            this.issuer = issuer;
            this.notBefore = notBefore;
            this.notAfter = notAfter;
            this.fingerprintSha256 = fingerprintSha256;
        }

        static CertificateMetadata from(X509Certificate certificate, String alias) throws GeneralSecurityException {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] fingerprint = digest.digest(certificate.getEncoded());
                return new CertificateMetadata(
                        alias,
                        certificate.getSubjectDN().getName(),
                        certificate.getIssuerDN().getName(),
                        certificate.getNotBefore(),
                        certificate.getNotAfter(),
                        toHex(fingerprint));
            } catch (java.security.cert.CertificateEncodingException exception) {
                throw new GeneralSecurityException("Unable to encode certificate", exception);
            }
        }

        public boolean isExpired(long nowMillis) {
            return notAfter != null && nowMillis > notAfter.getTime();
        }

        public String summary() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            String expiry = notAfter == null ? "unknown" : format.format(notAfter);
            return "CN imported • exp " + expiry + " • " + shortFingerprint();
        }

        public String shortFingerprint() {
            if (fingerprintSha256 == null || fingerprintSha256.length() < 8) {
                return "";
            }
            return fingerprintSha256.substring(0, 8);
        }

        private static String toHex(byte[] bytes) {
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format(Locale.US, "%02x", value));
            }
            return builder.toString();
        }
    }
}
