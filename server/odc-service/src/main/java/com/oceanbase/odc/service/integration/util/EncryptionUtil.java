/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.integration.util;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.util.EncodeUtils;
import com.oceanbase.odc.service.integration.model.Encryption;

import lombok.SneakyThrows;

/**
 * Encryption utility for integration usage.
 * 
 * @author gaoda.xy
 * @date 2023/4/3 17:44
 */
public class EncryptionUtil {

    public static final String PRIVATE_KEY_PREFIX = "-----BEGIN PRIVATE KEY-----";
    public static final String PRIVATE_KEY_SUFFIX = "-----END PRIVATE KEY-----";
    public static final String CERTIFICATE_KEY_PREFIX = "-----BEGIN CERTIFICATE-----";
    public static final String CERTIFICATE_KEY_SUFFIX = "-----END CERTIFICATE-----";
    private static final LoadingCache<Encryption, TextEncryptor> encryptorCache = Caffeine.newBuilder().maximumSize(100)
            .expireAfterAccess(10, TimeUnit.MINUTES).build(EncryptionUtil::getEncryptor);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static String encrypt(String plainText, Encryption encryption) {
        return Objects.requireNonNull(encryptorCache.get(encryption)).encrypt(plainText);
    }

    public static String decrypt(String encryptedText, Encryption encryption) {
        return Objects.requireNonNull(encryptorCache.get(encryption)).decrypt(encryptedText);
    }

    private static TextEncryptor getEncryptor(Encryption encryption) {
        if (!encryption.getEnabled()) {
            return Encryptors.empty();
        }
        switch (encryption.getAlgorithm()) {
            case AES256_BASE64:
                return Encryptors.aes256Base64(encryption.getSecret());
            case AES192_BASE64_4A:
                return Encryptors.aesBase64Cmcc4A(encryption.getSecret());
            case RAW:
            default:
                return Encryptors.empty();
        }
    }

    @SneakyThrows
    public static Pair<PrivateKey, X509Certificate> generateKeyPair() {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        JcaX509v3CertificateBuilder certBuilder = getJcaX509v3CertificateBuilder(keyPair);
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
        KeyUsage keyUsage = getKeyAllUsage();
        certBuilder.addExtension(Extension.keyUsage, true, keyUsage);

        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(contentSigner));

        return new Pair<>(privateKey, certificate);
    }

    private static KeyUsage getKeyAllUsage() {
        int keyUsageFlags = KeyUsage.digitalSignature
                | KeyUsage.nonRepudiation
                | KeyUsage.keyEncipherment
                | KeyUsage.dataEncipherment
                | KeyUsage.keyAgreement
                | KeyUsage.keyCertSign
                | KeyUsage.cRLSign
                | KeyUsage.encipherOnly
                | KeyUsage.decipherOnly;
        return new KeyUsage(keyUsageFlags);
    }

    private static JcaX509v3CertificateBuilder getJcaX509v3CertificateBuilder(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();

        X500Name issuerName = new X500Name("CN=Test,  O=ODC, L=Hangzhou,  ST=Hangzhou,  C=CN");
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + (365L * 24 * 60 * 60 * 1000));

        return new JcaX509v3CertificateBuilder(
                issuerName, serialNumber, startDate, endDate, issuerName,
                publicKey);
    }

    public static String convertPrivateKeyToPem(PrivateKey privateKey) {
        byte[] encodedPrivateKey = privateKey.getEncoded();
        String base64EncodedKey = EncodeUtils.base64EncodeToString(encodedPrivateKey);
        return PRIVATE_KEY_PREFIX + "\n"
                + base64EncodedKey
                + "\n" + PRIVATE_KEY_SUFFIX;
    }

    public static String convertCertificateToPem(X509Certificate certificate) throws CertificateEncodingException {
        byte[] encodedCertificate = certificate.getEncoded();
        String base64EncodedCert = EncodeUtils.base64EncodeToString(encodedCertificate);
        return CERTIFICATE_KEY_PREFIX + "\n"
                + base64EncodedCert
                + "\n" + CERTIFICATE_KEY_SUFFIX;
    }
}
