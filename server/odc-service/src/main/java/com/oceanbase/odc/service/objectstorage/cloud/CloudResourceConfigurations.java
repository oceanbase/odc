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

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.objectstorage.cloud.client.AlibabaCloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.AmazonCloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.NullCloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageProperties;
import com.oceanbase.odc.service.objectstorage.cloud.model.DefaultCloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CloudResourceConfigurations {

    @Bean("cloudEnvConfiguration")
    @ConditionalOnMissingBean(CloudEnvConfigurations.class)
    @RefreshScope
    public CloudEnvConfigurations defaultCloudEnvConfiguration(
            CloudObjectStorageProperties cloudObjectStorageProperties) {
        return new DefaultCloudEnvConfigurations(cloudObjectStorageProperties);
    }

    @Bean("publicEndpointCloudClient")
    @RefreshScope
    public CloudClient publicEndpointCloudClient(@Autowired CloudEnvConfigurations cloudEnvConfigurations) {
        ObjectStorageConfiguration objectStorageConfiguration = cloudEnvConfigurations.getObjectStorageConfiguration();
        return new CloudClientBuilder().generateCloudClient(objectStorageConfiguration,
                () -> getPublicOss(objectStorageConfiguration));

    }

    @Bean("internalEndpointCloudClient")
    @RefreshScope
    public CloudClient internalEndpointCloudClient(@Autowired CloudEnvConfigurations cloudEnvConfigurations) {
        ObjectStorageConfiguration objectStorageConfiguration = cloudEnvConfigurations.getObjectStorageConfiguration();
        return new CloudClientBuilder().generateCloudClient(objectStorageConfiguration,
                () -> getInternalOss(objectStorageConfiguration));
    }


    public static class CloudClientBuilder {
        public CloudClient generatePublicCloudClient(ObjectStorageConfiguration objectStorageConfiguration) {
            return generateCloudClient(objectStorageConfiguration, () -> getPublicOss(objectStorageConfiguration));
        }

        public CloudClient generateCloudClient(ObjectStorageConfiguration objectStorageConfiguration) {
            return generateCloudClient(objectStorageConfiguration, () -> getInternalOss(objectStorageConfiguration));
        }

        public CloudClient generateCloudClient(ObjectStorageConfiguration configuration, Supplier<OSS> ossSupplier) {
            CloudProvider cloudProvider = configuration.getCloudProvider();
            log.info("generate cloud client, cloudProvider={}, region={}, ak={}",
                    cloudProvider, configuration.getRegion(), configuration.getAccessKeyId());
            switch (cloudProvider) {
                case ALIBABA_CLOUD:
                    try {
                        return createAlibabaCloudClient(configuration, ossSupplier.get());
                    } catch (ClientException e) {
                        throw new RuntimeException("Create Alibaba Cloud Client failed", e);
                    }
                case AWS:
                case TENCENT_CLOUD:
                case HUAWEI_CLOUD:
                    return createAmazonCloudClient(configuration);
                default:
                    return new NullCloudClient();
            }
        }
    }


    static CloudClient createAlibabaCloudClient(ObjectStorageConfiguration configuration, OSS oss)
            throws ClientException {
        String accessKeyId = configuration.getAccessKeyId();
        String accessKeySecret = configuration.getAccessKeySecret();

        // 添加endpoint（直接使用STS endpoint，前两个参数留空，无需添加region ID）
        // 构造default profile（参数留空，无需添加region ID）
        DefaultProfile.addEndpoint("", "", "Sts", "sts.aliyuncs.com");
        IClientProfile profile = DefaultProfile.getProfile("", accessKeyId, accessKeySecret);
        IAcsClient acsClient = new DefaultAcsClient(profile);
        String roleSessionName = configuration.getRoleSessionName();
        String roleArn = configuration.getRoleArn();
        return new AlibabaCloudClient(oss, acsClient, roleSessionName, roleArn);
    }

    static AmazonCloudClient createAmazonCloudClient(ObjectStorageConfiguration configuration) {
        String region = configuration.getRegion();
        String accessKeyId = configuration.getAccessKeyId();
        String accessKeySecret = configuration.getAccessKeySecret();
        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKeyId, accessKeySecret));
        ClientConfiguration clientConfiguration = new ClientConfiguration().withProtocol(Protocol.HTTPS);

        AmazonS3ClientBuilder s3Builder = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withClientConfiguration(clientConfiguration)
                .disableChunkedEncoding();
        // if not AWS, means use S3 SDK to access other cloud storage, then we must set endpoint
        if (!configuration.getCloudProvider().isAWS()) {
            String endpoint = configuration.getPublicEndpoint();
            PreConditions.notBlank(endpoint, "endpoint");
            s3Builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, region));
            log.info("use S3 sdk for non-s3, cloudProvider={}, endpoint={}, region={}",
                    configuration.getCloudProvider(), endpoint, region);
        } else {
            PreConditions.notBlank(region, "region");
            s3Builder.withRegion(configuration.getRegion());
        }
        AmazonS3 s3 = s3Builder.build();

        // TODO: set sts endpoint for other cloud provider
        AWSSecurityTokenService sts = AWSSecurityTokenServiceClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
        String roleSessionName = configuration.getRoleSessionName();
        String roleArn = configuration.getRoleArn();
        return new AmazonCloudClient(s3, sts, roleSessionName, roleArn);
    }

    private static OSS getInternalOss(ObjectStorageConfiguration objectStorageConfiguration) {
        return createOssClient(
                objectStorageConfiguration.getAccessKeyId(),
                objectStorageConfiguration.getAccessKeySecret(),
                objectStorageConfiguration.getInternalEndpoint());
    }

    private static OSS getPublicOss(ObjectStorageConfiguration objectStorageConfiguration) {
        return createOssClient(
                objectStorageConfiguration.getAccessKeyId(),
                objectStorageConfiguration.getAccessKeySecret(),
                objectStorageConfiguration.getPublicEndpoint());
    }

    private static OSS createOssClient(String ak, String sk, String endpoint) {
        com.aliyun.oss.ClientBuilderConfiguration clientBuilderConfiguration =
                new com.aliyun.oss.ClientBuilderConfiguration();
        clientBuilderConfiguration.setProtocol(com.aliyun.oss.common.comm.Protocol.HTTPS);
        OSS oss = new OSSClientBuilder().build(endpoint, ak, sk, clientBuilderConfiguration);
        return oss;
    }
}
