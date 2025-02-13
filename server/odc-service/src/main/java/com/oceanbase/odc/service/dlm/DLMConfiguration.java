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
package com.oceanbase.odc.service.dlm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.tools.migrator.common.enums.ShardingStrategy;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/11 17:51
 * @Descripition:
 */

@Slf4j
@Getter
@Configuration
public class DLMConfiguration {

    @Value("${odc.task.dlm.single-task-read-write-ratio:0.5}")
    private double readWriteRatio;

    @Value("${odc.task.dlm.single-task-thread-pool-size:15}")
    private int singleTaskThreadPoolSize;

    @Value("${odc.task.dlm.task-connection-query-timeout-seconds:180}")
    private int taskConnectionQueryTimeout;

    @Value("${odc.task.dlm.sharding-strategy:FIXED_LENGTH}")
    private ShardingStrategy shardingStrategy;

    @Value("${odc.task.dlm.default-scan-batch-size:10000}")
    private int defaultScanBatchSize;

    @Value("${odc.task.dlm.session-limiting.enabled:true}")
    private boolean sessionLimitingEnabled;

    @Value("${odc.task.dlm.session-limiting-ratio:25}")
    private int sessionLimitingRatio;

}
