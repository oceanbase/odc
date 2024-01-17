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
package com.oceanbase.odc.service.task.executor.logger;

import java.io.IOException;
import java.util.Map;

import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
public interface LogBiz {

    String getLog(Long jobId, String logType, Long fetchMaxLine, Long fetchMaxByteSize);

    Map<String, String> uploadLogFileToCloudStorage(JobIdentity ji, CloudObjectStorageService cloudObjectStorageService)
            throws IOException;

}
