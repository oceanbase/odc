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
package com.oceanbase.odc.service.script.util;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.service.script.model.ScriptConstants;

import cn.hutool.core.lang.Tuple;

/**
 * @Author: Lebie
 * @Date: 2022/3/22 下午6:39
 * @Description: []
 */
public class ScriptUtils {
    private static final String SCRIPT_BATCH_DOWNLOAD_DIRECTORY = "temp_dir/script_download";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String getPersonalBucketName(String userIdStr) {
        PreConditions.notEmpty(userIdStr, "userIdStr");
        return ScriptConstants.SCRIPT_BASE_BUCKET.concat(File.separator).concat(userIdStr);
    }

    public static Tuple getScriptBatchDownloadDirectoryAndZipFile() {
        int hashCode = UUID.randomUUID().hashCode();
        ZonedDateTime nowInShanghai = ZonedDateTime.now(ZoneId.systemDefault());
        String name = String.format("%s_%s", AuditEventAction.DOWNLOAD_SCRIPT.getLocalizedMessage(),
                nowInShanghai.format(FORMATTER));
        return new Tuple(String.format("%s/%s/%s/", SCRIPT_BATCH_DOWNLOAD_DIRECTORY, hashCode, name),
                String.format("%s/%s/%s.zip", SCRIPT_BATCH_DOWNLOAD_DIRECTORY, hashCode, name));
    }

    public static String getScriptBatchDownloadDirectory() {
        return SCRIPT_BATCH_DOWNLOAD_DIRECTORY;
    }
}
