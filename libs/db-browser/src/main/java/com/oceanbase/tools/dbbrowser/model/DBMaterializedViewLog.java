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

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/7/14 10:24
 * @since: 4.4.0
 */
@Setter
@Getter
public class DBMaterializedViewLog implements DBObject {

    private String mViewLogName;

    private String baseTableName;
    /**
     * The materialized view log and the base table belong to the same schema. if null, use
     * defaultSchemaName in current connection.
     */
    private String schemaName;
    /**
     * The parallelism degree of purging expired data in the materialized view log.
     */
    private Long purgeParallelismDegree;
    /**
     * The configuration for automatic cleaning, if null, indicates that it is not enabled
     */
    private DBMViewLogPurgeSchedule purgeSchedule;

    private Date lastPurgeDate;

    private Boolean includeNewValues;

    @Override
    public String name() {
        return this.mViewLogName;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.MATERIALIZED_VIEW_LOG;
    }
}
