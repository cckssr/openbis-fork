package ch.ethz.sis.afssftp.server;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

public class KeyStoreGenerator {

    public static void generateKeyStore(Path keystoreFilename, String alias, String passwordAsString) throws Exception {
        char[] password = passwordAsString.toCharArray();

        try {
            // 1. Initialize an empty JKS KeyStore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, password);

            // 2. Generate an RSA Key Pair
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();

            // 3. Generate a self-signed certificate using Bouncy Castle
            Certificate[] chain = new Certificate[]{
                    generateSelfSignedCertificate(keyPair, "CN=SFTP Server, O=MyCompany, C=US")
            };

            // 4. Set the key entry into the KeyStore
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), password, chain);

            // 5. Save the KeyStore to a file
            try (FileOutputStream fos = new FileOutputStream(keystoreFilename.toFile())) {
                keyStore.store(fos, password);
            }

            System.out.println("Success! KeyStore created: " + keystoreFilename);

        } catch (Exception e) {
            System.err.println("Error generating KeyStore: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Safely generates a self-signed certificate without using internal 'sun.*' APIs
     */
    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String dn) throws Exception {
        X500Name issuerAndSubject = new X500Name(dn);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 365L * 24 * 60 * 60 * 1000); // 1 year validity

        // Build the certificate structure
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerAndSubject,
                serialNumber,
                startDate,
                endDate,
                issuerAndSubject,
                keyPair.getPublic()
        );

        // Sign the certificate using SHA256 with RSA
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);

        // Convert to standard java.security.cert.X509Certificate
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
}