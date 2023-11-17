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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.notification.EventRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.helper.NotificationPolicyFilter;
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
            return filtered;
        }
        Set<Long> organizationIds = events.stream().map(Event::getOrganizationId).collect(Collectors.toSet());
        Map<Long, List<NotificationPolicyEntity>> policies = notificationPolicyRepository
                .findByOrganizationIds(organizationIds).stream()
                .collect(Collectors.groupingBy(NotificationPolicyEntity::getOrganizationId));
        List<Long> thrown = new ArrayList<>();
        for (Event event : events) {
            List<NotificationPolicyEntity> matched = NotificationPolicyFilter.filter(event.getLabels(),
                    policies.get(event.getOrganizationId()));
            if (matched.isEmpty()) {
                thrown.add(event.getId());
            } else {
                filtered.add(event);
            }
        }
        if (!CollectionUtils.isEmpty(thrown)) {
            eventRepository.updateStatusByIds(EventStatus.THROWN, thrown);
        }
        if (!CollectionUtils.isEmpty(filtered)) {
            eventRepository.updateStatusByIds(EventStatus.CONVERTED, filtered.stream().map(Event::getId).collect(
                Collectors.toSet()));
        }
        return filtered;
    }
}
