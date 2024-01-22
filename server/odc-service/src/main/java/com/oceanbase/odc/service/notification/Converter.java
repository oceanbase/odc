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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.metadb.notification.NotificationChannelRelationEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyChannelRelationRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.service.notification.helper.ChannelMapper;
import com.oceanbase.odc.service.notification.helper.MessageTemplateProcessor;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.NotificationPolicy;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 14:45
 * @Description: []
 */
@Component
@Slf4j
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

    @Transactional
    public List<Message> convert(List<Event> events) {
        List<Message> messages = new ArrayList<>();
        if (CollectionUtils.isEmpty(events)) {
            return messages;
        }

        List<NotificationPolicy> policies = events.stream()
                .flatMap(event -> event.getPolicies().stream())
                .collect(Collectors.toList());

        List<NotificationChannelRelationEntity> policyChannelEntities =
                policyChannelRepository.findByNotificationPolicyIds(
                        policies.stream().map(NotificationPolicy::getId).collect(Collectors.toSet()));
        if (CollectionUtils.isEmpty(policyChannelEntities)) {
            return messages;
        }

        Map<Long, List<Channel>> mappedChannels = channelRepository.findByIdIn(
                policyChannelEntities.stream()
                        .map(NotificationChannelRelationEntity::getChannelId).collect(Collectors.toSet()))
                .stream().map(entity -> channelMapper.fromEntityWithConfig(entity))
                .collect(Collectors.groupingBy(channel -> {
                    for (NotificationChannelRelationEntity entity : policyChannelEntities) {
                        if (Objects.equals(entity.getChannelId(), channel.getId())) {
                            return entity.getNotificationPolicyId();
                        }
                    }
                    return null;
                }));

        for (Event event : events) {
            for (NotificationPolicy policy : event.getPolicies()) {
                List<Channel> channels = mappedChannels.get(policy.getId());
                if (CollectionUtils.isEmpty(channels)) {
                    continue;
                }
                channels.forEach(channel -> {
                    try {
                        Message message = new Message();
                        if (Objects.nonNull(channel.getChannelConfig())) {
                            Locale locale;
                            try {
                                locale = Locale.forLanguageTag(channel.getChannelConfig().getLanguage());
                            } catch (Exception e) {
                                locale = Locale.getDefault();
                            }
                            message.setTitle(MessageTemplateProcessor.replaceVariables(
                                    channel.getChannelConfig().getTitleTemplate(), locale, event.getLabels()));
                            message.setContent(MessageTemplateProcessor.replaceVariables(
                                    channel.getChannelConfig().getContentTemplate(), locale, event.getLabels()));
                        }
                        message.setOrganizationId(policy.getOrganizationId());
                        message.setCreatorId(event.getCreatorId());
                        message.setChannel(channel);
                        message.setStatus(MessageSendingStatus.CREATED);
                        message.setRetryTimes(0);
                        message.setProjectId(channel.getProjectId());
                        message.setMaxRetryTimes(notificationProperties.getMaxResendTimes());
                        messages.add(message);
                    } catch (Exception e) {
                        log.error("failed to convert event with id={}, channel id={}",
                                event.getId(), channel.getId(), e);
                    }
                });
            }
        }
        log.info("{} events were converted into {} messages", events.size(), messages.size());
        return messages;
    }
}
