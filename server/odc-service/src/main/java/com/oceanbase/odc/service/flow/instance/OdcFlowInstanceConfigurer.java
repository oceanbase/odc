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
package com.oceanbase.odc.service.flow.instance;

import com.oceanbase.odc.core.flow.ExecutionConfigurer;
import com.oceanbase.odc.core.flow.builder.ErrorBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.FlowableProcessBuilder;
import com.oceanbase.odc.core.flow.builder.TimerBoundaryEventBuilder;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.listener.PreCheckServiceTaskFailedListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskCancelledListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskExecutingCompleteListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskExpiredListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskFailedListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskPendingExpiredListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskPendingListener;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@link OdcFlowInstanceConfigurer}, due to the particularity of the odc business, needs to be
 * customized here, so a separate class is extracted for expansion
 *
 * @author yh263208
 * @date 2022-02-27 14:46
 * @since ODC_release_3.3.0
 * @see FlowInstanceConfigurer
 */
@Slf4j
public class OdcFlowInstanceConfigurer extends FlowInstanceConfigurer {

    /**
     * This constructor can not be invoked by user
     *
     * @param flowInstance target {@link OdcFlowInstance}
     */
    protected OdcFlowInstanceConfigurer(@NonNull OdcFlowInstance flowInstance,
            @NonNull FlowableProcessBuilder targetProcessBuilder, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull ProcessNodeBuilderAccessor accessor) {
        super(flowInstance, targetProcessBuilder, flowableAdaptor, accessor);
    }

    /**
     * This constructor can not be invoked by user
     *
     * @param flowInstance target {@link OdcFlowInstance}
     */
    protected OdcFlowInstanceConfigurer(@NonNull OdcFlowInstance flowInstance,
            @NonNull FlowableProcessBuilder targetProcessBuilder, @NonNull ExecutionConfigurer targetExecution,
            @NonNull FlowableAdaptor flowableAdaptor, @NonNull ProcessNodeBuilderAccessor accessor) {
        super(flowInstance, targetProcessBuilder, targetExecution, flowableAdaptor, accessor);
    }

    @Override
    public FlowInstanceConfigurer next(@NonNull FlowTaskInstance nextNode) {
        if (nextNode.getTaskType() == TaskType.GENERATE_ROLLBACK) {
            // sql-check/generate-rollback 任务不能阻塞正常的任务流程，这里做定制化处理：即使 sql-check/generate-rollback 任务失败流程依然会继续下去
            // sql-check/generate-rollback 任务默认为自动执行，不存在手动执行和定时执行，因此也不必要设置
            return next(nextNode, s -> s.setAsynchronous(true), u -> {
            }, u -> {
            });
        } else if (nextNode.getTaskType() == TaskType.SQL_CHECK
                || nextNode.getTaskType() == TaskType.PRE_CHECK) {
            return next(nextNode, s -> {
                s.setAsynchronous(true);
                ErrorBoundaryEventBuilder failedErrBuilder =
                        setHandleableError(nextNode, s, ErrorCodes.FlowTaskInstanceFailed);
                failedErrBuilder.addExecutionListener(PreCheckServiceTaskFailedListener.class);
            }, u -> {
            }, u -> {
            });
        }
        return next(nextNode, serviceTaskBuilder -> {
            serviceTaskBuilder.addExecutionListener(ServiceTaskExecutingCompleteListener.class);
            serviceTaskBuilder.setAsynchronous(true);
            ErrorBoundaryEventBuilder cancelErrBuilder =
                    setHandleableError(nextNode, serviceTaskBuilder, ErrorCodes.FlowTaskInstanceCancelled);
            cancelErrBuilder.addExecutionListener(ServiceTaskCancelledListener.class);
            ErrorBoundaryEventBuilder failedErrBuilder =
                    setHandleableError(nextNode, serviceTaskBuilder, ErrorCodes.FlowTaskInstanceFailed);
            failedErrBuilder.addExecutionListener(ServiceTaskFailedListener.class);
            ErrorBoundaryEventBuilder expiredErrBuilder =
                    setHandleableError(nextNode, serviceTaskBuilder, ErrorCodes.FlowTaskInstanceExpired);
            expiredErrBuilder.addExecutionListener(ServiceTaskExpiredListener.class);
        }, userTaskBuilder -> {
            userTaskBuilder.addExecutionListener(ServiceTaskPendingListener.class);
            int waitExecExpireIntervalSeconds = nextNode.getStrategyConfig().getPendingExpireIntervalSeconds();
            if (waitExecExpireIntervalSeconds <= 0) {
                return;
            }
            TimerBoundaryEventBuilder timerBuilder =
                    setExpireSeconds(nextNode, userTaskBuilder, waitExecExpireIntervalSeconds);
            timerBuilder.addExecutionListener(ServiceTaskPendingExpiredListener.class);
        }, userTimerTaskBuilder -> {
            userTimerTaskBuilder.addExecutionListener(ServiceTaskPendingListener.class);
        });
    }

}
