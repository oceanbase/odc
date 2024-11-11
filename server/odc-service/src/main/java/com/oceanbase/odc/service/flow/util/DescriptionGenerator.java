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
package com.oceanbase.odc.service.flow.util;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeDatabase;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeParameters;

/**
 * @Author：tinker
 * @Date: 2023/8/4 11:22
 * @Descripition:
 */
public class DescriptionGenerator {

    public static void generateDescription(CreateFlowInstanceReq req) {
        if (StringUtils.isEmpty(req.getDescription())) {
            // descriptions is recommended for localization.Facilitate fuzzy query
            String descFormat = "[%s]%s.%s";
            if (req.getTaskType() == TaskType.MULTIPLE_ASYNC) {
                MultipleDatabaseChangeParameters parameters = (MultipleDatabaseChangeParameters) req.getParameters();
                List<DatabaseChangeDatabase> databases = parameters.getDatabases();
                String description = databases.stream()
                        .map(db -> String.format(descFormat, db.getEnvironment().getName(),
                                db.getDataSource().getName(), db.getName()))
                        .collect(Collectors.joining(","));
                req.setDescription(description);
            } else {
                req.setDescription(String.format(descFormat,
                        req.getEnvironmentName(), req.getConnectionName(), req.getDatabaseName()));
            }
        }
    }

}
