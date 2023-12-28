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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.unit.BinarySize;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.GetObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration.CloudProvider;
import com.oceanbase.odc.service.objectstorage.cloud.model.PartETag;
import com.oceanbase.odc.service.objectstorage.cloud.model.PutObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartResult;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@SkipAuthorize
@RefreshScope
public class CloudObjectStorageService {
    /**
     * 校验用户传入的 objectName 是否合法
     */
    private static final Pattern IS_OBJECT_NAME_LEGAL = Pattern.compile("([a-zA-Z_0-9]/)*");
    public static final int MIN_PRESIGNED_URL_EXPIRATION_SECONDS = 5 * 60;
    public static final int PRESIGNED_UPLOAD_URL_EXPIRATION_SECONDS = 30 * 60;
    private final File tempDirectory = new File(
            CloudObjectStorageConstants.TEMP_DIR);

    private CloudObjectStorage publicEndpointCloudObjectStorage;
    private CloudObjectStorage internalEndpointCloudObjectStorage;
    private ObjectStorageConfiguration objectStorageConfiguration;

    public CloudObjectStorageService(
            @Autowired @Qualifier("publicEndpointCloudClient") CloudObjectStorage publicEndpointCloudObjectStorage,
            @Autowired @Qualifier("internalEndpointCloudClient") CloudObjectStorage internalEndpointCloudObjectStorage,
            CloudEnvConfigurations cloudEnvConfigurations) {
        this.publicEndpointCloudObjectStorage = publicEndpointCloudObjectStorage;
        this.internalEndpointCloudObjectStorage = internalEndpointCloudObjectStorage;
        this.objectStorageConfiguration = cloudEnvConfigurations.getObjectStorageConfiguration();
        if (this.publicEndpointCloudObjectStorage.supported()) {
            validateBucket();
            createTempDirectory();
            log.info("Cloud object storage initialized");
        } else {
            log.info("Cloud object storage not supported");
        }
    }

    public boolean supported() {
        return publicEndpointCloudObjectStorage.supported();
    }

    public String getBucketName() {
        verifySupported();
        return objectStorageConfiguration.getBucketName();
    }

    public String upload(@NotBlank String fileName, @NonNull InputStream input) throws IOException {
        verifySupported();
        String objectName = generateObjectName(fileName);
        upload(objectName, input, null);
        return objectName;
    }

    public String upload(@NotBlank String fileName, @NonNull File file) throws IOException {
        verifySupported();
        String objectName = generateObjectName(fileName);
        upload(objectName, file, null);
        return objectName;
    }

    public String uploadTemp(@NotBlank String fileName, @NonNull InputStream input) throws IOException {
        verifySupported();
        String objectName = generateObjectName(fileName);
        upload(objectName, input, ObjectMetadata.temp());
        return objectName;
    }

    public String uploadTemp(@NotBlank String fileName, @NonNull File file) throws IOException {
        verifySupported();
        String objectName = generateObjectName(fileName);
        upload(objectName, file, ObjectMetadata.temp());
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
        verifySupported();
        String objectName = generateObjectName(prefix, fileName);
        upload(objectName, input, null);
        return objectName;
    }

    public String upload(@NotBlank String prefix, @NotBlank String fileName, @NonNull File file) throws IOException {
        verifySupported();
        String objectName = generateObjectName(prefix, fileName);
        upload(objectName, file, null);
        return objectName;
    }

    public URL generateDownloadUrl(@NotBlank String objectName) throws IOException {
        return generateDownloadUrl(objectName, null);
    }

    public URL generateDownloadUrl(@NotBlank String objectName, Long expirationSeconds) throws IOException {
        verifySupported();
        ObjectMetadata objectMetadata = publicEndpointCloudObjectStorage.getObjectMetadata(getBucketName(), objectName);
        Date expirationTime = calcExpirationTime(expirationSeconds, objectMetadata.getContentLength());
        URL presignedUrl =
                publicEndpointCloudObjectStorage.generatePresignedUrl(getBucketName(), objectName, expirationTime);
        log.info("generate temporary download Url successfully, expirationTime={}, objectName={}, presignedUrl={}",
                expirationTime, objectName, presignedUrl);
        return presignedUrl;
    }

    public URL generateUploadUrl(@NotBlank String objectName) {
        verifySupported();
        Date expirationTime = new Date(System.currentTimeMillis() + PRESIGNED_UPLOAD_URL_EXPIRATION_SECONDS * 1000);
        URL presignedUrl =
                publicEndpointCloudObjectStorage.generatePresignedPutUrl(getBucketName(), objectName, expirationTime);
        log.info("generate temporary upload Url successfully, expirationTime={}, objectName={}, presignedUrl={}",
                expirationTime, objectName, presignedUrl);
        return presignedUrl;
    }

    /**
     * 读取目标文件内容
     *
     * @param objectName 目标文件名
     * @return 字符数组格式的文件内容
     * @throws IOException 目标对象不存在或读取内容失败
     */
    public byte[] readContent(@NotBlank String objectName) throws IOException {
        boolean exist = internalEndpointCloudObjectStorage.doesObjectExist(getBucketName(), objectName);
        if (!exist) {
            throw new FileNotFoundException("File dose not exist, object name " + objectName);
        }
        try (InputStream inputStream =
                internalEndpointCloudObjectStorage.getObject(getBucketName(), objectName).getObjectContent()) {
            return StreamUtils.copyToByteArray(inputStream);
        } catch (Exception exception) {
            log.warn("Read content failed, objectName={}", objectName, exception);
            throw new IOException(exception);
        }
    }

    /**
     * delete single object by objectName
     *
     * @param objectName object name
     * @return true if delete success, false if delete failed
     * @throws IOException
     */
    public boolean delete(@NotBlank String objectName) throws IOException {
        verifySupported();
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
        verifySupported();
        DeleteObjectsRequest request = new DeleteObjectsRequest();
        request.setBucketName(getBucketName());
        request.setKeys(objectNames);
        DeleteObjectsResult result = internalEndpointCloudObjectStorage.deleteObjects(request);
        List<String> deletedObjects = result.getDeletedObjects();
        log.info("Delete files success, tryDeleteObjectName={}, deletedObjectNames={}",
                objectNames, deletedObjects);
        return deletedObjects;
    }

    /**
     * download to temp file, locate at temp directly use UUID as local file name
     *
     * @param objectName objectName
     * @return downloaded file
     */
    public File downloadToTempFile(@NotBlank String objectName) {
        verifySupported();
        String absolute = tempDirectory.getAbsolutePath() + "/" + UUID.randomUUID() + ".tmp";
        File targetFile = new File(absolute);
        GetObjectRequest request = new GetObjectRequest(getBucketName(), objectName);
        internalEndpointCloudObjectStorage.getObject(request, targetFile);
        return targetFile;
    }

    ObjectStorageConfiguration getObjectStorageConfiguration() {
        return objectStorageConfiguration;
    }

    private void upload(@NotBlank String objectName, @NonNull InputStream input, ObjectMetadata metadata)
            throws IOException {
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
            upload(objectName, file, metadata);
        } finally {
            FileUtils.forceDelete(file);
        }
    }

    /**
     * 文件上传方法，为了保证性能，如果文件大小小于10MB使用简单上传，如果文件大小大于10MB则使用分片上传功能
     *
     * @param objectName objectName, 名称会被进行合法性校验
     * @param file 要上传的文件
     */
    private void upload(@NotBlank String objectName, @NonNull File file, ObjectMetadata metadata) throws IOException {
        try {
            innerUpload(objectName, file, metadata);
        } catch (Exception exception) {
            log.warn("Failed to upload file,  objectName={}, filePath={}",
                    objectName, file.getAbsolutePath(), exception);
            throw new IOException(exception);
        }
    }

    private void innerUpload(@NotBlank String objectName, @NonNull File file, ObjectMetadata metadata)
            throws IOException {
        if (!file.exists()) {
            log.warn("Upload temp file does not exist, filePath={}", file.getAbsolutePath());
            throw new FileNotFoundException(file.getName());
        }
        if (Objects.isNull(metadata)) {
            metadata = new ObjectMetadata();
        }
        validateFileName(objectName);
        long startTime = System.currentTimeMillis();
        BinarySize fileSize = BinarySizeUnit.B.of(file.length());
        BinarySize criticalSize = BinarySizeUnit.MB.of(CloudObjectStorageConstants.CRITICAL_FILE_SIZE_IN_MB);
        if (fileSize.compareTo(criticalSize) < 0) {
            log.debug("Use putObject method to upload, fileSize={}", fileSize);
            PutObjectResult result =
                    internalEndpointCloudObjectStorage.putObject(getBucketName(), objectName, file, metadata);
            log.info("Simple upload process is completed, fileSize={}, durationMS={} ms, result={}", fileSize,
                    System.currentTimeMillis() - startTime, result);
        } else {
            log.debug("Use multipartUpload to upload, fileSize={}", fileSize);
            if (Objects.isNull(metadata.getContentType())) {
                metadata.setContentType("application/octet-stream");
            }
            CompleteMultipartUploadResult result = multiPartUpload(objectName, file, metadata);
            log.info("Multipart upload process is completed, fileSize={}, duration={} ms, result={}", fileSize,
                    System.currentTimeMillis() - startTime, result);
        }
    }

    /**
     * refer from https://help.aliyun.com/document_detail/84786.html
     */
    private CompleteMultipartUploadResult multiPartUpload(@NotBlank String objectName, @NonNull File file,
            ObjectMetadata metadata) throws IOException {
        long fileLength = file.length();
        long partSize = calculatePartSize(fileLength);
        String bucketName = getBucketName();
        InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                new InitiateMultipartUploadRequest(bucketName, objectName, metadata);
        InitiateMultipartUploadResult initiateMultipartUploadResult =
                internalEndpointCloudObjectStorage.initiateMultipartUpload(initiateMultipartUploadRequest);
        String uploadId = initiateMultipartUploadResult.getUploadId();
        List<PartETag> partTags = new ArrayList<>();
        int partCount = (int) (fileLength / partSize);
        if (fileLength % partSize != 0) {
            partCount++;
        }
        for (int i = 0; i < partCount; i++) {
            long startPos = i * partSize;
            long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
            try (InputStream input = Files.newInputStream(file.toPath())) {
                long skip = input.skip(startPos);
                Verify.equals(startPos, skip, "skipped size");
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(objectName);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(input);
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber(i + 1);
                UploadPartResult uploadPartResult = internalEndpointCloudObjectStorage.uploadPart(uploadPartRequest);
                partTags.add(uploadPartResult.getPartETag());
            }
        }
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, partTags);
        CompleteMultipartUploadResult completeMultipartUploadResult =
                internalEndpointCloudObjectStorage.completeMultipartUpload(completeMultipartUploadRequest);
        log.info("Complete multipart upload, result={}", completeMultipartUploadResult);
        return completeMultipartUploadResult;
    }

    public long calculatePartSize(long fileLength) {
        long partSize = fileLength / CloudObjectStorageConstants.MAX_PART_COUNT;
        if (fileLength % CloudObjectStorageConstants.MAX_PART_COUNT != 0) {
            partSize += 1;
        }
        if (partSize < CloudObjectStorageConstants.MIN_PART_SIZE) {
            partSize = CloudObjectStorageConstants.MIN_PART_SIZE;
        }
        return partSize;
    }

    /**
     * 验证bucket操作是否合法，目前的访问模型中只允许访问本region的bucket， <br>
     * 也就是杭州的client只允许操作杭州的bucket，不允许跨域操作
     */
    private void validateBucket() {
        if (objectStorageConfiguration.getCloudProvider() == CloudProvider.NONE) {
            return;
        }
        String bucketName = getBucketName();
        boolean isExist = publicEndpointCloudObjectStorage.doesBucketExist(bucketName);
        Verify.verify(isExist, String.format("object storage bucket '%s' not exists", bucketName));

        String region = objectStorageConfiguration.getRegion();
        if (StringUtils.isNotEmpty(region)) {
            String location = publicEndpointCloudObjectStorage.getBucketLocation(bucketName);
            Verify.verify(StringUtils.equals(region, location) || StringUtils.endsWith(location, region),
                    "object storage bucket region does not match location, location=" + location + ", region="
                            + region);
        }
    }

    private void createTempDirectory() {
        if (!tempDirectory.exists() && !tempDirectory.mkdirs()) {
            throw new RuntimeException("Fail to create dir " + tempDirectory.getAbsolutePath());
        }
    }

    private void validateFileName(String objectName) {
        Matcher matcher = IS_OBJECT_NAME_LEGAL.matcher(objectName);
        if (!matcher.find()) {
            log.warn("Illegal filename detected, objectName={}", objectName);
            throw new IllegalArgumentException("Illegal filename, objectName=" + objectName);
        }
    }

    public String generateObjectName(@NotBlank String fileName) {
        return generateObjectName(CloudObjectStorageConstants.ODC_SERVER_PREFIX, fileName);
    }

    private String generateObjectName(String prefix, String fileName) {
        return CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(), prefix, fileName);
    }

    private Date calcExpirationTime(Long expirationSeconds, long contentSize) {
        if (expirationSeconds == null) {
            long time = contentSize / (1024 * 1024) + MIN_PRESIGNED_URL_EXPIRATION_SECONDS;
            return new Date(System.currentTimeMillis() + time * 1000);
        } else {
            return new Date(System.currentTimeMillis() + expirationSeconds * 1000);
        }
    }

    private void verifySupported() {
        Verify.verify(supported(), "Cloud object storage not supported");
    }
}
