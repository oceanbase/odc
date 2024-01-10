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
package com.oceanbase.odc.service.notification.helper;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.notification.EventEntity;
import com.oceanbase.odc.metadb.notification.EventLabelEntity;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;

/**
 * @Author: Lebie
 * @Date: 2023/3/21 21:13
 * @Description: []
 */
@Component
public class EventMapper {

    public Event fromEntity(EventEntity entity) {
        Event event = new Event();
        event.setId(entity.getId());
        event.setOrganizationId(entity.getOrganizationId());
        event.setCreatorId(entity.getCreatorId());
        event.setStatus(entity.getStatus());
        event.setTriggerTime(entity.getTriggerTime());
        entity.setProjectId(entity.getProjectId());
        if (CollectionUtils.isNotEmpty(entity.getLabels())) {
            event.setLabels(new EventLabels().addLabels(
                    entity.getLabels().stream().collect(HashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()),
                            HashMap::putAll)));
        }
        return event;
    }

    public EventEntity toEntity(Event event) {
        EventEntity entity = new EventEntity();
        entity.setCreatorId(event.getCreatorId());
        entity.setOrganizationId(event.getOrganizationId());
        entity.setStatus(event.getStatus());
        entity.setTriggerTime(event.getTriggerTime());
        entity.setProjectId(event.getProjectId());
        if (Objects.nonNull(event.getLabels())) {
            entity.setLabels(event.getLabels().entrySet().stream().map(entry -> {
                EventLabelEntity labelEntity = new EventLabelEntity();
                labelEntity.setKey(entry.getKey());
                labelEntity.setValue(entry.getValue());
                labelEntity.setEvent(entity);
                return labelEntity;
            }).collect(Collectors.toList()));
        }
        return entity;
    }
}
