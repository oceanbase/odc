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
import java.util.List;

import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeDatabase;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author: zijia.cj
 * @date: 2024/4/30
 */
@Getter
@Setter
@ToString
public class MultipleSqlCheckTaskResult implements Serializable, FlowTaskResult {
    private static final long serialVersionUID = -1410986697860096629L;
    private List<SqlCheckTaskResult> sqlCheckTaskResultList;
    private List<DatabaseChangeDatabase> databaseList;
    private boolean success;
    private Integer issueCount;
    private Integer maxLevel;
    private String error;
    private String fileName;
}
