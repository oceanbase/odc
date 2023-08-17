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
package com.oceanbase.odc.core.flow.builder;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.flowable.bpmn.model.Task;

import com.oceanbase.odc.core.shared.constant.ErrorCode;

import lombok.Getter;
import lombok.NonNull;

/**
 * Refer to task instance for flowable
 *
 * @author yh263208
 * @date 2022-01-19 11:19
 * @since ODC_release_3.3.0
 * @see BaseProcessNodeBuilder
 */
public abstract class BaseTaskBuilder<T extends Task> extends BaseProcessNodeBuilder<T> {

    private final List<BaseProcessNodeBuilder<?>> subProcessNodes = new LinkedList<>();
    @Getter
    private final Set<ErrorCode> processedErrorCodes = new HashSet<>();
    public final static String ATTACHED_REF_OBJ_ATTRI_NAME = "__ATTACHED_REF_OBJ_ATTRI_NAME";

    public BaseTaskBuilder(@NonNull String name) {
        super(name);
    }

    public ErrorBoundaryEventBuilder addErrorProcessEvent(@NonNull ErrorCode destErrorCode, boolean cancelActivity) {
        ErrorBoundaryEventBuilder processConfig =
                new ErrorBoundaryEventBuilder(getGraphId() + "-errorevent-" + subProcessNodes.size(), getGraphId(),
                        destErrorCode.code());
        processConfig.setCancelActivity(cancelActivity);
        subProcessNodes.add(processConfig);
        processedErrorCodes.add(destErrorCode);
        return processConfig;
    }

    public TimerBoundaryEventBuilder addTimerEvent(@NonNull Duration duration, boolean cancelActivity) {
        TimerBoundaryEventBuilder config = new TimerBoundaryEventBuilder(
                getGraphId() + "-timerevent-" + subProcessNodes.size(), getGraphId(), duration);
        config.setCancelActivity(cancelActivity);
        subProcessNodes.add(config);
        return config;
    }

    public TimerBoundaryEventBuilder addTimerEvent(@NonNull Date timedate, boolean cancelActivity) {
        TimerBoundaryEventBuilder config = new TimerBoundaryEventBuilder(
                getGraphId() + "-timerevent-" + subProcessNodes.size(), getGraphId(), timedate);
        config.setCancelActivity(cancelActivity);
        subProcessNodes.add(config);
        return config;
    }

    protected void enableBoundaryEvent(@NonNull T target) {
        for (BaseProcessNodeBuilder<?> nodeConfig : subProcessNodes) {
            nodeConfig.setAttribute(ATTACHED_REF_OBJ_ATTRI_NAME, target);
        }
    }

    @Override
    protected void init(@NonNull T target) {
        super.init(target);
        enableBoundaryEvent(target);
    }

    @Override
    public List<BaseProcessNodeBuilder<?>> getSubProcessNodeBuilders() {
        return subProcessNodes;
    }

}
