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

import lombok.Data;

/**
 * @description: This model is a mapping of internal dictionary view in oceanbase, recording the
 *               refresh information of the specified materialized view
 * 
 *               <pre>
 * obclient(root@mysql)[zijia]> desc  oceanbase.DBA_MVREF_STATS;
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
 *               </pre>
 *
 * @author: zijia.cj
 * @date: 2025/3/25 14:37
 * @since: 4.3.4
 */
@Data
public class DBMViewRefreshRecord {
    private String mvOwner;
    private String mvName;
    private Long refreshId;
    private String refreshMethod;
    private String refreshOptimizations;
    private String additionalExecutions;
    private Date startTime;
    private Date endTime;
    private Long elapsedTime;
    private Long logSetupTime;
    private Long logPurgeTime;
    private Long initialNumRows;
    private Long finalNumRows;

}
