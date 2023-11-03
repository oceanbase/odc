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
import java.util.Collections;
import java.util.List;
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
import com.oceanbase.odc.service.notification.model.ChannelConfig;
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
        if (CollectionUtils.isEmpty(events)) {
            return Collections.emptyList();
        }
        List<Notification> notifications = new ArrayList<>();
        for (Event event : events) {
            List<NotificationPolicyEntity> policies = NotificationPolicyFilter.filter(event.getLabels(),
                    notificationPolicyRepository.findByOrganizationId(event.getOrganizationId()));
            if (policies.isEmpty()) {
                continue;
            }
            for (NotificationPolicyEntity policy : policies) {
                List<NotificationChannelRelationEntity> policyChannelEntity =
                        policyChannelRepository.findByOrganizationIdAndNotificationPolicyId(event.getOrganizationId(),
                                policy.getId());
                List<ChannelConfig> channels = channelRepository.findAllById(
                        policyChannelEntity.stream()
                                .map(NotificationChannelRelationEntity::getChannelId)
                                .collect(Collectors.toSet()))
                        .stream().map(entity -> channelMapper.fromEntity(entity)).collect(Collectors.toList());

                if (CollectionUtils.isEmpty(channels)) {
                    return null;
                }
                channels.forEach(channel -> {
                    Notification notification = new Notification();

                    Message message = new Message();
                    message.setTitle(
                            MessageTemplateProcessor.replaceVariables(policy.getTitleTemplate(), event.getLabels()));
                    message.setContent(
                            MessageTemplateProcessor.replaceVariables(policy.getContentTemplate(), event.getLabels()));
                    message.setOrganizationId(policy.getOrganizationId());
                    message.setCreatorId(event.getCreatorId());
                    message.setEventId(event.getId());
                    message.setChannelId(channel.getId());
                    message.setStatus(MessageSendingStatus.CREATED);
                    message.setRetryTimes(0);
                    message.setMaxRetryTimes(notificationProperties.getMaxResendTimes());
                    message.setToRecipients(policy.getToRecipients());
                    message.setCcRecipients(policy.getCcRecipients());
                    notification.setMessage(message);
                    notifications.add(notification);
                });
            }
        }
        return notifications;
    }
}
