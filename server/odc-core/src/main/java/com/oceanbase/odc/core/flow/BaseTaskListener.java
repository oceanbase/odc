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
package com.oceanbase.odc.core.flow;

import java.util.Objects;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

import com.oceanbase.odc.core.flow.util.FlowConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Task Event Listener for Flowable task
 *
 * @author yh263208
 * @date 2022-01-18 21:06
 * @since ODC_release_3.3.0
 */
@Slf4j
public abstract class BaseTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        if (Objects.equals(eventName, FlowConstants.TASK_DELETE_EVENT_NAME)) {
            try {
                onTaskDeleted(delegateTask);
            } catch (Exception e) {
                log.warn("Task delete event listener method failed to execute, eventName={}, taskName={}, taskId={}",
                        eventName, delegateTask.getName(), delegateTask.getId(), e);
            }
        } else if (Objects.equals(eventName, FlowConstants.TASK_COMPLETE_EVENT_NAME)) {
            try {
                onTaskCompleted(delegateTask);
            } catch (Exception e) {
                log.warn("Task complete event listener method failed to execute, eventName={}, taskName={}, taskId={}",
                        eventName, delegateTask.getName(), delegateTask.getId(), e);
            }
        } else if (Objects.equals(eventName, FlowConstants.TASK_CREATE_EVENT_NAME)) {
            try {
                onTaskCreated(delegateTask);
            } catch (Exception e) {
                log.warn("Task create event listener method failed to execute, eventName={}, taskName={}, taskId={}",
                        eventName, delegateTask.getName(), delegateTask.getId(), e);
            }
        } else if (Objects.equals(eventName, FlowConstants.TASK_ASSIGN_EVENT_NAME)) {
            try {
                onTaskAssigned(delegateTask);
            } catch (Exception e) {
                log.warn("Task assigned event listener method failed to execute, eventName={}, taskName={}, taskId={}",
                        eventName, delegateTask.getName(), delegateTask.getId(), e);
            }
        } else {
            log.warn("Invalid event name, eventName={}, taskName={}, taskId={}", eventName, delegateTask.getName(),
                    delegateTask.getId());
        }
    }

    /**
     * Method will be invoked when task is created
     *
     * @param delegateTask {@link DelegateTask}
     */
    protected abstract void onTaskCreated(DelegateTask delegateTask);

    /**
     * Method will be invoked when task is deleted
     *
     * @param delegateTask {@link DelegateTask}
     */
    protected abstract void onTaskDeleted(DelegateTask delegateTask);

    /**
     * Method will be invoked when task is completed
     *
     * @param delegateTask {@link DelegateTask}
     */
    protected abstract void onTaskCompleted(DelegateTask delegateTask);

    /**
     * Method will be invoked when task is assigned
     *
     * @param delegateTask {@link DelegateTask}
     */
    protected abstract void onTaskAssigned(DelegateTask delegateTask);

}
