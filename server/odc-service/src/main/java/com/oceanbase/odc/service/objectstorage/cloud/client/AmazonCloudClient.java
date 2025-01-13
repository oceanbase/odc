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
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsResult.DeletedObject;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
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
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectTagging;
import com.oceanbase.odc.service.objectstorage.cloud.model.PartETag;
import com.oceanbase.odc.service.objectstorage.cloud.model.PutObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.cloud.model.StsPolicy;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadObjectTemporaryCredential;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartResult;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmazonCloudClient implements CloudClient {
    private static final String STS_POLICY_VERSION = "2012-10-17";
    private final AmazonS3 s3;
    private final AWSSecurityTokenService sts;
    private final String roleSessionName;
    private final String roleArn;

    public AmazonCloudClient(AmazonS3 s3, AWSSecurityTokenService sts, String roleSessionName, String roleArn) {
        this.s3 = s3;
        this.sts = sts;
        this.roleSessionName = roleSessionName;
        this.roleArn = roleArn;
    }

    @Override
    public boolean supported() {
        return true;
    }

    @Override
    public String getBucketLocation(String bucketName) throws CloudException {
        return callAmazonMethod("Get bucket location", () -> s3.getBucketLocation(bucketName));
    }

    @Override
    public boolean doesBucketExist(String bucketName) throws CloudException {
        return callAmazonMethod("Check bucket exist", () -> s3.doesBucketExistV2(bucketName));
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request)
            throws CloudException {
        return callAmazonMethod("Initiate multipart upload", () -> {
            com.amazonaws.services.s3.model.InitiateMultipartUploadRequest s3Request =
                    new com.amazonaws.services.s3.model.InitiateMultipartUploadRequest(request.getBucketName(),
                            request.getKey(), toS3(request.getObjectMetadata()));
            ObjectMetadata objectMetadata = request.getObjectMetadata();
            if (objectMetadata.hasTag()) {
                s3Request.withTagging(toS3(objectMetadata.getTagging()));
            }
            com.amazonaws.services.s3.model.InitiateMultipartUploadResult s3Result =
                    s3.initiateMultipartUpload(s3Request);
            InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
            result.setUploadId(s3Result.getUploadId());
            return result;
        });
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest request) throws CloudException {
        return callAmazonMethod("Initiate multipart upload", () -> {
            com.amazonaws.services.s3.model.UploadPartRequest s3Request =
                    new com.amazonaws.services.s3.model.UploadPartRequest()
                            .withBucketName(request.getBucketName())
                            .withKey(request.getKey())
                            .withUploadId(request.getUploadId())
                            .withPartNumber(request.getPartNumber())
                            .withInputStream(request.getInputStream())
                            .withPartSize(request.getPartSize());
            com.amazonaws.services.s3.model.UploadPartResult s3Result = s3.uploadPart(s3Request);
            UploadPartResult result = new UploadPartResult();
            result.setPartSize(request.getPartSize());
            result.setPartNumber(s3Result.getPartNumber());
            com.amazonaws.services.s3.model.PartETag partETag = s3Result.getPartETag();
            result.setETag(s3Result.getETag());
            result.setPartETag(new PartETag(partETag.getPartNumber(), partETag.getETag()));
            return result;
        });
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request)
            throws CloudException {
        return callAmazonMethod("Complete multipart upload", () -> {
            com.amazonaws.services.s3.model.CompleteMultipartUploadRequest s3Request =
                    new com.amazonaws.services.s3.model.CompleteMultipartUploadRequest(
                            request.getBucketName(), request.getKey(),
                            request.getUploadId(),
                            request.getPartETags().stream()
                                    .map(t -> new com.amazonaws.services.s3.model.PartETag(t.getPartNumber(),
                                            t.getETag()))
                                    .collect(Collectors.toList()));
            com.amazonaws.services.s3.model.CompleteMultipartUploadResult s3Result =
                    s3.completeMultipartUpload(s3Request);
            CompleteMultipartUploadResult result = new CompleteMultipartUploadResult();
            result.setBucketName(s3Result.getBucketName());
            result.setKey(s3Result.getKey());
            result.setLocation(s3Result.getLocation());
            result.setETag(s3Result.getETag());
            return result;
        });
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, File file, ObjectMetadata metadata)
            throws CloudException {
        return callAmazonMethod("Put object", () -> {
            com.amazonaws.services.s3.model.ObjectMetadata objectMetadata = toS3(metadata);
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, file)
                    .withMetadata(objectMetadata);
            if (metadata.hasTag()) {
                putRequest.withTagging(toS3(metadata.getTagging()));
            }
            com.amazonaws.services.s3.model.PutObjectResult s3Result = s3.putObject(putRequest);
            PutObjectResult result = new PutObjectResult();
            result.setVersionId(s3Result.getVersionId());
            result.setETag(s3Result.getETag());
            return result;
        });
    }

    @Override
    public CopyObjectResult copyObject(String bucketName, String from, String to)
            throws CloudException {
        return callAmazonMethod("Copy object to", () -> {
            com.amazonaws.services.s3.model.CopyObjectResult copyObjectResult =
                    s3.copyObject(bucketName, from, bucketName, to);
            CopyObjectResult result = new CopyObjectResult();
            result.setVersionId(copyObjectResult.getVersionId());
            result.setVersionId(copyObjectResult.getETag());
            result.setLastModifyTime(copyObjectResult.getLastModifiedDate());
            return result;
        });
    }

    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) throws CloudException {
        com.amazonaws.services.s3.model.DeleteObjectsRequest s3Request =
                new com.amazonaws.services.s3.model.DeleteObjectsRequest(request.getBucketName())
                        .withKeys(request.getKeys().toArray(new String[0]))
                        .withQuiet(request.isQuiet());
        return callAmazonMethod("Delete objects", () -> {
            com.amazonaws.services.s3.model.DeleteObjectsResult s3Result = s3.deleteObjects(s3Request);
            DeleteObjectsResult result = new DeleteObjectsResult();
            result.setDeletedObjects(
                    s3Result.getDeletedObjects().stream().map(DeletedObject::getKey).collect(Collectors.toList()));
            return result;
        });
    }

    @Override
    public String deleteObject(DeleteObjectRequest request) throws CloudException {
        com.amazonaws.services.s3.model.DeleteObjectRequest s3Request =
                new com.amazonaws.services.s3.model.DeleteObjectRequest(request.getBucketName(), request.getKey());
        return callAmazonMethod("Delete object", () -> {
            s3.deleteObject(s3Request);
            return s3Request.getKey();
        });
    }

    @Override
    public boolean doesObjectExist(String bucketName, String key) throws CloudException {
        return callAmazonMethod("Check object exist", () -> s3.doesObjectExist(bucketName, key));
    }

    @Override
    public StorageObject getObject(String bucketName, String key) throws CloudException {
        return callAmazonMethod("Get object", () -> toCloud(s3.getObject(bucketName, key)));
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest request, File file) throws CloudException {
        com.amazonaws.services.s3.model.GetObjectRequest s3Request =
                new com.amazonaws.services.s3.model.GetObjectRequest(request.getBucketName(), request.getKey(),
                        request.getVersionId());
        return callAmazonMethod("Get object", () -> toCloud(s3.getObject(s3Request, file)));
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key) throws CloudException {
        return callAmazonMethod("Get object metadata", () -> toCloud(s3.getObjectMetadata(bucketName, key)));
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws CloudException {
        Verify.notBlank(key, "key");
        return callAmazonMethod("Generate presigned URL", () -> {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setBucketName(bucketName);
            request.setExpiration(expiration);
            request.setKey(key);
            ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides();
            responseHeaderOverrides.setContentDisposition(
                    String.format("attachment;filename=%s",
                            new String(CloudObjectStorageUtil.getOriginalFileName(key).getBytes(),
                                    StandardCharsets.ISO_8859_1)));
            request.setResponseHeaders(responseHeaderOverrides);
            return s3.generatePresignedUrl(request);
        });
    }

    @Override
    public URL generatePresignedUrlWithCustomFileName(String bucketName, String key, Date expiration,
            String customFileName) throws CloudException {
        Verify.notBlank(key, "key");
        return callAmazonMethod("Generate presigned URL", () -> {
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
            return s3.generatePresignedUrl(request);
        });
    }

    @Override
    public URL generatePresignedPutUrl(String bucketName, String key, Date expiration) throws CloudException {
        Verify.notBlank(key, "key");
        return callAmazonMethod("Generate presigned PUT URL", () -> {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setExpiration(expiration);
            request.setMethod(HttpMethod.PUT);
            request.setContentType("application/octet-stream");
            return s3.generatePresignedUrl(request);
        });
    }

    @Override
    public List<ObjectSummary> list(String bucketName, String prefix) throws CloudException {
        return callAmazonMethod("List objects by prefix", () -> s3.listObjects(bucketName, prefix).getObjectSummaries()
                .stream().map(this::toCloud).collect(Collectors.toList()));
    }

    /**
     * refer from https://docs.aws.amazon.com/AmazonS3/latest/userguide/example-bucket-policies.html ,
     * the policy version must be '2012-10-17'
     */
    @Override
    public UploadObjectTemporaryCredential generateTempCredential(@NotBlank String bucketName,
            @NotBlank String objectName, @NotNull Long durationSeconds) throws CloudException {
        return callAmazonMethod("Generate temp credential", () -> {
            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setRoleArn(this.roleArn);
            request.setRoleSessionName(this.roleSessionName);
            request.setDurationSeconds(durationSeconds.intValue());

            String objectKeyArn = String.format("arn:aws:s3:::%s/%s", bucketName, objectName);
            StsPolicy stsPolicy = new StsPolicy().withVersion(STS_POLICY_VERSION)
                    .allowPutObject("s3:PutObject", objectKeyArn);
            String policyJson = JsonUtils.toJson(stsPolicy);
            request.setPolicy(policyJson);

            // do assumeRole
            AssumeRoleResult assumeRoleResult = sts.assumeRole(request);

            UploadObjectTemporaryCredential credential = new UploadObjectTemporaryCredential();
            Credentials credentials = assumeRoleResult.getCredentials();
            credential.setAccessKeyId(credentials.getAccessKeyId());
            credential.setAccessKeySecret(credentials.getSecretAccessKey());
            credential.setSecurityToken(credentials.getSessionToken());
            credential.setExpiration(credentials.getExpiration().toString());
            credential.setFilePath(objectName);
            return credential;

        });
    }

    private <T> T callAmazonMethod(String operation, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            throw new CloudException(operation + " failed", ex);
        }
    }

    private StorageObject toCloud(S3Object s3Object) {
        StorageObject storageObject = new StorageObject() {
            @Override
            public InputStream getObjectContent() {
                return s3Object.getObjectContent();
            }

            @Override
            public InputStream getAbortableContent() {
                return new AbortableS3Stream(s3Object.getObjectContent());
            }
        };
        storageObject.setBucketName(s3Object.getBucketName());
        storageObject.setKey(s3Object.getKey());
        storageObject.setMetadata(toCloud(s3Object.getObjectMetadata()));
        return storageObject;
    }

    private ObjectMetadata toCloud(com.amazonaws.services.s3.model.ObjectMetadata metadata) {
        PreConditions.notNull(metadata, "metadata");
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(metadata.getContentLength());
        objectMetadata.setContentMD5(metadata.getContentMD5());
        objectMetadata.setContentType(metadata.getContentType());
        objectMetadata.setETag(metadata.getETag());
        objectMetadata.setExpirationTime(metadata.getExpirationTime());
        objectMetadata.setUserMetadata(metadata.getUserMetadata());
        return objectMetadata;
    }

    private ObjectSummary toCloud(S3ObjectSummary s3ObjectSummary) {
        ObjectSummary objectSummary = new ObjectSummary();
        objectSummary.setBucketName(s3ObjectSummary.getBucketName());
        objectSummary.setKey(s3ObjectSummary.getKey());
        objectSummary.setETag(s3ObjectSummary.getETag());
        objectSummary.setStorageClass(s3ObjectSummary.getStorageClass());
        objectSummary.setLastModified(s3ObjectSummary.getLastModified());
        objectSummary.setSize(s3ObjectSummary.getSize());
        return objectSummary;
    }

    private com.amazonaws.services.s3.model.ObjectMetadata toS3(ObjectMetadata metadata) {
        PreConditions.notNull(metadata, "metadata");
        com.amazonaws.services.s3.model.ObjectMetadata objectMetadata =
                new com.amazonaws.services.s3.model.ObjectMetadata();
        if (Objects.nonNull(metadata.getContentLength())) {
            objectMetadata.setContentLength(metadata.getContentLength());
        }
        objectMetadata.setContentMD5(metadata.getContentMD5());
        objectMetadata.setContentType(metadata.getContentType());
        if (Objects.nonNull(metadata.getExpirationTime())) {
            objectMetadata.setExpirationTime(metadata.getExpirationTime());
        }
        objectMetadata.setUserMetadata(metadata.getUserMetadata());
        return objectMetadata;
    }

    private com.amazonaws.services.s3.model.ObjectTagging toS3(ObjectTagging objectTagging) {
        return new com.amazonaws.services.s3.model.ObjectTagging(objectTagging.getTagSet()
                .stream().map(t -> new Tag(t.getKey(), t.getValue())).collect(Collectors.toList()));
    }

    private static final class AbortableS3Stream extends InputStream {
        private final S3ObjectInputStream content;

        AbortableS3Stream(S3ObjectInputStream inputStream) {
            this.content = inputStream;
        }

        @Override
        public int read() throws IOException {
            return content.read();
        }

        @Override
        public void close() {
            content.abort();
        }
    }

}
