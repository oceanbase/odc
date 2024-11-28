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
package com.oceanbase.odc;

import java.util.Map;

import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.test.tool.EncryptableConfigurations;

/**
 * integration test configurations
 */
public class ITConfigurations {
    private static final String CONFIG_FILE = "src/test/resources/integration-test.properties";
    private static final Map<String, String> properties = EncryptableConfigurations.loadProperties(CONFIG_FILE);

    public static String getOb311OracleCommandLine() {
        return get("odc.ob.311.oracle.commandline");
    }

    public static ObjectStorageConfiguration getOssConfiguration() {
        ObjectStorageConfiguration configuration = new ObjectStorageConfiguration();
        configuration.setCloudProvider(CloudProvider.ALIBABA_CLOUD);
        configuration.setRegion(get("odc.cloud.object-storage.oss.region"));
        configuration.setPublicEndpoint(get("odc.cloud.object-storage.oss.endpoint"));
        configuration.setAccessKeyId(get("odc.cloud.object-storage.oss.access-key-id"));
        configuration.setAccessKeySecret(get("odc.cloud.object-storage.oss.access-key-secret"));
        configuration.setBucketName(get("odc.cloud.object-storage.oss.bucket-name"));
        configuration.setRoleArn(get("odc.cloud.object-storage.oss.role-arn"));
        configuration.setRoleSessionName(get("odc.cloud.object-storage.oss.role-session-name"));
        return configuration;
    }

    public static ObjectStorageConfiguration getS3Configuration() {
        ObjectStorageConfiguration configuration = new ObjectStorageConfiguration();
        configuration.setCloudProvider(CloudProvider.AWS);
        configuration.setRegion(get("odc.cloud.object-storage.s3.region"));
        configuration.setPublicEndpoint(get("odc.cloud.object-storage.s3.endpoint"));
        configuration.setAccessKeyId(get("odc.cloud.object-storage.s3.access-key-id"));
        configuration.setAccessKeySecret(get("odc.cloud.object-storage.s3.access-key-secret"));
        configuration.setBucketName(get("odc.cloud.object-storage.s3.bucket-name"));
        configuration.setRoleArn(get("odc.cloud.object-storage.s3.role-arn"));
        configuration.setRoleSessionName(get("odc.cloud.object-storage.s3.role-session-name"));
        return configuration;
    }

    public static ObjectStorageConfiguration getCOSConfiguration() {
        ObjectStorageConfiguration configuration = new ObjectStorageConfiguration();
        configuration.setCloudProvider(CloudProvider.TENCENT_CLOUD);
        configuration.setRegion(get("odc.cloud.object-storage.cos.region"));
        configuration.setPublicEndpoint(get("odc.cloud.object-storage.cos.endpoint"));
        configuration.setAccessKeyId(get("odc.cloud.object-storage.cos.access-key-id"));
        configuration.setAccessKeySecret(get("odc.cloud.object-storage.cos.access-key-secret"));
        configuration.setBucketName(get("odc.cloud.object-storage.cos.bucket-name"));
        configuration.setRoleArn(get("odc.cloud.object-storage.cos.role-arn"));
        configuration.setRoleSessionName(get("odc.cloud.object-storage.cos.role-session-name"));
        return configuration;
    }

    public static ObjectStorageConfiguration getOBSConfiguration() {
        ObjectStorageConfiguration configuration = new ObjectStorageConfiguration();
        configuration.setCloudProvider(CloudProvider.HUAWEI_CLOUD);
        configuration.setRegion(get("odc.cloud.object-storage.obs.region"));
        configuration.setPublicEndpoint(get("odc.cloud.object-storage.obs.endpoint"));
        configuration.setAccessKeyId(get("odc.cloud.object-storage.obs.access-key-id"));
        configuration.setAccessKeySecret(get("odc.cloud.object-storage.obs.access-key-secret"));
        configuration.setBucketName(get("odc.cloud.object-storage.obs.bucket-name"));
        configuration.setRoleArn(get("odc.cloud.object-storage.obs.role-arn"));
        configuration.setRoleSessionName(get("odc.cloud.object-storage.obs.role-session-name"));
        return configuration;
    }

    public static ObjectStorageConfiguration getGCSConfiguration() {
        ObjectStorageConfiguration configuration = new ObjectStorageConfiguration();
        configuration.setCloudProvider(CloudProvider.GOOGLE_CLOUD);
        configuration.setPublicEndpoint(get("odc.cloud.object-storage.gcs.endpoint"));
        configuration.setAccessKeyId(get("odc.cloud.object-storage.gcs.access-key-id"));
        configuration.setAccessKeySecret(get("odc.cloud.object-storage.gcs.access-key-secret"));
        configuration.setBucketName(get("odc.cloud.object-storage.gcs.bucket-name"));
        configuration.setRoleArn(get("odc.cloud.object-storage.gcs.role-arn"));
        configuration.setRoleSessionName(get("odc.cloud.object-storage.gcs.role-session-name"));
        return configuration;
    }

    public static OnlineSchemaChangeProperties getOscPrivateCloudProperties() {
        OnlineSchemaChangeProperties configuration = new OnlineSchemaChangeProperties();
        OmsProperties omsConfiguration = new OmsProperties();
        omsConfiguration.setUrl(get("odc.osc.oms.url"));
        omsConfiguration.setAuthorization(get("odc.osc.oms.authorization"));
        omsConfiguration.setRegion(get("odc.osc.oms.region"));
        configuration.setOms(omsConfiguration);
        return configuration;
    }

    private static String get(String key) {
        return properties.get(key);
    }

}
