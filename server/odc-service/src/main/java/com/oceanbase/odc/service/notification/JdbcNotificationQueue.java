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
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.notification.ChannelEntity;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.metadb.notification.MessageEntity;
import com.oceanbase.odc.metadb.notification.MessageRepository;
import com.oceanbase.odc.service.notification.helper.ChannelMapper;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/4/7 16:05
 * @Description: []
 */
@Slf4j
@Service
@SkipAuthorize("currently not in use")
public class JdbcNotificationQueue implements NotificationQueue {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelMapper channelMapper;

    @Override
    @Transactional
    public boolean offer(List<Message> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return true;
        }
        List<MessageEntity> saved = messageRepository.saveAll(messages.stream()
                .map(Message::toEntity).collect(Collectors.toList()));
        log.info("offered {} messages finishied.", saved.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Message> peek(int batchSize, MessageSendingStatus status) {
        List<Message> messages = new ArrayList<>();
        List<MessageEntity> messageEntities = messageRepository.findNByStatusForUpdate(status, batchSize);
        if (CollectionUtils.isEmpty(messageEntities)) {
            return messages;
        }
        messageEntities.forEach(messageEntity -> {
            Optional<ChannelEntity> channelOpt = channelRepository.findById(messageEntity.getChannelId());
            if (!channelOpt.isPresent()) {
                messageRepository.updateStatusById(messageEntity.getId(), MessageSendingStatus.THROWN);
            } else {
                messageRepository.updateStatusById(messageEntity.getId(), MessageSendingStatus.SENDING);
                ChannelEntity channel = channelOpt.get();
                Message message = Message.fromEntity(messageEntity);
                message.setChannel(channelMapper.fromEntityWithConfig(channel));
                messages.add(message);
            }
        });
        return messages;
    }

    @Override
    public int size() {
        return messageRepository.findAll().size();
    }
}
