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
package com.oceanbase.odc.service.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@RefreshScope
@Configuration
public class SessionProperties {

    @Value("${odc.session.sql-execute.oracle.remove-comment-prefix:false}")
    private boolean oracleRemoveCommentPrefix = false;

    @Value("${odc.session.sql-execute.result-set.max-cached-lines:10000}")
    private int resultSetMaxCachedLines = 10000;

    /**
     * 1 GB
     */
    @Value("${odc.session.sql-execute.result-set.max-cached-size:1073741824}")
    private long resultSetMaxCachedSize = 1024 * 1024 * 1024L;

    /**
     * The maximum size limit of the result set, with a default of -1, indicating no restriction
     */
    @Value(("${odc.session.sql-execute.result-set.max-query-size:-1}"))
    private long resultSetMaxQuerySize = -1;

    /**
     * 查询结果集最大行数，默认 100000
     */
    @Value("${odc.session.sql-execute.max-result-set-rows:100000}")
    private long resultSetMaxRows = 100000;

    /**
     * 查询结果集默认行数，默认 1000 行
     */
    @Value("${odc.session.sql-execute.default-result-set-rows:1000}")
    private long resultSetDefaultRows = 1000;

    /**
     * 最大独立 session 数，默认 -1，表示不限制
     */
    @Value("${odc.session.sql-execute.max-single-session-count:-1}")
    private long singleSessionMaxCount = -1;

    /**
     * 允许创建 Session 的最大用户数，默认 -1，表示不限制
     */
    @Value("${odc.session.sql-execute.user-max-count:-1}")
    private long userMaxCount = -1;

    /**
     * 单次执行的最大 SQL 语句长度，默认值 -1， <=0 表示不限制
     */
    @Value("${odc.session.sql-execute.max-sql-length:0}")
    private long maxSqlLength = -1;

    /**
     * 单次执行的最大 SQL 语句数量，默认值 -1， <=0 表示不限制
     */
    @Value("${odc.session.sql-execute.max-sql-statement-count:0}")
    private long maxSqlStatementCount = -1;

    /**
     * dbms 输出最大的行数
     */
    @Value("${odc.session.sql-execute.dbms-output-max-rows:1000}")
    private int dbmsOutputMaxRows = 1000;

    /**
     * 当前 database 最大表数量，默认值 -1， <=0 表示不限制
     */
    @Value("${odc.session.database.max-table-count:0}")
    private long maxTableCount = -1;

    /**
     * 当前 database 最大表分区数量，默认值 -1， <=0 表示不限制
     */
    @Value("${odc.session.database.max-table-partition-count:0}")
    private long maxTablePartitionCount = -1;

    /**
     * 当前 database 最大数据空间占用，单位字节，默认值 -1， <=0 表示不限制
     */
    @Value("${odc.session.database.max-data-size:0}")
    private long maxDataSize = -1;

    /**
     * 当前 database 最大 MemStore 空间占用，单位字节，默认值 -1， <=0 表示不限制
     */
    @Value("${odc.session.database.max-memstore-size:0}")
    private long maxMemStoreSize = -1;

    /**
     * ODC 会话后台连接的查询超时时间，单位为微秒，默认 60 秒
     */
    @Value("${odc.session.sql-execute.backend-query-timeout-micros:60000000}")
    private long backendQueryTimeoutMicros = 60000000;

    /**
     * ODC 会话超时时间，默认 480 分钟，一个会话超过 480 分钟没有被使用就会被回收
     */
    @Value("${odc.session.timeout-mins:480}")
    private long timeoutMins = 480;

    /**
     * 用于控制用户在 SQL 执行时是否改写增加 rowid 列
     */
    @Value("${odc.session.sql-execute.add-internal-rowid:true}")
    private boolean addInternalRowId = true;

    /**
     * 是否开启全链路诊断功能
     */
    @Value("${odc.session.full-link-trace.enabled:true}")
    private boolean enableFullLinkTrace = true;

    /**
     * Timeout for querying full link trace
     */
    @Value("${odc.session.full-link-trace-timeout-seconds:60}")
    private int fullLinkTraceTimeoutSeconds;

}
