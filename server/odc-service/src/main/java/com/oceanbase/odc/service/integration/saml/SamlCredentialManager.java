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
package com.oceanbase.odc.service.integration.saml;

import static com.oceanbase.odc.service.integration.saml.SamlRegistrationConfigHelper.removeBase64CertificatePem;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.service.integration.util.EncryptionUtil;

import lombok.SneakyThrows;

@Component
public class SamlCredentialManager {

    public final Cache<String, String> certPrivateKeyCache =
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(5, TimeUnit.MINUTES).build();

    @SneakyThrows
    public String generateCertWithCachedPrivateKey() {
        Pair<PrivateKey, X509Certificate> pair = EncryptionUtil.generateKeyPair();
        String privateKeyPem = EncryptionUtil.convertPrivateKeyToPem(pair.left);
        String certificate = EncryptionUtil.convertCertificateToPem(pair.right);
        certPrivateKeyCache.put(removeBase64CertificatePem(certificate), privateKeyPem);
        return certificate;
    }

    public String getPrivateKeyByCert(String certificate) {
        return certPrivateKeyCache.get(removeBase64CertificatePem(certificate), (key) -> {
            throw new RuntimeException("Certificate expired");
        });
    }

}

