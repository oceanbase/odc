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
package com.oceanbase.odc.service.objectstorage.cloud.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * Oss region information
 *
 * @author yh263208
 * @date 2021-12-14 11:59
 * @since ODC_release_3.2.3
 * @references https://help.aliyun.com/document_detail/31837.htm?spm=a2c4g.11186623.0.0.243d351e2SBWUh#concept-zt4-cvy-5db
 */
@SuppressWarnings("all")
@Getter
@RefreshScope
@Configuration
@Deprecated
public class OssConfiguration {
    /**
     * String like {@code oss-cn-hangzhou}
     */
    @Value("${odc.oss.region-id:#{null}}")
    private String regionId;

    @Value("${odc.oss.public-endpoint:#{null}}")
    private String publicEndpoint;

    @Value("${odc.oss.inner-endpoint:#{null}}")
    private String internalEndpoint;

    @Value("${odc.oss.arn-role:#{null}}")
    private String arnRole;

    @Value("${odc.oss.bucket-name:#{null}}")
    private String bucketName;

    @Value("${odc.oss.role-session-name:#{null}}")
    private String roleSessionName;

    @Value("${odc.oss.access-key-id:#{null}}")
    private String accessKeyId;

    @Value("${odc.oss.access-key-secret:#{null}}")
    private String accessKeySecret;

    @Value("${odc.oss.secret:#{null}}")
    private String secret;
    /**
     * default: 2 hr
     */
    @Value("${odc.oss.download-url-expiration-interval-seconds:7200}")
    private long downloadUrlExpirationIntervalSeconds = 120 * 60L;

    public String getAccessKeySecret() {
        if (this.accessKeySecret == null) {
            return null;
        }
        if (this.secret == null || "CHANGE_ME".equalsIgnoreCase(secret)) {
            throw new IllegalStateException("Secret is not set");
        }
        return EncryptAlgorithm.AES.decrypt(this.accessKeySecret, this.secret, "UTF-8");
    }

    public String getAccessKeyId() {
        if (this.accessKeyId == null) {
            return null;
        }
        if (this.secret == null || "CHANGE_ME".equalsIgnoreCase(secret)) {
            throw new IllegalStateException("Secret is not set");
        }
        return EncryptAlgorithm.AES.decrypt(this.accessKeyId, this.secret, "UTF-8");
    }

}
