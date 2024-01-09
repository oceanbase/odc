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

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.DingTalkChannelConfig;
import com.oceanbase.odc.service.notification.model.Message;
import com.taobao.api.TaobaoResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 16:10
 * @Description: []
 */
@Slf4j
public class DingTalkBotChannel implements MessageChannel {
    private final String serviceUri;

    public DingTalkBotChannel(Channel channel) {
        DingTalkChannelConfig channelConfig = (DingTalkChannelConfig) channel.getChannelConfig();
        this.serviceUri = channelConfig.getWebhook();
    }

    @Override
    public ChannelType type() {
        return ChannelType.DingTalk;
    }

    @Override
    public boolean send(Message message) {
        DingTalkClient client = new DefaultDingTalkClient(this.serviceUri);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
        request.setAt(at);
        request.setMsgtype("text");
        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        text.setContent(message.getContent());
        request.setText(text);
        TaobaoResponse response;
        try {
            response = client.execute(request);
        } catch (Exception ex) {
            log.warn("call DingTalk Open API failed, ", ex);
            return false;
        }
        if (response.isSuccess()) {
            log.info("DingTalk Bot message sent successfully");
            return true;
        } else {
            log.warn("DingTalk Bot sent failed, errorCode={}, message={}, requestUrl={}, requestId={}",
                    response.getErrorCode(),
                    response.getMessage(), response.getRequestUrl(), response.getRequestId());
            return false;
        }
    }

}
