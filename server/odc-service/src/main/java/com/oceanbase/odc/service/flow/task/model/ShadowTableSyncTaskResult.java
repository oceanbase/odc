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

import java.util.List;

import com.oceanbase.odc.core.flow.model.AbstractFlowTaskResult;
import com.oceanbase.odc.service.shadowtable.model.TableSyncExecuteStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: Lebie
 * @Date: 2022/9/19 下午2:19
 * @Description: []
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShadowTableSyncTaskResult extends AbstractFlowTaskResult {
    private Long shadowTableComparingId;
    private List<TableSyncExecuting> tables;

    @Data
    public static class TableSyncExecuting {
        private Long id;
        private String originTableName;
        private String destTableName;
        private TableSyncExecuteStatus status;
    }
}
