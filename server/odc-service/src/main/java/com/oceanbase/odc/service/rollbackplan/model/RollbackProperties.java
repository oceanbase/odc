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
package com.oceanbase.odc.service.rollbackplan.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * {@link RollbackProperties}
 *
 * @author jingtian
 * @date 2023/5/11
 * @since ODC_release_4.2.0
 */
@Data
@Configuration
@RefreshScope
public class RollbackProperties {

    @Value("${odc.rollback.each-sql-max-change-lines:100000}")
    private int eachSqlMaxChangeLines;

    @Value("${odc.rollback.query-data-batch-size:1000}")
    private int queryDataBatchSize;

    @Value("${odc.session.default-time-zone:Asia/Shanghai}")
    private String defaultTimeZone;

    @Value("${odc.task.async.rollback.max-timeout-millisecond:900000}")
    private Long maxTimeoutMillisecond;

    @Value("${odc.task.async.rollback.max-rollback-content-size-bytes:268435456}")
    private long maxRollbackContentSizeBytes;

    @Value("${odc.task.async.rollback.total-max-change-lines:1000000}")
    private int totalMaxChangeLines;

}
