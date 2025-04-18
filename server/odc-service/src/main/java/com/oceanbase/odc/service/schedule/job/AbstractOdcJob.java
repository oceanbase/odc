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
package com.oceanbase.odc.service.schedule.job;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.schedule.submitter.JobSubmitter;

/**
 * @Author：tinker
 * @Date: 2025/1/22 15:16
 * @Descripition:
 */
public abstract class AbstractOdcJob implements OdcJob {

    public Long submitToTaskFramework(String parametersJson, String type, Long timeoutMillis, Long srcDatabaseId) {
        Database database =
                SpringContextUtil.getBean(DatabaseService.class).innerDetailForTask(srcDatabaseId);
        CloudProvider cloudProvider = StringUtils.isEmpty(database.getDataSource().getCloudProvider()) ? null
                : CloudProvider.fromValue(database.getDataSource().getCloudProvider());
        return submitToTaskFramework(parametersJson, type, timeoutMillis, cloudProvider,
                database.getDataSource().getRegion());
    }

    public Long submitToTaskFramework(String parametersJson, String type, Long timeoutMillis,
            CloudProvider cloudProvider, String region) {
        return SpringContextUtil.getBean(JobSubmitter.class).submit(parametersJson, type, timeoutMillis,
                cloudProvider, region);
    }
}
