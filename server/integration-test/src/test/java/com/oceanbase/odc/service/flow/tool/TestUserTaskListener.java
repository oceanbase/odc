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
package com.oceanbase.odc.service.flow.tool;

import org.flowable.task.service.delegate.DelegateTask;

import com.oceanbase.odc.core.flow.BaseTaskListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestUserTaskListener extends BaseTaskListener {

    @Override
    protected void onTaskCreated(DelegateTask delegateTask) {
        log.info("Task Created, taskName={}", delegateTask.getName());
    }

    @Override
    protected void onTaskDeleted(DelegateTask delegateTask) {
        log.info("Task Deleted, taskName={}", delegateTask.getName());
    }

    @Override
    protected void onTaskCompleted(DelegateTask delegateTask) {
        log.info("Task Completed, taskName={}", delegateTask.getName());
    }

    @Override
    protected void onTaskAssigned(DelegateTask delegateTask) {
        log.info("Task Assigned, taskName={}", delegateTask.getName());
    }

}
