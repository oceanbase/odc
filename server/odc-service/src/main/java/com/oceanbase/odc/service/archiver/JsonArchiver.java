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
package com.oceanbase.odc.service.archiver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Properties;

import javax.annotation.Nullable;

import org.springframework.boot.info.BuildProperties;

import com.google.common.base.Verify;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.EncryptAlgorithm;
import com.oceanbase.odc.service.archiver.streamprovider.ByteArrayStreamProvider;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonArchiver implements Archiver {

    CloudObjectStorageService cloudObjectStorageService;

    /**
     * TODO adds a way to stream archiving
     * 
     * @param data Full data
     * @param metaData metadata
     * @return
     * @throws Exception
     */
    public ArchivedFile archiveFullDataToLocal(Object data, @Nullable Properties metaData, @Nullable String encryptKey)
            throws Exception {
        if (metaData == null) {
            metaData = new Properties();
        }
        addDefaultMetaData(metaData);

        ArchivedData<Object> archivedData = new ArchivedData<>();
        archivedData.setMetadata(metaData);
        archivedData.setData(data);
        String json = JsonUtils.toJson(archivedData);
        if (encryptKey != null) {
            json = EncryptAlgorithm.AES.encrypt(json, encryptKey, StandardCharsets.UTF_8.name());
        }
        String url = writeToFile(metaData, json);
        return new ArchivedFile(url, metaData.getProperty(ArchiveConstants.FILE_NAME), encryptKey,
                new ByteArrayStreamProvider(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public ArchivedFile archiveFullDataToCloudObject(Object data, @Nullable Properties metaData, String encryptKey)
            throws Exception {
        Verify.verifyNotNull(cloudObjectStorageService, "cloudObjectStorageService");
        Verify.verify(cloudObjectStorageService.supported(), "Not supported cloud object storage service");
        ArchivedFile archivedFile = archiveFullDataToLocal(data, metaData, encryptKey);
        String objectKey = cloudObjectStorageService.uploadTemp(archivedFile.getFileName(),
                archivedFile.getProvider().getInputStream());
        archivedFile.setUri(cloudObjectStorageService.generateDownloadUrl(objectKey).toString());
        return archivedFile;
    }

    public ArchivedFile archiveFullDataToLocal(Object data, @Nullable Properties metaData)
            throws Exception {
        return archiveFullDataToLocal(data, metaData, null);
    }

    @Override
    public ArchivedFile archiveFullDataToCloudObject(Object data, @Nullable Properties metaData) throws Exception {
        return archiveFullDataToCloudObject(data, metaData, null);
    }

    private String writeToFile(Properties metaData, String encrypted) throws IOException {
        String filePath = metaData.getProperty(ArchiveConstants.FILE_PATH);
        String fileName = metaData.getProperty(ArchiveConstants.FILE_NAME);
        String uri = filePath + File.pathSeparator + fileName;
        Verify.verify(checkFileSuffix(fileName, ArchiveConstants.FILE_TXT_SUFFER), "FileName should end with .txt");
        try (FileWriter fileWriter = new FileWriter(uri)) {
            fileWriter.write(encrypted);
            log.info("Archived file: {}", uri);
        } catch (IOException e) {
            log.error("Failed to write json file", e);
            throw e;
        }
        return uri;
    }


    public boolean checkFileSuffix(String filePath, String suffix) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        return filePath.toLowerCase().endsWith(suffix);
    }

    private void addDefaultMetaData(Properties metaData) {
        Verify.verifyNotNull(metaData);
        if (metaData.getProperty(ArchiveConstants.ODC_VERSION) == null) {
            try {
                BuildProperties buildProperties = SpringContextUtil.getBean(BuildProperties.class);
                String version = buildProperties.getVersion();
                metaData.setProperty(ArchiveConstants.ODC_VERSION, version);
            } catch (Exception e) {
                log.warn("Failed to load build properties", e);
            }
        }
        metaData.putIfAbsent(ArchiveConstants.CREATE_TIME, LocalDateTime.now());
    }

}
