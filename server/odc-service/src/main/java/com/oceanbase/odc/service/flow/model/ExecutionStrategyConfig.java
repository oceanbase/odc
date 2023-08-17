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
package com.oceanbase.odc.service.flow.model;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Config for {@link com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy}
 *
 * @author yh263208
 * @date 2022-03-04 13:36
 * @since ODC_release_3.3.0
 */
@Setter
@Getter
@ToString
@EqualsAndHashCode
public class ExecutionStrategyConfig implements Serializable {

    private final FlowTaskExecutionStrategy strategy;
    private final int pendingExpireIntervalSeconds;
    private final Date executionTime;
    public static final int INVALID_EXPIRE_INTERVAL_SECOND = 0;

    private ExecutionStrategyConfig(@NonNull FlowTaskExecutionStrategy strategy, int pendingExpireIntervalSeconds,
            Date executionTime) {
        this.strategy = strategy;
        this.pendingExpireIntervalSeconds = pendingExpireIntervalSeconds;
        this.executionTime = executionTime;
    }

    public static ExecutionStrategyConfig autoStrategy() {
        return new ExecutionStrategyConfig(FlowTaskExecutionStrategy.AUTO, INVALID_EXPIRE_INTERVAL_SECOND, null);
    }

    public static ExecutionStrategyConfig manualStrategy(long expireIntervalTime, @NonNull TimeUnit timeUnit) {
        PreConditions.notNegative(expireIntervalTime, "WaitExecExpireIntervalSeconds");
        int expireIntervalTimeSeconds = (int) TimeUnit.SECONDS.convert(expireIntervalTime, timeUnit);
        Verify.verify(expireIntervalTime > 0, "WaitExecExpireIntervalSeconds is too large " + expireIntervalTime);
        return new ExecutionStrategyConfig(FlowTaskExecutionStrategy.MANUAL, expireIntervalTimeSeconds, null);
    }

    public static ExecutionStrategyConfig timerStrategy(Date dateTime) {
        PreConditions.notNull(dateTime, "executionTime");
        return new ExecutionStrategyConfig(FlowTaskExecutionStrategy.TIMER, INVALID_EXPIRE_INTERVAL_SECOND, dateTime);
    }

    public static ExecutionStrategyConfig from(@NonNull CreateFlowInstanceReq req, int expireIntervalSeconds) {
        if (req.getExecutionStrategy() == FlowTaskExecutionStrategy.TIMER) {
            return timerStrategy(req.getExecutionTime());
        }
        if (req.getExecutionStrategy() == FlowTaskExecutionStrategy.MANUAL) {
            return manualStrategy(expireIntervalSeconds, TimeUnit.SECONDS);
        }
        return autoStrategy();
    }

    public static ExecutionStrategyConfig from(@NonNull ServiceTaskInstanceEntity entity) {
        if (entity.getStrategy() == FlowTaskExecutionStrategy.TIMER) {
            return timerStrategy(entity.getExecutionTime());
        }
        if (entity.getStrategy() == FlowTaskExecutionStrategy.MANUAL) {
            Verify.notNull(entity.getWaitExecExpireIntervalSeconds(), "waitExecExpireIntervalSeconds");
            return manualStrategy(entity.getWaitExecExpireIntervalSeconds(), TimeUnit.SECONDS);
        }
        return autoStrategy();
    }

}
