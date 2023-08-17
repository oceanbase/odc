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
package com.oceanbase.odc.core.migrate;

import java.sql.Timestamp;

import lombok.Data;

/**
 * @author yizhou.xw
 * @version : SchemaHistory.java, v 0.1 2021-03-23 16:26
 */
@Data
public class SchemaHistory {
    private Long installRank;
    private String version;
    private String description;
    private Type type;
    private String script;
    private String checksum;
    private String installedBy;
    private Timestamp installedOn;
    private Long executionMillis;
    private Boolean success;

    public static SchemaHistory fromMigratable(Migrator migratable) {
        SchemaHistory history = new SchemaHistory();
        history.setScript(migratable.script());
        history.setDescription(migratable.description());
        history.setType(migratable.type());
        history.setChecksum(migratable.checksum());
        history.setVersion(migratable.version());
        return history;
    }

}
