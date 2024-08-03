/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.projectfiles.model;

import lombok.Data;

/**
 * 文件上传临时凭证响应
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@Data
public class FileUploadTempCredentialResp {
    private String region;
    private String endpoint;
    private String bucket;
    private String bucketEndpoint;
    private String accessKeySecret;
    private String accessKeyId;
    /**
     * 凭证有效期至
     */
    private String expiration;
    private String securityToken;
    /**
     * 上传到OSS文件的objectKey的目录，这个是一个临时目录，在一定时间之后会自动删除
     */
    private String objectKeyDirectory;
}
