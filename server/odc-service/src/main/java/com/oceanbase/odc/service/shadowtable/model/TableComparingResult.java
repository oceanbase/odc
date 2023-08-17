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
package com.oceanbase.odc.service.shadowtable.model;

public enum TableComparingResult {
    /**
     * 目标表不存在，需要新建表
     */
    CREATE,

    /**
     * 表结构不一致，需要更新表结构
     */
    UPDATE,

    /**
     * 表结构一致，无需变更
     */
    NO_ACTION,

    /**
     * 等待分析
     */
    WAITING,

    /**
     * 结构对比分析中
     */
    COMPARING,

    /**
     * 跳过分析
     */
    SKIP;

    public boolean isDone() {
        return this != WAITING && this != COMPARING;
    }

    public boolean needsSync() {
        return this == CREATE || this == UPDATE;
    }
}
