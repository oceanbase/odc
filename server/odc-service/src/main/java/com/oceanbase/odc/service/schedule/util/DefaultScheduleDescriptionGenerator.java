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
package com.oceanbase.odc.service.schedule.util;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;

/**
 * @Author：tinker
 * @Date: 2024/11/11 11:35
 * @Descripition:
 */
public class DefaultScheduleDescriptionGenerator implements ScheduleDescriptionGenerator {
    @Override
    public void generateScheduleDescription(ScheduleChangeParams req) {
        if (StringUtils.isEmpty(req.getCreateScheduleReq().getDescription())) {
            String environmentName = req.getEnvironmentName();
            String connectionName = req.getConnectionName();
            String databaseName = req.getDatabaseName();
            String description =
                    StringUtils.isEmpty(connectionName) ? String.format("[%s]%s", environmentName, databaseName)
                            : String.format("[%s]%s.%s", environmentName, connectionName, databaseName);
            req.getCreateScheduleReq().setDescription(description);
        }
    }
}
