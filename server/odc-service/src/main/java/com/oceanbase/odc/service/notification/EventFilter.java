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
package com.oceanbase.odc.service.notification;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.notification.EventRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.helper.EventUtils;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventStatus;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 14:45
 * @Description: []
 */
@Service
@SkipAuthorize("odc internal usage")
public class EventFilter {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private NotificationPolicyRepository notificationPolicyRepository;

    @Autowired
    private EventMapper eventMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<Event> filter(List<Event> events) {
        List<Event> filtered = new ArrayList<>();
        if (CollectionUtils.isEmpty(events)) {
            return ListUtils.EMPTY_LIST;
        }
        for (Event event : events) {
            String matchExpression = EventUtils.generateMatchExpression(event.getLabels());
            EventStatus status;
            if (notificationPolicyRepository.existsByOrganizationIdAndMatchExpression(event.getOrganizationId(),
                    matchExpression)) {
                status = EventStatus.CONVERTED;
                filtered.add(event);
            } else {
                status = EventStatus.THROWN;
            }
            event.setStatus(status);
            eventRepository.updateStatusById(event.getId(), status);
        }
        return filtered;
    }
}
