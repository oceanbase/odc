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
package com.oceanbase.odc.service.connection.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/12/6 下午8:57
 * @Description: []
 */
@Slf4j
@Component
public class DefaultCertificateStorage implements CertificateStorage {
    @Value("${file.storage.dir:./data}/SSL")
    private String sslWorkDirectory;
    private static final char[] DEFAULT_PASSWORD = "".toCharArray();
    public static final String JKS_EXTENSION = ".jks";
    private static final String X509 = "X.509";

    public static final String CA_CERT_ALIAS = "ca-cert";
    public static final String CLIENT_CERT_ALIAS = "client-cert";
    public static final String CLIENT_KEY_ALIAS = "key-cert";

    @PostConstruct
    public void init() {
        try {
            FileUtils.forceMkdir(new File(sslWorkDirectory));
        } catch (IOException ex) {
            log.error("create ssl work directory failed, directory path={}, ex=", sslWorkDirectory, ex);
            throw new RuntimeException("create ssl work directory failed");
        }
        /**
         * register BouncyCastleProvider when setting up <br>
         */
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public void addCertificate(String name, InputStream caCertStream, InputStream clientCertStream,
            InputStream clientKeyStream) {
        if (caCertStream == null && clientCertStream == null && clientKeyStream == null) {
            return;
        }
        KeyStore keyStore = getKeyStore(name);
        try {
            CertificateFactory cf = CertificateFactory.getInstance(X509);
            List<Certificate> certChain = new ArrayList<>();
            if (caCertStream != null) {
                Certificate caCert = cf.generateCertificate(caCertStream);
                keyStore.setCertificateEntry(CA_CERT_ALIAS, caCert);
            }
            if (clientCertStream != null) {
                Certificate clientCert = cf.generateCertificate(clientCertStream);
                keyStore.setCertificateEntry(CLIENT_CERT_ALIAS, clientCert);
                certChain.add(clientCert);
            }
            if (clientKeyStream != null) {
                PEMParser pemParser = new PEMParser(new InputStreamReader(clientKeyStream));
                PemObject pemObject = pemParser.readPemObject();
                KeyFactory factory = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(pemObject.getContent()));
                keyStore.setKeyEntry(CLIENT_KEY_ALIAS, privateKey, DEFAULT_PASSWORD,
                        certChain.toArray(new Certificate[certChain.size()]));
            }

            saveKeyStore(keyStore, name);
        } catch (Exception e) {
            log.error("add certificate to KeyStore failed, name={}, ex=", name, e);
            throw new UnexpectedException("invalid .pem file");
        }
    }

    @Override
    public void deleteCertificate(String name) {
        final KeyStore keyStore = getKeyStore(name);
        try {
            keyStore.deleteEntry(CA_CERT_ALIAS);
            keyStore.deleteEntry(CLIENT_CERT_ALIAS);
            keyStore.deleteEntry(CLIENT_KEY_ALIAS);
            saveKeyStore(keyStore, name);
        } catch (Exception e) {
            log.error("delete certificate from KeyStore failed, name={}, ex=", name, e);
            throw new UnexpectedException("delete certificate from KeyStore");
        }
    }

    @Override
    public KeyStore getKeyStore(String name) {
        try {
            File ksFile = getKeyStoreFile(name);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if (ksFile.exists()) {
                try (InputStream is = new FileInputStream(ksFile)) {
                    ks.load(is, getKeyStorePassword(name));
                }
            } else {
                ks.load(null, DEFAULT_PASSWORD);
                saveKeyStore(ks, name);
            }

            return ks;
        } catch (Exception e) {
            log.error("obtain KeyStore failed, name={}, ex=", name, e);
            throw new UnexpectedException("obtain KeyStore failed");
        }
    }

    @Override
    public File getKeyStoreFile(String name) {
        return new File(sslWorkDirectory, name + JKS_EXTENSION);
    }

    @Override
    public char[] getKeyStorePassword(String name) {
        return DEFAULT_PASSWORD;
    }

    protected void saveKeyStore(KeyStore keyStore, String name) throws Exception {
        final File ksFile = getKeyStoreFile(name);
        try (OutputStream os = new FileOutputStream(ksFile)) {
            keyStore.store(os, DEFAULT_PASSWORD);
        }
    }
}
