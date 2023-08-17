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

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.service.flow.ActiveTaskAccessor;
import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ActiveTaskStatisticsListener}
 *
 * @author yh263208
 * @date 2022-04-01 12:25
 * @since ODC_release_3.3.0
 * @see AbstractEventListener
 */
@Slf4j
public class ActiveTaskStatisticsListener extends AbstractEventListener<ActiveTaskStatisticsEvent> {

    private final BaseRuntimeFlowableDelegate<?> serviceTask;

    public ActiveTaskStatisticsListener(@NonNull BaseRuntimeFlowableDelegate<?> serviceTask) {
        this.serviceTask = serviceTask;
    }

    @Override
    public void onEvent(ActiveTaskStatisticsEvent event) {
        ActiveTaskAccessor activeTaskAccessor = event.getActiveTaskAccessor();
        if (activeTaskAccessor == null) {
            log.warn("ActiveTaskAccessor is null, unknown error");
            return;
        }
        activeTaskAccessor.addActiveTask(serviceTask);
    }

}
