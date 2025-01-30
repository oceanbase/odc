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

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.schedule.provider.HostUrlProvider;

/**
 * @author yaobin
 * @date 2023-11-30
 * @since 4.2.4
 */
public class DefaultJobContextBuilder implements JobContextBuilder {
    @Override
    public JobContext build(JobIdentity ji, JobDefinition jd) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        DefaultJobContext jobContext = new DefaultJobContext();
        jobContext.setJobIdentity(ji);
        jobContext.setJobClass(jd.getJobClass().getCanonicalName());
        jobContext.setJobParameters(jd.getJobParameters());
        jobContext.setJobProperties(jd.getJobProperties());
        jobContext.setHostUrls(configuration.getHostUrlProvider().hostUrl());
        return jobContext;
    }

    @Override
    public JobContext build(JobEntity jobEntity) {
        return build(jobEntity, JobConfigurationHolder.getJobConfiguration());
    }

    public JobContext build(JobEntity jobEntity, JobConfiguration configuration) {
        DefaultJobContext jobContext = new DefaultJobContext();
        jobContext.setJobIdentity(JobIdentity.of(jobEntity.getId()));
        jobContext.setJobClass(jobEntity.getJobClass());
        jobContext.setJobParameters(JsonUtils.fromJson(jobEntity.getJobParametersJson(),
                new TypeReference<Map<String, String>>() {}));
        jobContext.setJobProperties(jobEntity.getJobProperties());

        HostUrlProvider hostUrlProvider = configuration.getHostUrlProvider();
        jobContext.setHostUrls(hostUrlProvider.hostUrl());
        return jobContext;
    }

}
