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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.oceanbase.odc.service.objectstorage.cloud.client.AlibabaCloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.AmazonCloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.NullCloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageProperties;
import com.oceanbase.odc.service.objectstorage.cloud.model.DefaultCloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration.CloudProvider;

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
        public CloudClient generateCloudClient(ObjectStorageConfiguration objectStorageConfiguration) {
            return generateCloudClient(objectStorageConfiguration, () -> getInternalOss(objectStorageConfiguration));
        }

        public CloudClient generateCloudClient(ObjectStorageConfiguration objectStorageConfiguration,
                Supplier<OSS> ossSupplier) {
            CloudProvider cloudProvider = objectStorageConfiguration.getCloudProvider();
            log.info("recreate cloud client, ak=" + objectStorageConfiguration.getAccessKeyId());
            switch (cloudProvider) {
                case ALIBABA_CLOUD:
                    try {
                        return createAlibabaCloudClient(objectStorageConfiguration, ossSupplier.get());
                    } catch (ClientException e) {
                        throw new RuntimeException("Create Alibaba Cloud Client failed", e);
                    }
                case AWS:
                    return createAmazonCloudClient(objectStorageConfiguration);
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
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .withClientConfiguration(clientConfiguration)
                .disableChunkedEncoding()
                .build();
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
