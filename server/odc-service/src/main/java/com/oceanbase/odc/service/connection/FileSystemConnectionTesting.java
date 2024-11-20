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
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
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
public class FileSystemConnectionTesting {

    private static final String COS_ENDPOINT_REGEX = "cos\\.(\\w+-\\w+)\\.myqcloud\\.com";
    private static final String OSS_ENDPOINT_REGEX = "oss-([a-zA-Z0-9-]+)\\.aliyuncs\\.com";
    private static final String OBS_ENDPOINT_REGEX = "obs\\.([a-zA-Z0-9-]+)\\.myhuaweicloud\\.com";
    private static final String S3_ENDPOINT_REGEX = "s3\\.([a-zA-Z0-9-]+)\\.amazonaws\\.com(\\.cn)?";
    private static final String TMP_FILE_NAME_PREFIX = "odc-test-object-";
    private static final String TMP_TEST_DATA = "This is a test object to check read and write permissions.";

    public ConnectionTestResult test(@NonNull ConnectionConfig config) {
        PreConditions.notBlank(config.getPassword(), "AccessKeySecret");
        PreConditions.notBlank(config.getDefaultSchema(), "Bucket");
        ObjectStorageConfiguration storageConfig = new ObjectStorageConfiguration();
        storageConfig.setAccessKeyId(config.getUsername());
        storageConfig.setAccessKeySecret(config.getPassword());
        storageConfig.setBucketName(config.getDefaultSchema().split("/")[0]);
        storageConfig.setRegion(getRegion(config));
        storageConfig.setCloudProvider(getCloudProvider(config.getDialectType()));
        storageConfig.setPublicEndpoint(config.getHost());
        try {
            CloudClient cloudClient =
                    new CloudResourceConfigurations.CloudClientBuilder().generateCloudClient(storageConfig);
            String tempFileName = generateTempFileName();
            String objectKey = config.getDefaultSchema() + tempFileName;
            cloudClient.putObject(storageConfig.getBucketName(), objectKey,
                    new ByteArrayInputStream(TMP_TEST_DATA.getBytes(StandardCharsets.UTF_8)), new ObjectMetadata());
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest();
            deleteObjectsRequest.setBucketName(storageConfig.getBucketName());
            deleteObjectsRequest.setKey(objectKey);
            cloudClient.deleteObjects(deleteObjectsRequest);
            return ConnectionTestResult.success(config.getType());
        } catch (CloudException e) {
            if (e.getCause() instanceof OSSException) {
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

    private String getRegion(ConnectionConfig config) {
        Pattern pattern = Pattern.compile(getEndPointRegex(config.getDialectType()));
        Matcher matcher = pattern.matcher(config.getHost());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new UnExpectedException("Illegal endpoint");
        }
    }

    private CloudProvider getCloudProvider(DialectType type) {
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

    private String getEndPointRegex(DialectType type) {
        switch (type) {
            case COS:
                return COS_ENDPOINT_REGEX;
            case OBS:
                return OBS_ENDPOINT_REGEX;
            case S3A:
                return S3_ENDPOINT_REGEX;
            case OSS:
                return OSS_ENDPOINT_REGEX;
            default:
                throw new UnExpectedException();
        }
    }

    private String generateTempFileName() {
        return TMP_FILE_NAME_PREFIX + UUID.randomUUID();
    }
}
