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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.constants.JobConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-24
 * @since 4.2.4
 */
@Slf4j
public class PrepareCallJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobContext jc = JsonUtils.fromJson(
                context.getMergedJobDataMap().getString(JobConstants.QUARTZ_DATA_MAP_JOB_CONTEXT),
                DefaultJobContext.class);
        try {
            JobConfigurationHolder.getJobConfiguration().getJobDispatcher().start(jc);
        } catch (JobException e) {
            throw new JobExecutionException(e);
        }
    }
}
