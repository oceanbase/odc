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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudException;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.tools.migrator.common.exception.UnExpectedException;

import lombok.NonNull;

/**
 * @Author：tinker
 * @Date: 2024/11/19 11:24
 * @Descripition:
 */

@Component
public class FileSystemConnectionTester {

    private static final String COS_ENDPOINT_PATTERN = "cos.{0}.myqcloud.com";
    private static final String OBS_ENDPOINT_PATTERN = "obs.{0}.myhuaweicloud.com";
    private static final String OSS_ENDPOINT_PATTERN = "oss-{0}.aliyuncs.com";
    private static final String S3_ENDPOINT_GLOBAL_PATTERN = "s3.{0}.amazonaws.com";
    private static final String S3_ENDPOINT_CN_PATTERN = "s3.{0}.amazonaws.com.cn";

    private static final String TMP_FILE_NAME_PREFIX = "odc-test-object-";
    private static final String TMP_TEST_DATA = "This is a test object to check read and write permissions.";

    public ConnectionTestResult test(@NonNull ConnectionConfig config) {
        PreConditions.notBlank(config.getPassword(), "AccessKeySecret");
        PreConditions.notBlank(config.getRegion(), "Region");
        URI uri = URI.create(config.getHost());
        ObjectStorageConfiguration storageConfig = new ObjectStorageConfiguration();
        storageConfig.setAccessKeyId(config.getUsername());
        storageConfig.setAccessKeySecret(config.getPassword());
        storageConfig.setBucketName(uri.getAuthority());
        storageConfig.setRegion(config.getRegion());
        storageConfig.setCloudProvider(getCloudProvider(config.getType()));
        storageConfig.setPublicEndpoint(getEndPointByRegion(config.getType(), config.getRegion()));
        try {
            CloudClient cloudClient =
                    new CloudResourceConfigurations.CloudClientBuilder().generateCloudClient(storageConfig);
            String objectKey = uri.getPath().endsWith("/") ? uri.getAuthority() + uri.getPath() + generateTempFileName()
                    : uri.getAuthority() + uri.getPath() + "/" + generateTempFileName();
            cloudClient.putObject(storageConfig.getBucketName(), objectKey,
                    new ByteArrayInputStream(TMP_TEST_DATA.getBytes(StandardCharsets.UTF_8)), new ObjectMetadata());
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest();
            deleteObjectsRequest.setBucketName(storageConfig.getBucketName());
            deleteObjectsRequest.setKeys(Collections.singletonList(objectKey));
            cloudClient.deleteObjects(deleteObjectsRequest);
            return ConnectionTestResult.success(config.getType());
        } catch (CloudException e) {
            if (e.getCause() != null && e.getCause() instanceof OSSException) {
                OSSException cause = (OSSException) e.getCause();
                switch (cause.getErrorCode()) {
                    case OSSErrorCode.ACCESS_DENIED:
                        return new ConnectionTestResult(TestResult.akAccessDenied(storageConfig.getAccessKeyId()),
                                config.getType());
                    case OSSErrorCode.INVALID_ACCESS_KEY_ID:
                        return new ConnectionTestResult(TestResult.invalidAccessKeyId(storageConfig.getAccessKeyId()),
                                config.getType());
                    case OSSErrorCode.SIGNATURE_DOES_NOT_MATCH:
                        return new ConnectionTestResult(
                                TestResult.signatureDoesNotMatch(storageConfig.getAccessKeyId()), config.getType());
                    case OSSErrorCode.NO_SUCH_BUCKET:
                        return new ConnectionTestResult(TestResult.bucketNotExist(storageConfig.getBucketName()),
                                config.getType());
                    default:
                        return new ConnectionTestResult(TestResult.unknownError(e), config.getType());
                }
            }
            // TODO:process s3 error message
            return new ConnectionTestResult(TestResult.unknownError(e), config.getType());
        }
    }

    private CloudProvider getCloudProvider(ConnectType type) {
        switch (type) {
            case COS:
                return CloudProvider.TENCENT_CLOUD;
            case OBS:
                return CloudProvider.HUAWEI_CLOUD;
            case S3A:
                return CloudProvider.AWS;
            case OSS:
                return CloudProvider.ALIBABA_CLOUD;
            default:
                throw new UnExpectedException();
        }
    }

    private static String getEndPointByRegion(ConnectType type, String region) {
        switch (type) {
            case COS:
                return MessageFormat.format(COS_ENDPOINT_PATTERN, region);
            case OSS:
                return MessageFormat.format(OSS_ENDPOINT_PATTERN, region);
            case OBS:
                return MessageFormat.format(OBS_ENDPOINT_PATTERN, region);
            case S3A:
                // Note there is a difference of Top-Level Domain between cn and global regions.
                if (region.startsWith("cn-")) {
                    return MessageFormat.format(S3_ENDPOINT_CN_PATTERN, region);
                }
                return MessageFormat.format(S3_ENDPOINT_GLOBAL_PATTERN, region);
            default:
                throw new IllegalArgumentException("regionToEndpoint is not applicable for storageType " + type);
        }
    }

    private String generateTempFileName() {
        return TMP_FILE_NAME_PREFIX + UUID.randomUUID();
    }
}
