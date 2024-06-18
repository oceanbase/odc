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
import java.util.Locale;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.Symbols;
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
        Locale locale = LocaleContextHolder.getLocale();
        if (StringUtils.isEmpty(req.getDescription())) {
            // descriptions is recommended for localization.Facilitate fuzzy query
            String descFormat = Symbols.LEFT_BRACKET.getLocalizedMessage() + "%s"
                    + Symbols.RIGHT_BRACKET.getLocalizedMessage() + "%s.%s";
            if (req.getTaskType() == TaskType.MULTIPLE_ASYNC) {
                MultipleDatabaseChangeParameters parameters = (MultipleDatabaseChangeParameters) req.getParameters();
                List<DatabaseChangeDatabase> databases = parameters.getDatabases();
                String description = databases.stream()
                        .map(db -> String.format(descFormat, localEnvName(db.getEnvironment().getName(), locale),
                                db.getDataSource().getName(), db.getName()))
                        .collect(Collectors.joining(Symbols.COMMA.getLocalizedMessage()));
                req.setDescription(description);
            } else {
                req.setDescription(String.format(descFormat,
                        localEnvName(req.getEnvironmentName(), locale), req.getConnectionName(),
                        req.getDatabaseName()));
            }
        }
    }

    public static String localEnvName(@NotNull String envName, @NotNull Locale locale) {
        String envKey = envName.substring(2, envName.length() - 1);
        return I18n.translate(envKey, null, envKey, locale);
    }

}
