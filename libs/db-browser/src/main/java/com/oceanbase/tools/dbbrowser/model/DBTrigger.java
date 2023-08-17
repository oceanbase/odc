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
package com.oceanbase.tools.dbbrowser.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 触发器创建单元参数，目前仅针对简单触发器，复合触发器oracle 11g支持，但是截止到22x还不支持，因此ODC暂不支持
 *
 * @author yh263208
 * @date 2020-12-03 15:21
 * @since ODC_release_2.4.0
 */
@Getter
@Setter
public class DBTrigger implements DBObject {

    private DBTriggerType triggerType;
    private String triggerName;
    private String owner;
    private DBTriggerMode triggerMode;
    private List<DBTriggerEvent> triggerEvents;
    // 等效于之前的 tableOwner
    private String schemaMode;
    // 等效于之前的 tableName
    private String schemaName;
    private Boolean rowLevel = true;
    private boolean enable;
    private String sqlExpression;
    private String status;
    private String baseObjectType;
    private String ddl;
    private String errorMessage;
    private List<DBTriggerReference> references;

    @Override
    public String name() {
        return this.triggerName;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.TRIGGER;
    }

    public String getTableName() {
        return this.schemaName;
    }

    public String getTableOwner() {
        return this.schemaMode;
    }

    public String getEnableState() {
        return this.enable ? "ENABLED" : "DISABLED";
    }

}
