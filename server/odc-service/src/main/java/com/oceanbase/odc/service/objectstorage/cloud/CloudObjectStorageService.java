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
package com.oceanbase.odc.service.objectstorage.cloud;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;
import com.oceanbase.odc.service.objectstorage.lifecycle.Lifecycle;
import com.oceanbase.odc.service.objectstorage.pure.CloudPureObjectStorage;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@SkipAuthorize
@RefreshScope
public class CloudObjectStorageService {
    private final File tempDirectory = new File(
            CloudObjectStorageConstants.TEMP_DIR);
    private final CloudPureObjectStorage pureObjectStorage;

    @Autowired
    public CloudObjectStorageService(
            @Autowired @Qualifier("publicEndpointCloudClient") CloudObjectStorage publicEndpointCloudObjectStorage,
            @Autowired @Qualifier("internalEndpointCloudClient") CloudObjectStorage internalEndpointCloudObjectStorage,
            CloudEnvConfigurations cloudEnvConfigurations) {

        this(publicEndpointCloudObjectStorage, internalEndpointCloudObjectStorage,
                cloudEnvConfigurations.getObjectStorageConfiguration());
    }

    public CloudObjectStorageService(CloudObjectStorage publicEndpointCloudObjectStorage,
            CloudObjectStorage internalEndpointCloudObjectStorage,
            ObjectStorageConfiguration objectStorageConfiguration) {
        pureObjectStorage = new CloudPureObjectStorage(publicEndpointCloudObjectStorage,
                internalEndpointCloudObjectStorage, objectStorageConfiguration);
        if (this.pureObjectStorage.supported()) {
            createTempDirectory();
            log.info("Cloud object storage initialized");
        } else {
            log.info("Cloud object storage not supported");
        }
    }

    public boolean supported() {
        return pureObjectStorage.supported();
    }

    public String getBucketName() {
        return pureObjectStorage.getBucketName();
    }

    public String upload(@NotBlank String fileName, @NonNull InputStream input) throws IOException {
        String objectName = generateObjectName(fileName);
        upload(objectName, input, null);
        return objectName;
    }

    public String upload(@NotBlank String fileName, @NonNull File file) throws IOException {
        String objectName = generateObjectName(fileName);
        pureObjectStorage.putObject(objectName, file, null);
        return objectName;
    }

    public String uploadTemp(@NotBlank String fileName, @NonNull InputStream input) throws IOException {
        String objectName = generateObjectName(fileName);
        upload(objectName, input, Lifecycle.temp());
        return objectName;
    }

    public String uploadTemp(@NotBlank String fileName, @NonNull File file) throws IOException {
        String objectName = generateObjectName(fileName);
        pureObjectStorage.putObject(objectName, file, Lifecycle.temp());
        return objectName;
    }

    /**
     * upload input stream with prefix and fileName
     *
     * @param prefix ODC level directory, for identify ODC module
     * @param fileName file name
     * @param input InputStream
     * @return objectName
     * @throws IOException
     */
    public String upload(@NotBlank String prefix, @NotBlank String fileName, @NonNull InputStream input)
            throws IOException {
        String objectName = generateObjectName(prefix, fileName);
        upload(objectName, input, null);
        return objectName;
    }

    public String upload(@NotBlank String prefix, @NotBlank String fileName, @NonNull File file) throws IOException {
        String objectName = generateObjectName(prefix, fileName);
        pureObjectStorage.putObject(objectName, file, null);
        return objectName;
    }

    public URL generateDownloadUrl(@NotBlank String objectName) throws IOException {
        return generateDownloadUrl(objectName, null);
    }

    public URL generateDownloadUrl(@NotBlank String objectName, Long expirationSeconds) throws IOException {
        return pureObjectStorage.getDownloadUrl(objectName, expirationSeconds);
    }

    public URL generateUploadUrl(@NotBlank String objectName) {
        return pureObjectStorage.getUploadUrl(objectName);
    }

    /**
     * 读取目标文件内容
     *
     * @param objectName 目标文件名
     * @return 字符数组格式的文件内容
     * @throws IOException 目标对象不存在或读取内容失败
     */
    public byte[] readContent(@NotBlank String objectName) throws IOException {
        return pureObjectStorage.readContent(objectName);
    }

    /**
     * delete single object by objectName
     *
     * @param objectName object name
     * @return true if delete success, false if delete failed
     * @throws IOException
     */
    public boolean delete(@NotBlank String objectName) throws IOException {
        List<String> deletedObjectNames = delete(Collections.singletonList(objectName));
        return !deletedObjectNames.isEmpty();
    }

    /**
     * batch delete objects by objectNames
     *
     * @param objectNames
     * @return deleted object names
     * @throws IOException
     */
    public List<String> delete(@NotEmpty List<String> objectNames) throws IOException {
        return pureObjectStorage.deleteObjects(objectNames);
    }

    /**
     * download to temp file, locate at temp directly use UUID as local file name
     *
     * @param objectName objectName
     * @return downloaded file
     */
    public File downloadToTempFile(@NotBlank String objectName) {
        String absolute = tempDirectory.getAbsolutePath() + "/" + UUID.randomUUID() + ".tmp";
        File targetFile = new File(absolute);
        try {
            pureObjectStorage.downloadToFile(objectName, targetFile);
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException("download to temp file failed,objectName=" + objectName, e);
        }
    }

    ObjectStorageConfiguration getObjectStorageConfiguration() {
        return pureObjectStorage.getObjectStorageConfiguration();
    }

    private void upload(@NotBlank String objectName, @NonNull InputStream input, Lifecycle metadata)
            throws IOException {
        pureObjectStorage.verifySupported();
        String tempFilePath = tempDirectory.getAbsolutePath() + "/" + UUID.randomUUID() + ".tmp";
        FileOutputStream output = new FileOutputStream(tempFilePath);
        try {
            IOUtils.copy(input, output);
        } finally {
            input.close();
            output.close();
        }
        File file = new File(tempFilePath);
        try {
            pureObjectStorage.putObject(objectName, file, metadata);
        } finally {
            FileUtils.forceDelete(file);
        }
    }

    private void createTempDirectory() {
        if (!tempDirectory.exists() && !tempDirectory.mkdirs()) {
            throw new RuntimeException("Fail to create dir " + tempDirectory.getAbsolutePath());
        }
    }

    public String generateObjectName(@NotBlank String fileName) {
        return generateObjectName(CloudObjectStorageConstants.ODC_SERVER_PREFIX, fileName);
    }

    private String generateObjectName(String prefix, String fileName) {
        return CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(), prefix, fileName);
    }
}
