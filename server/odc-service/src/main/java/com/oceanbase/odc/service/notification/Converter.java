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
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.metadb.notification.NotificationChannelRelationEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyChannelRelationRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.service.notification.helper.ChannelMapper;
import com.oceanbase.odc.service.notification.helper.MessageTemplateProcessor;
import com.oceanbase.odc.service.notification.helper.NotificationPolicyFilter;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.Notification;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 14:45
 * @Description: []
 */
@Service
@SkipAuthorize("currently not in use")
public class Converter {
    @Autowired
    private NotificationPolicyRepository notificationPolicyRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private NotificationPolicyChannelRelationRepository policyChannelRepository;

    @Autowired
    private ChannelMapper channelMapper;

    @Autowired
    private NotificationProperties notificationProperties;

    public List<Notification> convert(List<Event> events) {
        List<Notification> notifications = new ArrayList<>();
        if (CollectionUtils.isEmpty(events)) {
            return notifications;
        }

        List<NotificationPolicyEntity> policies = notificationPolicyRepository.findByOrganizationIds(
                events.stream().map(Event::getOrganizationId).collect(Collectors.toSet()));
        if (CollectionUtils.isEmpty(policies)) {
            return notifications;
        }
        Map<Long, List<NotificationPolicyEntity>> mappedPolicies = policies.stream()
                .collect(Collectors.groupingBy(NotificationPolicyEntity::getOrganizationId));

        List<NotificationChannelRelationEntity> policyChannelEntities =
                policyChannelRepository.findByNotificationPolicyIds(
                        policies.stream().map(NotificationPolicyEntity::getId).collect(Collectors.toSet()));
        if (CollectionUtils.isEmpty(policyChannelEntities)) {
            return notifications;
        }

        Map<Long, List<Channel>> mappedChannels = channelRepository.findByIdIn(
                policyChannelEntities.stream()
                        .map(NotificationChannelRelationEntity::getChannelId).collect(Collectors.toSet()))
                .stream().map(entity -> channelMapper.fromEntity(entity))
                .collect(Collectors.groupingBy(channel -> {
                    for (NotificationChannelRelationEntity entity : policyChannelEntities) {
                        if (Objects.equals(entity.getChannelId(), channel.getId())) {
                            return entity.getNotificationPolicyId();
                        }
                    }
                    return null;
                }));

        for (Event event : events) {
            List<NotificationPolicyEntity> matched = NotificationPolicyFilter.filter(event.getLabels(),
                    mappedPolicies.get(event.getOrganizationId()));
            if (matched.isEmpty()) {
                continue;
            }
            for (NotificationPolicyEntity policy : matched) {
                List<Channel> channels = mappedChannels.get(policy.getId());

                if (CollectionUtils.isEmpty(channels)) {
                    return null;
                }
                channels.forEach(channel -> {
                    Notification notification = new Notification();

                    Message message = new Message();
                    if (Objects.nonNull(channel.getChannelConfig())) {
                        message.setTitle(MessageTemplateProcessor
                                .replaceVariables(channel.getChannelConfig().getTitleTemplate(), event.getLabels()));
                        message.setContent(MessageTemplateProcessor
                                .replaceVariables(channel.getChannelConfig().getContentTemplate(), event.getLabels()));
                    }
                    message.setOrganizationId(policy.getOrganizationId());
                    message.setCreatorId(event.getCreatorId());
                    message.setChannel(channel);
                    message.setStatus(MessageSendingStatus.CREATED);
                    message.setRetryTimes(0);
                    message.setProjectId(channel.getProjectId());
                    message.setMaxRetryTimes(notificationProperties.getMaxResendTimes());
                    notification.setMessage(message);
                    notifications.add(notification);
                });
            }
        }
        return notifications;
    }
}
