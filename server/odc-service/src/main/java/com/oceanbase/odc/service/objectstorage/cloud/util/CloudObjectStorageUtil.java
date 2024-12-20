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
package com.oceanbase.odc.service.objectstorage.cloud.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.aliyun.oss.common.utils.BinaryUtil;
import com.amazonaws.util.SdkHttpUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Util for cloud object storage
 *
 * @author yh263208
 * @date 2021-12-14 16:11
 * @since ODC_release_3.2.3
 */
@Slf4j
public class CloudObjectStorageUtil {
    /**
     * 默认日期格式
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * generate object name
     *
     * @param taskId task id, can be null
     * @param fileName name of the file, can not be null
     * @param userId user id, can be null
     * @return generated object name
     */
    public static String generateObjectName(String userId, @NonNull String taskId, @NonNull String fileName) {
        return generateObjectName(userId, taskId, CloudObjectStorageConstants.ODC_SERVER_PREFIX, fileName);
    }

    /**
     * generate object name
     *
     * @param taskId task id, can be null
     * @param fileName name of the file, can not be null
     * @param prefix prefix of the file, can not be null
     * @param userId user id, can be null
     * @return generated object name
     */
    public static String generateObjectName(String userId, @NonNull String taskId, @NonNull String prefix,
            @NonNull String fileName) {
        Date date = new Date();
        StringBuilder builder = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH");
        dateFormat.setTimeZone(TimeZone.getDefault());
        String digest = BinaryUtil.toBase64String(BinaryUtil.calculateMd5((userId + taskId).getBytes()));
        digest = digest.replace("/", "_");
        int subPath = Math.abs(fileName.hashCode()) % 16;
        SimpleDateFormat format = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        format.setTimeZone(TimeZone.getDefault());
        builder.append(prefix).append(format.format(date)).append("/")
                .append(StringUtils.isEmpty(userId) ? "" : userId + "/")
                .append(dateFormat.format(date)).append("/")
                .append(digest).append("/")
                .append(subPath).append("/")
                .append(fileName);
        return builder.toString();
    }

    public static String generateOneDayObjectName(@NonNull String fileName) {
        return CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                CloudObjectStorageConstants.TEMP_ONE_DAY_DIR, fileName);
    }

    public static String getOriginalFileName(String objectName) {
        PreConditions.notBlank(objectName, "objectName");
        String[] segment = StringUtils.split(objectName, "/");
        String fileName = segment[segment.length - 1];
        return SdkHttpUtils.urlDecode(fileName);
    }
}
