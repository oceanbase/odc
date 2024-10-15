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
package com.oceanbase.odc.service.loaddata.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.text.MessageFormat;

import javax.validation.constraints.NotBlank;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.s3a.S3AFileSystem;

import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.loaddata.util.CloudProviderUtil;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author chang
 * @date 2024/03/13
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class ObjectStorageConfig {
    private static final String COS_ENDPOINT_PATTERN = "cos.{0}.myqcloud.com";
    private static final String OBS_ENDPOINT_PATTERN = "obs.{0}.myhuaweicloud.com";
    private static final String OSS_ENDPOINT_PATTERN = "oss-{0}.aliyuncs.com";
    private static final String S3_ENDPOINT_GLOBAL_PATTERN = "s3.{0}.amazonaws.com";
    private static final String S3_ENDPOINT_CN_PATTERN = "s3.{0}.amazonaws.com.cn";
    private static final String AZURE_ENDPOINT_PATTERN = "https://{0}.blob.core.windows.net";

    private static final String GCS_ENDPOINT = "storage.googleapis.com";
    protected static final Configuration fsConf = new Configuration();

    static {
        fsConf.set("fs.s3a.impl", S3AFileSystem.class.getName());
        fsConf.set("fs.s3.impl", S3AFileSystem.class.getName());
        fsConf.set("fs.obs.impl", S3AFileSystem.class.getName());
        fsConf.set("fs.oss.impl", S3AFileSystem.class.getName());
        fsConf.set("fs.cos.impl", S3AFileSystem.class.getName());
        fsConf.set("fs.cosn.impl", S3AFileSystem.class.getName());
    }

    /**
     * 云储存类型，可选值：OSS, S3, COS, OBS. 前端无须填写，根据 uri 推断。
     */
    private CloudProvider cloudProvider;

    /**
     * 资源定位符。如 oss://bucket/path/to/file。必填
     */
    protected String objectUri;

    /**
     * 访问密钥ID。必填
     */
    @NotBlank(message = "\"accessKey\" cannot be blank")
    @SensitiveInput
    String accessKey;

    /**
     * 访问密钥。必填
     */
    @NotBlank(message = "\"secretKey\" cannot be blank")
    @SensitiveInput
    String secretKey;

    /**
     * 对应云储存的 region，与 endpoint 二选一即可，若都填写，则以 endpoint 为准
     */
    String region;

    /**
     * 对应云存储的 endpoint。与 region 二选一即可，若都填写，则以 endpoint 为准
     */
    String endpoint;

    /**
     * 存储桶名称。非必填，自动从 objectUrl 里提取
     */
    String bucket;

    /**
     * 对象在存储桶中的键（key）。非必填，自动从 objectUrl 里提取
     */
    String objectKey;

    // 以下字段仅用于临时凭证。
    /**
     * 角色资源名称（ARN），用于角色托管的凭证。非必填
     */
    String roleArn;

    /**
     * 角色会话名称，用于角色托管的凭证。非必填
     */
    String roleSessionName;

    public static ObjectStorageConfig create(String objectUri) {
        return new ObjectStorageConfig(objectUri);
    }

    /**
     * @param objectUri
     */
    private ObjectStorageConfig(String objectUri) {
        this.objectUri = objectUri;
        parseAndFill();
    }

    /**
     * 解析 uri 并填充 bucket, storageType, objectKey 字段。
     */
    private void parseAndFill() {
        URI uri = URI.create(objectUri);
        if (cloudProvider == null) {
            checkArgument(uri.getScheme() != null,
                    "Invalid object uri. It should starts with a protocol prefix. e.g. oss://bucket/path/to/file");
            this.cloudProvider = CloudProviderUtil.fromScheme(uri.getScheme());
        }
        if (bucket == null) {
            this.bucket = uri.getAuthority();
        }
        if (objectKey == null) {
            this.objectKey = StringUtils.removeStart(uri.getPath(), "/");
        }
    }

    /**
     * Return the bucket name.
     *
     * @return String
     */
    public String getBucket() {
        if (bucket != null) {
            return bucket;
        }
        parseAndFill();
        return bucket;
    }

    /**
     * Return the object key.
     *
     * @return String
     */
    public String getObjectKey() {
        if (objectKey != null) {
            return objectKey;
        }
        parseAndFill();
        return this.objectKey;
    }

    public CloudProvider getProviderType() {
        if (cloudProvider != null) {
            return cloudProvider;
        }
        parseAndFill();
        return cloudProvider;
    }

    public String getEndpoint() {
        if (cloudProvider == null) {
            parseAndFill();
        }

        // S3 and other compatible cloud storage types that requires a region to concat the whole endpoint,
        // while azure use the account name as one component in endpoint.
        // GCS need neither region nor access key to concat the endpoint.
        if (cloudProvider == CloudProvider.AZURE) {
            this.endpoint = concatEndpoint(cloudProvider, accessKey);
        } else if (cloudProvider == CloudProvider.GOOGLE_CLOUD) {
            this.endpoint = GCS_ENDPOINT;
        } else if (region != null) {
            this.endpoint = concatEndpoint(cloudProvider, region);
        }
        return endpoint;
    }

    public Configuration newFsConf() {
        Configuration conf = new Configuration(fsConf);
        conf.set("fs.s3a.access.key", accessKey);
        conf.set("fs.s3a.secret.key", secretKey);
        conf.set("fs.s3a.endpoint", this.getEndpoint());
        return conf;
    }

    public ObjectStorageConfiguration toObjectStorageConfiguration() {
        ObjectStorageConfiguration config = new ObjectStorageConfiguration();
        config.setPublicEndpoint(this.getEndpoint());
        config.setAccessKeyId(this.getAccessKey());
        config.setAccessKeySecret(this.getSecretKey());
        config.setBucketName(this.getBucket());
        config.setCloudProvider(this.getCloudProvider());
        return config;
    }

    private static String concatEndpoint(CloudProvider cloudProvider, String component) {
        switch (cloudProvider) {
            case TENCENT_CLOUD:
                return MessageFormat.format(COS_ENDPOINT_PATTERN, component);
            case ALIBABA_CLOUD:
                return MessageFormat.format(OSS_ENDPOINT_PATTERN, component);
            case HUAWEI_CLOUD:
                return MessageFormat.format(OBS_ENDPOINT_PATTERN, component);
            case GOOGLE_CLOUD:
                return GCS_ENDPOINT;
            case AZURE:
                return MessageFormat.format(AZURE_ENDPOINT_PATTERN, component);
            case AWS:
                // Note there is a difference of Top-Level Domain between cn and global regions.
                if (component.startsWith("cn-")) {
                    return MessageFormat.format(S3_ENDPOINT_CN_PATTERN, component);
                }
                return MessageFormat.format(S3_ENDPOINT_GLOBAL_PATTERN, component);
            default:
                throw new IllegalArgumentException("concatEndpoint is not applicable for storageType " + cloudProvider);
        }
    }
}
