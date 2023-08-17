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
package com.oceanbase.odc.service.connection;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStoreException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.ssl.CertificateStorage;

public class DefaultCertificateStorageTest extends ServiceTestEnv {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private CertificateStorage certificateStorage;

    private static final String KEYSTORE_NAME = "test";

    @Test
    public void test_AddCertificate_Success() throws FileNotFoundException {
        File caCert = getCACertFile();
        InputStream caCertIs = new FileInputStream(caCert);
        certificateStorage.addCertificate(KEYSTORE_NAME, caCertIs, null, null);

        Assert.assertTrue(certificateStorage.getKeyStoreFile(KEYSTORE_NAME).exists());
    }

    @Test
    public void test_AddCertificate_IllegalFile() {
        InputStream illegalCAFile =
                new ByteArrayInputStream("-----BEGIN CERTIFICATE-----\n fake \n-----END CERTIFICATE-----".getBytes());

        thrown.expect(UnexpectedException.class);
        certificateStorage.addCertificate(KEYSTORE_NAME, illegalCAFile, null, null);
    }


    @Test
    public void test_deleteCertificate_EmptyKeyEntry() throws FileNotFoundException, KeyStoreException {
        File caCert = getCACertFile();
        InputStream caCertIs = new FileInputStream(caCert);
        certificateStorage.addCertificate(KEYSTORE_NAME, caCertIs, null, null);
        certificateStorage.deleteCertificate(KEYSTORE_NAME);

        Assert.assertEquals(0, certificateStorage.getKeyStore(KEYSTORE_NAME).size());
    }

    @Test
    public void test_getKeyStore_Success() {
        Assert.assertTrue(certificateStorage.getKeyStore(KEYSTORE_NAME).getProvider() != null);
    }


    @Test
    public void test_getKeyStorePassword_Success() {
        Assert.assertEquals("", String.valueOf(certificateStorage.getKeyStorePassword(KEYSTORE_NAME)));

    }

    private File getCACertFile() throws FileNotFoundException {
        return new File(ResourceUtils.getURL("classpath:").getPath() + "/ssl/ca.pem");
    }
}
