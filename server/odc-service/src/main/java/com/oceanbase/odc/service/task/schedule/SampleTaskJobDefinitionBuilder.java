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

package com.oceanbase.odc.service.task.schedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.executor.sampletask.SampleTask;
import com.oceanbase.odc.service.task.executor.sampletask.SampleTaskParameter;

/**
 * @author yaobin
 * @date 2023-11-30
 * @since 4.2.4
 */
public class SampleTaskJobDefinitionBuilder {

    public JobDefinition build(ConnectionConfig config, String databaseName, List<String> sqls) {
        Map<String, String> jobData = new HashMap<>();
        config.setDefaultSchema(databaseName);
        SampleTaskParameter stp = new SampleTaskParameter();
        stp.setDefaultSchema(databaseName);
        stp.setSqls(sqls);
        jobData.put(JobDataMapConstants.META_DB_TASK_PARAMETER, JsonUtils.toJson(stp));
        jobData.put(JobDataMapConstants.CONNECTION_CONFIG, JobUtils.toJson(config));

        return DefaultJobDefinition.builder()
                .jobClass(SampleTask.class)
                .jobData(jobData)
                .build();
    }
}
