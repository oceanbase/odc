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
package com.oceanbase.odc.service.task.executor.task;

import com.oceanbase.odc.common.util.StringUtils;

import lombok.Getter;

/**
 * @author longpeng.zlp
 * @date 2024/10/10 10:37
 */
public enum TaskDescription {
    DLM("DML", "data lifecycle management task"),

    LOAD_DATA("LOAD_DATA", "load data task"),

    LOGICAL_DATABASE_CHANGE("LogicalDatabaseChange", "logic database change task");

    @Getter
    private final String type;
    @Getter
    private final String description;

    TaskDescription(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public boolean matched(String targetType) {
        return StringUtils.equalsIgnoreCase(this.type, targetType);
    }
}
