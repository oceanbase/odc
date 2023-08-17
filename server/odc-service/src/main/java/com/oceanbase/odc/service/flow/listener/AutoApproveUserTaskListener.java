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
package com.oceanbase.odc.service.flow.listener;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.common.util.RetryExecutor;
import com.oceanbase.odc.service.flow.event.UserTaskCreatedEvent;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link AutoApproveUserTaskListener}
 *
 * @author yh263208
 * @date 2022-08-22 20:21
 * @since ODC_release_3.4.0
 */
@Slf4j
public class AutoApproveUserTaskListener extends AbstractEventListener<UserTaskCreatedEvent> {

    private final ThreadPoolTaskExecutor executorService;
    private final RetryExecutor retryExecutor =
            RetryExecutor.builder().initialDelay(true).retryIntervalMillis(1000).retryTimes(3).build();

    static class AutoApproveTask implements Runnable {

        private final RetryExecutor retryExecutor;
        private final FlowApprovalInstance approvalInstance;

        public AutoApproveTask(FlowApprovalInstance approvalInstance, RetryExecutor retryExecutor) {
            this.retryExecutor = retryExecutor;
            this.approvalInstance = approvalInstance;
        }

        @Override
        public void run() {
            retryExecutor.run(this::approve, r -> r);
        }

        private boolean approve() {
            Long flowInstanceId = approvalInstance.getFlowInstanceId();
            try {
                approvalInstance.approve("system auto-approval", true);
                log.info("Auto-approval succeeded, flowInstanceId={}", flowInstanceId);
                return true;
            } catch (Exception e) {
                log.warn("Auto-approval failed, flowInstanceId={}", flowInstanceId, e);
                return false;
            }
        }
    }

    public AutoApproveUserTaskListener(@NonNull ThreadPoolTaskExecutor executorService) {
        this.executorService = executorService;
    }

    @Override
    public void onEvent(UserTaskCreatedEvent event) {
        FlowApprovalInstance approvalInstance = event.getApprovalInstance();
        if (!approvalInstance.isAutoApprove()) {
            return;
        }
        this.executorService.submit(new AutoApproveTask(approvalInstance, retryExecutor));
    }

}
