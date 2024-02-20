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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Properties for common flow task
 *
 * @author yh263208
 * @date 2022-03-08 14:31
 * @since ODC_release_3.3.0
 */
@Data
@RefreshScope
@Configuration
public class FlowTaskProperties {

    @Value("${odc.task.default-execution-expiration-interval-hours:48}")
    private int defaultExecutionExpirationIntervalHours = 48;

    @Value("${odc.task.default-wait-execution-expiration-interval-hours:48}")
    private int defaultWaitExecutionExpirationIntervalHours = 48;

    @Value("${odc.task.file-expire-hours:336}")
    private int fileExpireHours = 336;

    @Value("${odc.task.async.sql-content-max-length:10485760}")
    private int sqlContentMaxLength = 10485760;

    @Value("${odc.task.async.rollback.total-max-change-lines:1000000}")
    private int totalMaxChangeLines;

    @Value("${odc.task.async.rollback.max-rollback-content-size-bytes:268435456}")
    private long maxRollbackContentSizeBytes;

    @Value("${odc.task.async.index-change-max-timeout-millis:432000000}")
    private long indexChangeMaxTimeoutMillisecond;
}
