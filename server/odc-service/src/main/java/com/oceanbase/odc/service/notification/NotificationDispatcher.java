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

import static com.oceanbase.odc.service.notification.model.RateLimitConfig.OverLimitStrategy.THROWN;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.notification.MessageRepository;
import com.oceanbase.odc.metadb.notification.MessageSendingHistoryEntity;
import com.oceanbase.odc.metadb.notification.MessageSendingHistoryRepository;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendResult;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.RateLimitConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 14:45
 * @Description: []
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class NotificationDispatcher {
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private MessageSendingHistoryRepository sendingHistoryRepository;
    @Autowired
    private MessageSenderMapper messageSenderMapper;

    public void dispatch(Message message) throws Exception {
        Channel channel = message.getChannel();
        Verify.notNull(channel.getChannelConfig(), "channel.config");

        RateLimitConfig rateLimitConfig = channel.getChannelConfig().getRateLimitConfig();
        if (Objects.nonNull(rateLimitConfig) && isChannelRestricted(channel.getId(), rateLimitConfig)) {
            MessageSendingStatus status = rateLimitConfig.getOverLimitStrategy() == THROWN ? MessageSendingStatus.THROWN
                    : MessageSendingStatus.CREATED;
            log.info("channel with id={} is restricted, the message with id={} will be converted into {}",
                    channel.getId(), message.getId(), status);
            messageRepository.updateStatusById(message.getId(), status);
            return;
        }

        MessageSender sender = messageSenderMapper.get(channel);
        MessageSendResult result = sender.send(message);
        if (result.isActive()) {
            messageRepository.updateStatusAndSentTimeById(message.getId(), MessageSendingStatus.SENT_SUCCESSFULLY);
            sendingHistoryRepository
                    .save(new MessageSendingHistoryEntity(message.getId(), MessageSendingStatus.SENT_SUCCESSFULLY));
        } else {
            messageRepository.updateStatusAndRetryTimesAndErrorMessageById(message.getId(),
                    MessageSendingStatus.SENT_FAILED, result.getErrorMessage());
            sendingHistoryRepository.save(new MessageSendingHistoryEntity(message.getId(),
                    MessageSendingStatus.SENT_FAILED, result.getErrorMessage()));
        }
    }

    private boolean isChannelRestricted(Long channelId, RateLimitConfig rateLimitConfig) {
        long time = rateLimitConfig.getTime() <= 0 ? 1 : rateLimitConfig.getTime();
        int sendingTimes = sendingHistoryRepository.countMessageSendingTimesByChannelId(channelId,
                rateLimitConfig.getTimeUnit().toMinutes(time));
        return rateLimitConfig.getLimit() <= sendingTimes;
    }
}
