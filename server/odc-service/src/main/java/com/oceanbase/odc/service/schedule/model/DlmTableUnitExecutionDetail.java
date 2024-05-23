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
package com.oceanbase.odc.service.schedule.model;

import java.util.Date;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.tools.migrator.common.enums.JobType;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/5/16 17:36
 * @Descripition:
 */

@Data
public class DlmTableUnitExecutionDetail {

    private String tableName;

    private String userCondition;

    private Long processedRowCount;

    private Long readRowCount;

    private Long processedRowsPerSecond;

    private Long readRowsPerSecond;

    private Date startTime;

    private Date endTime;

    private JobType type;

    private TaskStatus status;
}
