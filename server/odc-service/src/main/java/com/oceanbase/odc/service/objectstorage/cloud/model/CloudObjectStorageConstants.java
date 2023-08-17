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

/**
 * Constants for cloud object storage
 *
 * @author yh263208
 * @date 2021-12-14 17:34
 * @since ODC_release_3.2.3
 */
public class CloudObjectStorageConstants {
    /**
     * 临界文件大小，单位为MB，超过此限制就要使用大文件上传和下载
     */
    public static final long CRITICAL_FILE_SIZE_IN_MB = 10;
    /**
     * 服务器分片上传时最大的分片数量
     */
    public static final long MAX_PART_COUNT = 10000;
    /**
     * 分片上传时最小的分片大小，单位为 字节，<br>
     * OSS 最小 100KB， S3 最小 5M，这里统一为 5M
     */
    public static final long MIN_PART_SIZE = 5 * 1024 * 1024L;
    /**
     * 暂存数据存放位置
     */
    public static final String TEMP_DIR = "oss_temp_dir";

    /**
     * 默认临时凭证过期时间，单位为秒
     */
    public static final Long DEFAULT_EXPIRATION_TIME = 15 * 60L;

    /**
     * 进行中转请求的文件前缀
     */

    public static final String ODC_TRANSFER_PREFIX = "ODC-transfer-";

    /**
     * 默认文件前缀
     */
    public static final String ODC_SERVER_PREFIX = "ODC-server-";

    /**
     * 使用 cloud storage 进行中转请求时返回的 header 标头
     */
    public static final String OSS_OBJECT_HEADER = "OssObject";

    /**
     * 用于标识不能被 {@code AliyunHttpBodyFilter} 过滤的 response
     */
    public static final String FILTER_WHITE_LIST_RESPONSE = "FilterWhiteList";

}
