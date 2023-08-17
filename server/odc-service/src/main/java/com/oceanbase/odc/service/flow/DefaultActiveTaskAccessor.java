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
package com.oceanbase.odc.service.flow;

import java.util.LinkedList;
import java.util.List;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.service.flow.listener.ActiveTaskStatisticsEvent;
import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;

import lombok.NonNull;

/**
 * {@link DefaultActiveTaskAccessor}
 *
 * @author yh263208
 * @date 2022-08-25 10:35
 * @since ODC_release_3.4.0
 * @see ActiveTaskAccessor
 */
public class DefaultActiveTaskAccessor implements ActiveTaskAccessor {

    private final EventPublisher eventPublisher;
    private final List<BaseRuntimeFlowableDelegate<?>> activeTaskList = new LinkedList<>();

    public DefaultActiveTaskAccessor(@NonNull EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void addActiveTask(@NonNull BaseRuntimeFlowableDelegate<?> task) {
        this.activeTaskList.add(task);
    }

    @Override
    public List<BaseRuntimeFlowableDelegate<?>> getActiveTasks() {
        activeTaskList.clear();
        eventPublisher.publishEvent(new ActiveTaskStatisticsEvent(this));
        List<BaseRuntimeFlowableDelegate<?>> returnVal = new LinkedList<>(activeTaskList);
        activeTaskList.clear();
        return returnVal;
    }

}
