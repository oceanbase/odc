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

import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.request.OapiRobotSendRequest.Markdown;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.DingTalkChannelConfig;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 16:10
 * @Description: []
 */
@Slf4j
@Component("DingTalk")
public class DingTalkSender implements MessageSender {

    @Override
    public ChannelType type() {
        return ChannelType.DingTalk;
    }

    @Override
    public MessageSendResult send(Message message) throws Exception {
        Verify.notEmpty(message.getTitle(), "message.title");
        Verify.notEmpty(message.getContent(), "message.content");

        DingTalkChannelConfig channelConfig = (DingTalkChannelConfig) message.getChannel().getChannelConfig();

        DefaultDingTalkClient client = new DefaultDingTalkClient(channelConfig.getWebhook());
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        if (CollectionUtils.isNotEmpty(channelConfig.getAtMobiles())) {
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            at.setAtMobiles(
                    channelConfig.getAtMobiles().stream().map(Object::toString).collect(Collectors.toList()));
            request.setAt(at);
        }
        request.setMsgtype("markdown");
        Markdown markdown = new Markdown();
        markdown.setTitle(message.getTitle());
        markdown.setText(message.getContent());
        request.setMarkdown(markdown);
        OapiRobotSendResponse response = client.execute(request);
        if (response.isSuccess()) {
            log.info("DingTalk Bot message sent successfully");
            return MessageSendResult.ofSuccess();
        } else {
            log.warn("DingTalk Bot sent failed, errorCode={}, message={}, requestUrl={}, requestId={}",
                    response.getErrorCode(),
                    response.getErrmsg(),
                    response.getRequestUrl(), response.getRequestId());
            return MessageSendResult.ofFail(response.getErrmsg());
        }
    }

}
