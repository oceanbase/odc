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
package com.oceanbase.odc.service.objectstorage.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotBlank;

import org.springframework.util.StreamUtils;

import com.oceanbase.odc.common.unit.BinarySize;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorage;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.GetObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectTagging;
import com.oceanbase.odc.service.objectstorage.cloud.model.PartETag;
import com.oceanbase.odc.service.objectstorage.cloud.model.PutObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartResult;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author keyang
 * @date 2024/08/09
 * @since 4.3.2
 */
@Slf4j
public class CloudObjectStorageClient implements ObjectStorageClient {
    private static final Pattern IS_OBJECT_NAME_LEGAL = Pattern.compile("([a-zA-Z_0-9]/)*");
    public static final int MIN_PRESIGNED_URL_EXPIRATION_SECONDS = 5 * 60;
    public static final int PRESIGNED_UPLOAD_URL_EXPIRATION_SECONDS = 30 * 60;

    private final CloudObjectStorage publicEndpointCloudObjectStorage;
    private final CloudObjectStorage internalEndpointCloudObjectStorage;
    @Getter
    private final ObjectStorageConfiguration objectStorageConfiguration;

    public CloudObjectStorageClient(CloudObjectStorage publicEndpointCloudObjectStorage,
            CloudObjectStorage internalEndpointCloudObjectStorage,
            ObjectStorageConfiguration objectStorageConfiguration) {
        this.publicEndpointCloudObjectStorage = publicEndpointCloudObjectStorage;
        this.internalEndpointCloudObjectStorage = internalEndpointCloudObjectStorage;
        this.objectStorageConfiguration = objectStorageConfiguration;
        if (this.publicEndpointCloudObjectStorage.supported()) {
            validateBucket();
            log.info("Cloud object storage initialized");
        } else {
            log.info("Cloud object storage not supported");
        }
    }

    @Override
    public URL generateDownloadUrl(String objectName, Long expirationSeconds, String customFileName) {
        verifySupported();
        ObjectMetadata objectMetadata = publicEndpointCloudObjectStorage.getObjectMetadata(getBucketName(), objectName);
        Date expirationTime = calcExpirationTime(expirationSeconds, objectMetadata.getContentLength());
        URL presignedUrl =
                publicEndpointCloudObjectStorage.generatePresignedUrlWithCustomFileName(getBucketName(), objectName,
                        expirationTime, customFileName);
        log.info(
                "generate temporary download Url successfully, expirationTime={}, objectName={}, customFileName={}, presignedUrl={}",
                expirationTime, objectName, customFileName, presignedUrl);
        return presignedUrl;
    }

    @Override
    public URL generateUploadUrl(String objectName) {
        verifySupported();
        Date expirationTime = new Date(System.currentTimeMillis() + PRESIGNED_UPLOAD_URL_EXPIRATION_SECONDS * 1000);
        URL presignedUrl =
                publicEndpointCloudObjectStorage.generatePresignedPutUrl(getBucketName(), objectName, expirationTime);
        log.info("generate temporary upload Url successfully, expirationTime={}, objectName={}, presignedUrl={}",
                expirationTime, objectName, presignedUrl);
        return presignedUrl;
    }

    @Override
    public void putObject(String objectName, File file, ObjectTagging objectTagging) throws IOException {
        verifySupported();
        ObjectMetadata metadata = ObjectMetadata.builder().tagging(objectTagging).build();
        try {
            innerUpload(objectName, file, metadata);
        } catch (IOException e) {
            log.warn("Failed to upload file,  objectName={}, filePath={}",
                    objectName, file.getAbsolutePath(), e);
            throw new IOException(e);
        }
    }

    @Override
    public byte[] readContent(String objectName) throws IOException {
        verifySupported();
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

    @Override
    public void downloadToFile(String objectName, File targetFile) throws IOException {
        verifySupported();
        boolean exist = internalEndpointCloudObjectStorage.doesObjectExist(getBucketName(), objectName);
        if (!exist) {
            throw new FileNotFoundException("File dose not exist, object name " + objectName);
        }
        GetObjectRequest request = new GetObjectRequest(getBucketName(), objectName);
        internalEndpointCloudObjectStorage.getObject(request, targetFile);
    }

    @Override
    public List<String> deleteObjects(List<String> objectNames) {
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

    @Override
    public String deleteObject(String objectName) {
        verifySupported();
        DeleteObjectRequest request = new DeleteObjectRequest(getBucketName(), objectName);
        String deleted = internalEndpointCloudObjectStorage.deleteObject(request);
        log.info("Delete file success, tryDeleteObjectName={}, deletedObjectName={}",
                objectName, deleted);
        return deleted;
    }

    @Override
    public InputStream getObject(String objectName) throws IOException {
        verifySupported();
        boolean exist = internalEndpointCloudObjectStorage.doesObjectExist(getBucketName(), objectName);
        if (!exist) {
            throw new FileNotFoundException("File dose not exist, object name " + objectName);
        }
        try {
            return internalEndpointCloudObjectStorage.getObject(getBucketName(), objectName).getObjectContent();
        } catch (Exception exception) {
            log.warn("get object failed, objectName={}", objectName, exception);
            throw new IOException(exception);
        }
    }

    @Override
    public InputStream getAbortableObject(String objectName) throws IOException {
        verifySupported();
        if (!internalEndpointCloudObjectStorage.doesObjectExist(getBucketName(), objectName)) {
            throw new FileNotFoundException("File dose not exist, object name " + objectName);
        }
        try {
            return internalEndpointCloudObjectStorage.getObject(getBucketName(), objectName)
                    .getAbortableContent();
        } catch (Exception exception) {
            log.warn("get object failed, objectName={}", objectName, exception);
            throw new IOException(exception);
        }
    }

    /**
     * 文件上传方法，为了保证性能，如果文件大小小于10MB使用简单上传，如果文件大小大于10MB则使用分片上传功能
     *
     * @param objectName objectName, 名称会被进行合法性校验
     * @param file 要上传的文件
     */
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
        String bucketName = getBucketName();
        boolean isExist = publicEndpointCloudObjectStorage.doesBucketExist(bucketName);
        Verify.verify(isExist, String.format("object storage bucket '%s' not exists", bucketName));

        if (objectStorageConfiguration.getCloudProvider() != CloudProvider.ALIBABA_CLOUD) {
            return;
        }
        String region = objectStorageConfiguration.getRegion();
        if (StringUtils.isNotEmpty(region)) {
            String location = publicEndpointCloudObjectStorage.getBucketLocation(bucketName);
            log.info("location={},region={},cloudProvider={}", location, region,
                    objectStorageConfiguration.getCloudProvider());
            Verify.verify(StringUtils.equals(region, location) || StringUtils.endsWith(location, region),
                    "object storage bucket region does not match location, location=" + location + ", region="
                            + region);
        }
    }

    public boolean supported() {
        return publicEndpointCloudObjectStorage.supported();
    }

    public String getBucketName() {
        verifySupported();
        return objectStorageConfiguration.getBucketName();
    }

    public void verifySupported() {
        Verify.verify(supported(), "Cloud object storage not supported");
    }

    private void validateFileName(String objectName) {
        Matcher matcher = IS_OBJECT_NAME_LEGAL.matcher(objectName);
        if (!matcher.find()) {
            log.warn("Illegal filename detected, objectName={}", objectName);
            throw new IllegalArgumentException("Illegal filename, objectName=" + objectName);
        }
    }

    private Date calcExpirationTime(Long expirationSeconds, long contentSize) {
        if (expirationSeconds == null) {
            long time = contentSize / (1024 * 1024) + MIN_PRESIGNED_URL_EXPIRATION_SECONDS;
            return new Date(System.currentTimeMillis() + time * 1000);
        } else {
            return new Date(System.currentTimeMillis() + expirationSeconds * 1000);
        }
    }
}
