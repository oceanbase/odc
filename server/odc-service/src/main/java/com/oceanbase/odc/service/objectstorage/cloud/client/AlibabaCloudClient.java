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
package com.oceanbase.odc.service.objectstorage.cloud.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse.Credentials;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.CopyObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.GetObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectSummary;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectTagging.Tag;
import com.oceanbase.odc.service.objectstorage.cloud.model.PartETag;
import com.oceanbase.odc.service.objectstorage.cloud.model.PutObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.cloud.model.StsPolicy;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadObjectTemporaryCredential;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartResult;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;

public class AlibabaCloudClient implements CloudClient {
    private final OSS oss;
    private final IAcsClient acsClient;
    private final String roleSessionName;
    private final String roleArn;

    public AlibabaCloudClient(OSS oss, IAcsClient acsClient, String roleSessionName, String roleArn) {
        this.oss = oss;
        this.acsClient = acsClient;
        this.roleSessionName = roleSessionName;
        this.roleArn = roleArn;
    }

    @Override
    public boolean supported() {
        return true;
    }

    @Override
    public String getBucketLocation(String bucketName) throws CloudException {
        return callOssMethod("Get bucket location", () -> oss.getBucketLocation(bucketName));
    }

    @Override
    public boolean doesBucketExist(String bucketName) throws CloudException {
        return callOssMethod("Check bucket exist", () -> oss.doesBucketExist(bucketName));
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request)
            throws CloudException {
        return callOssMethod("Initiate multipart upload", () -> {
            com.aliyun.oss.model.InitiateMultipartUploadRequest ossRequest =
                    new com.aliyun.oss.model.InitiateMultipartUploadRequest(
                            request.getBucketName(), request.getKey(), toOss(request.getObjectMetadata()));
            com.aliyun.oss.model.InitiateMultipartUploadResult ossResult = oss.initiateMultipartUpload(ossRequest);
            InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
            result.setUploadId(ossResult.getUploadId());
            result.setRequestId(ossResult.getRequestId());
            result.setClientCRC(ossResult.getClientCRC());
            result.setServerCRC(ossResult.getServerCRC());
            return result;
        });
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest request) throws CloudException {
        return callOssMethod("Initiate multipart upload", () -> {
            com.aliyun.oss.model.UploadPartRequest ossRequest =
                    new com.aliyun.oss.model.UploadPartRequest(request.getBucketName(), request.getKey(),
                            request.getUploadId(), request.getPartNumber(), request.getInputStream(),
                            request.getPartSize());
            com.aliyun.oss.model.UploadPartResult ossResult = oss.uploadPart(ossRequest);
            UploadPartResult result = new UploadPartResult();
            result.setPartSize(ossResult.getPartSize());
            result.setPartNumber(ossResult.getPartNumber());
            com.aliyun.oss.model.PartETag partETag = ossResult.getPartETag();
            result.setETag(ossResult.getETag());
            result.setPartETag(new PartETag(partETag.getPartNumber(), partETag.getETag()));
            return result;
        });
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request)
            throws CloudException {
        return callOssMethod("Complete multipart upload", () -> {
            com.aliyun.oss.model.CompleteMultipartUploadRequest ossRequest =
                    new com.aliyun.oss.model.CompleteMultipartUploadRequest(
                            request.getBucketName(), request.getKey(),
                            request.getUploadId(),
                            request.getPartETags().stream()
                                    .map(t -> new com.aliyun.oss.model.PartETag(t.getPartNumber(), t.getETag()))
                                    .collect(Collectors.toList()));
            com.aliyun.oss.model.CompleteMultipartUploadResult ossResult = oss.completeMultipartUpload(ossRequest);
            CompleteMultipartUploadResult result = new CompleteMultipartUploadResult();
            result.setBucketName(ossResult.getBucketName());
            result.setKey(ossResult.getKey());
            result.setLocation(ossResult.getLocation());
            result.setETag(ossResult.getETag());
            return result;
        });
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, File file, ObjectMetadata metadata)
            throws CloudException {
        PutObjectResult putObject = callOssMethod("Put object", () -> {
            com.aliyun.oss.model.ObjectMetadata objectMetadata = toOss(metadata);
            com.aliyun.oss.model.PutObjectResult ossResult = oss.putObject(bucketName, key, file, objectMetadata);
            PutObjectResult result = new PutObjectResult();
            result.setVersionId(ossResult.getVersionId());
            result.setETag(ossResult.getETag());
            result.setRequestId(ossResult.getRequestId());
            result.setClientCRC(ossResult.getClientCRC());
            result.setServerCRC(ossResult.getServerCRC());
            return result;
        });
        return putObject;
    }

    @Override
    public CopyObjectResult copyObject(String bucketName, String from, String to)
            throws CloudException {
        return callOssMethod("Copy object to", () -> {
            com.aliyun.oss.model.CopyObjectResult copyObjectResult = oss.copyObject(bucketName, from, bucketName, to);
            CopyObjectResult result = new CopyObjectResult();
            result.setVersionId(copyObjectResult.getVersionId());
            result.setVersionId(copyObjectResult.getETag());
            result.setRequestId(copyObjectResult.getRequestId());
            result.setClientCRC(copyObjectResult.getClientCRC());
            result.setServerCRC(copyObjectResult.getServerCRC());
            return result;
        });
    }

    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) throws CloudException {
        com.aliyun.oss.model.DeleteObjectsRequest ossRequest =
                new com.aliyun.oss.model.DeleteObjectsRequest(request.getBucketName())
                        .withKeys(request.getKeys())
                        .withQuiet(request.isQuiet());
        return callOssMethod("Delete objects", () -> {
            com.aliyun.oss.model.DeleteObjectsResult ossResult = oss.deleteObjects(ossRequest);
            DeleteObjectsResult result = new DeleteObjectsResult();
            result.setRequestId(ossResult.getRequestId());
            result.setDeletedObjects(ossResult.getDeletedObjects());
            return result;
        });
    }

    @Override
    public String deleteObject(DeleteObjectRequest request) throws CloudException {
        com.aliyun.oss.model.DeleteObjectsRequest ossRequest =
                new com.aliyun.oss.model.DeleteObjectsRequest(request.getBucketName())
                        .withKeys(Collections.singletonList(request.getKey()));
        return callOssMethod("Delete object", () -> {
            com.aliyun.oss.model.DeleteObjectsResult ossResult = oss.deleteObjects(ossRequest);
            return ossResult.getDeletedObjects().get(0);
        });
    }

    @Override
    public boolean doesObjectExist(String bucketName, String key) throws CloudException {
        return callOssMethod("Check object exist", () -> oss.doesObjectExist(bucketName, key));
    }

    @Override
    public StorageObject getObject(String bucketName, String key) throws CloudException {
        return callOssMethod("Get object", () -> toCloud(oss.getObject(bucketName, key)));
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest request, File file) throws CloudException {
        com.aliyun.oss.model.GetObjectRequest ossRequest =
                new com.aliyun.oss.model.GetObjectRequest(request.getBucketName(), request.getKey(),
                        request.getVersionId());
        return callOssMethod("Get object", () -> toCloud(oss.getObject(ossRequest, file)));
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key) throws CloudException {
        return callOssMethod("Get object metadata", () -> toCloud(oss.getObjectMetadata(bucketName, key)));
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws CloudException {
        Verify.notBlank(key, "key");
        return callOssMethod("Generate presigned URL", () -> {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setBucketName(bucketName);
            request.setExpiration(expiration);
            request.setKey(key);
            ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides();
            responseHeaderOverrides.setContentDisposition(
                    String.format("attachment;filename=%s", CloudObjectStorageUtil.getOriginalFileName(key)));
            request.setResponseHeaders(responseHeaderOverrides);
            return oss.generatePresignedUrl(request);
        });
    }

    @Override
    public URL generatePresignedUrlWithCustomFileName(String bucketName, String key, Date expiration,
            String customFileName) throws CloudException {
        Verify.notBlank(key, "key");
        return callOssMethod("Generate presigned URL", () -> {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setBucketName(bucketName);
            request.setExpiration(expiration);
            request.setKey(key);
            ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides();
            String fileName = customFileName;
            if (StringUtils.isBlank(customFileName)) {
                fileName = CloudObjectStorageUtil.getOriginalFileName(key);
            }
            responseHeaderOverrides.setContentDisposition(
                    String.format("attachment;filename=%s", fileName));
            request.setResponseHeaders(responseHeaderOverrides);
            return oss.generatePresignedUrl(request);
        });
    }

    @Override
    public URL generatePresignedPutUrl(String bucketName, String key, Date expiration) throws CloudException {
        Verify.notBlank(key, "key");
        return callOssMethod("Generate presigned PUT URL", () -> {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setExpiration(expiration);
            request.setMethod(HttpMethod.PUT);
            request.setContentType("application/octet-stream");
            return oss.generatePresignedUrl(request);
        });
    }

    @Override
    public List<ObjectSummary> list(String bucketName, String prefix) throws CloudException {
        return callOssMethod("List objects by prefix", () -> oss.listObjects(bucketName, prefix).getObjectSummaries()
                .stream().map(this::toCloud).collect(Collectors.toList()));
    }

    /**
     * refer from https://help.aliyun.com/document_detail/100624.htm?#p-tbd-4ia-uki
     */
    @Override
    public UploadObjectTemporaryCredential generateTempCredential(@NotBlank String bucketName,
            @NotBlank String objectName, @NotNull Long durationSeconds) throws CloudException {
        return callOssMethod("Generate temp credential", () -> {
            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setMethod(MethodType.POST);
            request.setRoleArn(this.roleArn);
            request.setRoleSessionName(this.roleSessionName);
            request.setDurationSeconds(durationSeconds);

            StsPolicy policy = new StsPolicy().allowPutObject("oss:PutObject",
                    String.format("acs:oss:*:*:%s/%s", bucketName, objectName));
            request.setPolicy(JsonUtils.toJson(policy));
            try {
                AssumeRoleResponse response = acsClient.getAcsResponse(request);
                UploadObjectTemporaryCredential credential = new UploadObjectTemporaryCredential();
                Credentials credentials = response.getCredentials();
                credential.setAccessKeyId(credentials.getAccessKeyId());
                credential.setAccessKeySecret(credentials.getAccessKeySecret());
                credential.setSecurityToken(credentials.getSecurityToken());
                credential.setExpiration(credentials.getExpiration());
                credential.setFilePath(objectName);
                return credential;
            } catch (ClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> T callOssMethod(String operation, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            throw new CloudException(operation + " failed", ex);
        }
    }

    private StorageObject toCloud(OSSObject ossObject) {
        StorageObject storageObject = new StorageObject() {
            @Override
            public InputStream getObjectContent() {
                return ossObject.getObjectContent();
            }

            @Override
            public InputStream getAbortableContent() {
                return new AbortableOssStream(ossObject);
            }
        };
        storageObject.setBucketName(ossObject.getBucketName());
        storageObject.setKey(ossObject.getKey());
        storageObject.setRequestId(ossObject.getRequestId());
        storageObject.setMetadata(toCloud(ossObject.getObjectMetadata()));
        return storageObject;
    }

    private ObjectSummary toCloud(OSSObjectSummary ossObject) {
        ObjectSummary objectSummary = new ObjectSummary();
        objectSummary.setBucketName(ossObject.getBucketName());
        objectSummary.setKey(ossObject.getKey());
        objectSummary.setType(ossObject.getType());
        objectSummary.setETag(ossObject.getETag());
        objectSummary.setStorageClass(ossObject.getStorageClass());
        objectSummary.setLastModified(ossObject.getLastModified());
        objectSummary.setSize(ossObject.getSize());
        return objectSummary;
    }

    private ObjectMetadata toCloud(com.aliyun.oss.model.ObjectMetadata metadata) {
        PreConditions.notNull(metadata, "metadata");
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(metadata.getContentLength());
        objectMetadata.setContentMD5(metadata.getContentMD5());
        objectMetadata.setContentType(metadata.getContentType());
        objectMetadata.setETag(metadata.getETag());
        try {
            objectMetadata.setExpirationTime(metadata.getExpirationTime());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        objectMetadata.setUserMetadata(metadata.getUserMetadata());
        return objectMetadata;
    }

    private com.aliyun.oss.model.ObjectMetadata toOss(ObjectMetadata metadata) {
        PreConditions.notNull(metadata, "metadata");
        com.aliyun.oss.model.ObjectMetadata objectMetadata = new com.aliyun.oss.model.ObjectMetadata();
        if (Objects.nonNull(metadata.getContentLength())) {
            objectMetadata.setContentLength(metadata.getContentLength());
        }
        objectMetadata.setContentMD5(metadata.getContentMD5());
        objectMetadata.setContentType(metadata.getContentType());
        if (Objects.nonNull(metadata.getExpirationTime())) {
            objectMetadata.setExpirationTime(metadata.getExpirationTime());
        }
        objectMetadata.setUserMetadata(metadata.getUserMetadata());
        if (metadata.hasTag()) {
            Map<String, String> tags = metadata.getTagging().getTagSet().stream().collect(
                    Collectors.toMap(Tag::getKey, Tag::getValue));
            objectMetadata.setObjectTagging(tags);
        }
        return objectMetadata;
    }

    private static final class AbortableOssStream extends InputStream {
        private final OSSObject ossObject;
        private final InputStream content;

        AbortableOssStream(OSSObject ossObject) {
            this.ossObject = ossObject;
            this.content = ossObject.getObjectContent();
        }

        @Override
        public int read() throws IOException {
            return content.read();
        }

        @Override
        public void close() throws IOException {
            ossObject.forcedClose();
        }
    }
}
