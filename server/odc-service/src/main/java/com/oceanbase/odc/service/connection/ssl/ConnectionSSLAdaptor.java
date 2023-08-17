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
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.model.ConnectionConfig.SSLConfig;
import com.oceanbase.odc.service.connection.model.ConnectionConfig.SSLFileEntry;
import com.oceanbase.odc.service.connection.model.SSLConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/12/2 下午4:02
 * @Description: []
 */
@Component
@Slf4j
public class ConnectionSSLAdaptor {


    @Autowired
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private CertificateStorage certificateStorage;

    public <T extends SSLConnectionConfig> T adapt(@NonNull T connection) {
        SSLConfig sslConfig = connection.getSslConfig();
        if (Objects.isNull(sslConfig) || !sslConfig.getEnabled()) {
            return connection;
        }
        SSLStorageObjects storageObjects = new SSLStorageObjects();
        try {
            if (StringUtils.isNotBlank(sslConfig.getClientCertObjectId())) {
                storageObjects
                        .setClientCert(objectStorageFacade.loadObject(sslBucket(), sslConfig.getClientCertObjectId()));
            }
            if (StringUtils.isNotBlank(sslConfig.getClientKeyObjectId())) {
                storageObjects
                        .setClientKey(objectStorageFacade.loadObject(sslBucket(), sslConfig.getClientKeyObjectId()));
            }
            if (StringUtils.isNotBlank(sslConfig.getCACertObjectId())) {
                storageObjects.setCaCert(objectStorageFacade.loadObject(sslBucket(), sslConfig.getCACertObjectId()));
            }
        } catch (IOException ex) {
            log.warn("load ssl config file failed, ex=", ex);
            throw new UnexpectedException("load ssl config file failed");
        }

        String certificateName = UUID.randomUUID().toString();
        certificateStorage.addCertificate(certificateName,
                storageObjects.getCaCert() == null ? null : storageObjects.getCaCert().getContent(),
                storageObjects.getClientCert() == null ? null : storageObjects.getClientCert().getContent(),
                storageObjects.getClientKey() == null ? null : storageObjects.getClientKey().getContent());
        if (!certificateStorage.getKeyStoreFile(certificateName).exists()) {
            return connection;
        }
        connection.setSslFileEntry(SSLFileEntry.builder()
                .keyStoreFilePath(certificateStorage.getKeyStoreFile(certificateName).getAbsolutePath())
                .keyStoreFilePassword(String.valueOf(certificateStorage.getKeyStorePassword(certificateName)))
                .build());
        return connection;
    }

    @Data
    private static class SSLStorageObjects {
        private StorageObject clientCert;
        private StorageObject clientKey;
        private StorageObject caCert;
    }


    private String sslBucket() {
        return "ssl".concat(File.separator).concat(authenticationFacade.currentUserIdStr());
    }
}
