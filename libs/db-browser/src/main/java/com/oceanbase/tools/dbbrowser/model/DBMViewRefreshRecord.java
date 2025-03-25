/*
 * Copyright (c) 2025 OceanBase.
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


import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @description: This model is a mapping of internal dictionary view in oceanbase, recording the refresh of the specified materialized view
 * <pre>
 * obclient(root@mysql)[zijia]> desc oceanbase.DBA_MVREF_STATS;
 * +-----------------------+--------------+------+------+---------+-------+
 * | Field                 | Type         | Null | Key  | Default | Extra |
 * +-----------------------+--------------+------+------+---------+-------+
 * | MV_OWNER              | varchar(128) | NO   |      |         |       |
 * | MV_NAME               | varchar(128) | NO   |      |         |       |
 * | REFRESH_ID            | bigint(20)   | NO   |      | NULL    |       |
 * | REFRESH_METHOD        | varchar(30)  | NO   |      |         |       |
 * | REFRESH_OPTIMIZATIONS | text         | NO   |      |         |       |
 * | ADDITIONAL_EXECUTIONS | text         | NO   |      |         |       |
 * | START_TIME            | datetime     | NO   |      | NULL    |       |
 * | END_TIME              | datetime     | NO   |      | NULL    |       |
 * | ELAPSED_TIME          | bigint(20)   | NO   |      | NULL    |       |
 * | LOG_SETUP_TIME        | bigint(1)    | NO   |      |         |       |
 * | LOG_PURGE_TIME        | bigint(20)   | NO   |      | NULL    |       |
 * | INITIAL_NUM_ROWS      | bigint(20)   | NO   |      | NULL    |       |
 * | FINAL_NUM_ROWS        | bigint(20)   | NO   |      | NULL    |       |
 * +-----------------------+--------------+------+------+---------+-------+
 * <pre/>
 * @author: zijia.cj
 * @date: 2025/3/25 14:37
 * @since: 4.3.4
 */
@Data
public class DBMViewRefreshRecord {
    private String mvOwner;             // MV_OWNER
    private String mvName;              // MV_NAME
    private Long refreshId;             // REFRESH_ID
    private DBMaterializedViewRefreshMethod refreshMethod;       // REFRESH_METHOD
    private String refreshOptimizations;// REFRESH_OPTIMIZATIONS (text)
    private String additionalExecutions;// ADDITIONAL_EXECUTIONS (text)
    private LocalDateTime startTime;    // START_TIME
    private LocalDateTime endTime;      // END_TIME
    private Long elapsedTime;           // ELAPSED_TIME
    private Long logSetupTime;          // LOG_SETUP_TIME
    private Long logPurgeTime;          // LOG_PURGE_TIME
    private Long initialNumRows;        // INITIAL_NUM_ROWS
    private Long finalNumRows;          // FINAL_NUM_ROWS
    public void setRefreshMethod(String refreshMethod) {
        this.refreshMethod = DBMaterializedViewRefreshMethod.getEnumByShowName(refreshMethod);
    }
}
