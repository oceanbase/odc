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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.TimerEventDefinition;

import lombok.NonNull;

/**
 * Refer to {@link org.flowable.bpmn.model.TimerEventDefinition}
 *
 * @author yh263208
 * @date 2022-01-19 16:07
 * @since ODC-release_3.3.0
 * @see BaseProcessNodeBuilder
 */
public class TimerBoundaryEventBuilder extends BaseBoundaryEventBuilder {

    private final Duration duration;
    private final Date timedate;

    TimerBoundaryEventBuilder(@NonNull String name, @NonNull String attachedToRefId, @NonNull Duration duration) {
        super(name, attachedToRefId);
        this.duration = duration;
        this.timedate = null;
    }

    TimerBoundaryEventBuilder(@NonNull String name, @NonNull String attachedToRefId, @NonNull Date timedate) {
        super(name, attachedToRefId);
        this.timedate = timedate;
        this.duration = null;
    }

    @Override
    public BoundaryEvent build() {
        BoundaryEvent returnValue = new BoundaryEvent();
        init(returnValue);
        return returnValue;
    }

    @Override
    protected void init(@NonNull BoundaryEvent event) {
        super.init(event);
        TimerEventDefinition definition = new TimerEventDefinition();
        if (duration != null) {
            definition.setTimeDuration(duration.toString());
        } else if (timedate != null) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            String timeDateToString = simpleDateFormat.format(timedate);
            definition.setTimeDate(timeDateToString);
        }
        event.setCancelActivity(cancelActivity);
        event.addEventDefinition(definition);
    }

}
