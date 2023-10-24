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
package com.oceanbase.odc.service.flow.task.model;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.core.flow.model.TaskParameters;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/9/19 下午2:18
 * @Description: []
 */
@Data
public class ShadowTableSyncTaskParameter implements Serializable, TaskParameters {
    @NotNull
    private Long comparingTaskId;
    @NotNull
    private Long connectionId;
    @JsonIgnore
    private ConnectionConfig connectionConfig;
    @NotEmpty
    private String schemaName;
    private TaskErrorStrategy errorStrategy;
}
