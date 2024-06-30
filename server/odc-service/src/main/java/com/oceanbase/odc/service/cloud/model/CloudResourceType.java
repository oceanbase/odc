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
package com.oceanbase.odc.service.cloud.model;


public enum CloudResourceType {
    /**
     * 不支持
     */
    NONE,

    /**
     * 阿里云对象存储 OSS
     */
    OSS,

    /**
     * AWS 对象存储 S3
     */
    S3,

    /**
     * 华为云对象存储 OBS
     */
    OBS,

    /**
     * 腾讯云对象存储 COS
     */
    COS,

    /**
     * Google Cloud Storage
     */
    GCS
}
