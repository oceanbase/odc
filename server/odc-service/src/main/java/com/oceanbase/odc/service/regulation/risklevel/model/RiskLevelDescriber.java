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
package com.oceanbase.odc.service.regulation.risklevel.model;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/6/19 13:57
 * @Description: []
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RiskLevelDescriber implements Serializable {

    private static final long serialVersionUID = 1080959497443673210L;

    private String environmentId;

    private String environmentName;

    private String projectName;

    private String sqlCheckResult;

    private String databaseName;

    private String taskType;

    private boolean overLimit;

    public String describe(ConditionExpression expression) {
        if (expression == ConditionExpression.ENVIRONMENT_ID) {
            return StringUtils.isEmpty(this.environmentId) ? StringUtils.EMPTY : this.environmentId;
        } else if (expression == ConditionExpression.ENVIRONMENT_NAME) {
            return StringUtils.isEmpty(this.environmentName) ? StringUtils.EMPTY : this.environmentName;
        } else if (expression == ConditionExpression.PROJECT_NAME) {
            return StringUtils.isEmpty(this.projectName) ? StringUtils.EMPTY : this.projectName;
        } else if (expression == ConditionExpression.SQL_CHECK_RESULT) {
            return StringUtils.isEmpty(this.sqlCheckResult) ? StringUtils.EMPTY : this.sqlCheckResult;
        } else if (expression == ConditionExpression.DATABASE_NAME) {
            return StringUtils.isEmpty(this.databaseName) ? StringUtils.EMPTY : this.databaseName;
        } else if (expression == ConditionExpression.TASK_TYPE) {
            return StringUtils.isEmpty(this.taskType) ? StringUtils.EMPTY : this.taskType;
        } else {
            return StringUtils.EMPTY;
        }
    }

    public static RiskLevelDescriber of(@NonNull Database db, String taskType) {
        RiskLevelDescriber describer = RiskLevelDescriber.builder()
                .databaseName(db.getName())
                .taskType(taskType)
                .build();
        if (Objects.nonNull(db.getEnvironment())) {
            describer.setEnvironmentId(String.valueOf(db.getEnvironment().getId()));
            describer.setEnvironmentName(db.getEnvironment().getName());
        }
        if (Objects.nonNull(db.getProject())) {
            describer.setProjectName(db.getProject().getName());
        }
        return describer;
    }
}
