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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.notification.EventEntity;
import com.oceanbase.odc.metadb.notification.EventRepository;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 17:53
 * @Description: []
 */
@Service
@Slf4j
@SkipAuthorize("currently not in use")
public class JdbcEventQueue implements EventQueue {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventMapper eventMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean offer(Event event) {
        eventRepository.save(eventMapper.toEntity(event));
        return true;
    }

    @Override
    public Event peek(EventStatus status) {
        Optional<EventEntity> entityOpt =
                eventRepository.findByStatusForUpdate(status);
        return entityOpt.map(entity -> eventMapper.fromEntity(entity)).orElseGet(null);
    }

    @Override
    @Transactional
    public List<Event> peek(int batchSize, EventStatus status) {
        List<Event> events = eventRepository.findNByStatusForUpdate(status, batchSize)
                .stream().map(entity -> eventMapper.fromEntity(entity)).collect(Collectors.toList());
        eventRepository.updateStatusByIds(EventStatus.CONVERTING,
                events.stream().map(Event::getId).collect(Collectors.toSet()));
        if (CollectionUtils.isNotEmpty(events)) {
            log.info("poll {} events finished.", events.size());
        }
        return events;
    }

    @Override
    public int size() {
        return eventRepository.findAll().size();
    }
}
