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
package com.oceanbase.odc.service.task.executor.context;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @Author: Lebie
 * @Date: 2024/9/24 17:27
 * @Description: []
 */
public class ProcessJobContextProvider implements JobContextProvider {
    @Override
    public JobContext provide() {
        String encryptedJobContextJson;
        try {
            encryptedJobContextJson = FileUtils
                    .readFileToString(new File(System.getProperty(JobEnvKeyConstants.ODC_JOB_CONTEXT_FILE_PATH)),
                            Charset.defaultCharset());
        } catch (Exception ex) {
            throw new RuntimeException("read job context file failed, ex=", ex);
        } finally {
            FileUtils.deleteQuietly(new File(System.getProperty(JobEnvKeyConstants.ODC_JOB_CONTEXT_FILE_PATH)));
        }
        String rawJobContextJson = JobUtils.decrypt(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ENCRYPT_KEY),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ENCRYPT_SALT), encryptedJobContextJson);
        return JsonUtils.fromJson(rawJobContextJson, DefaultJobContext.class);
    }
}
