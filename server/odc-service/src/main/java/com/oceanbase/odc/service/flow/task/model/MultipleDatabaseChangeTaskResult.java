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

import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangingRecord;

import lombok.Getter;
import lombok.Setter;

/**
 * @author: zijia.cj
 * @date: 2024/4/7
 */
@Getter
@Setter
public class MultipleDatabaseChangeTaskResult implements FlowTaskResult {
    private List<DatabaseChangingRecord> databaseChangingRecordList;
    private boolean success;
}
