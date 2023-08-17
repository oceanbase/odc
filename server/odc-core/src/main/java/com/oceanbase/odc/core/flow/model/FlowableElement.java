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
package com.oceanbase.odc.core.flow.model;

import com.oceanbase.odc.core.flow.builder.ErrorBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.SignalCatchEventBuilder;
import com.oceanbase.odc.core.flow.builder.TimerBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.UserTaskBuilder;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * {@link FlowableElementType}
 *
 * @author yh263208
 * @date 2022-02-21 20:32
 * @since ODC_release_3.3.0
 */
@Getter
@ToString
@EqualsAndHashCode
public class FlowableElement {

    private final String activityId;
    private final String name;
    private final FlowableElementType type;

    public FlowableElement(@NonNull String activityId, @NonNull String name, @NonNull FlowableElementType type) {
        this.activityId = activityId;
        this.name = name;
        this.type = type;
    }

    public FlowableElement(@NonNull TimerBoundaryEventBuilder timerBuilder) {
        this(timerBuilder.getGraphId(), timerBuilder.getName(), FlowableElementType.TIMER_BOUNDARY_EVENT);
    }

    public FlowableElement(@NonNull ErrorBoundaryEventBuilder eventBuilder) {
        this(eventBuilder.getGraphId(), eventBuilder.getName(), FlowableElementType.ERROR_BOUNDARY_EVENT);
    }

    public FlowableElement(@NonNull SignalCatchEventBuilder signalCatchEventBuilder) {
        this(signalCatchEventBuilder.getGraphId(), signalCatchEventBuilder.getName(),
                FlowableElementType.SIGNAL_CATCH_EVENT);
    }

    public FlowableElement(@NonNull UserTaskBuilder userTaskBuilder) {
        this(userTaskBuilder.getGraphId(), userTaskBuilder.getName(), FlowableElementType.USER_TASK);
    }

}
