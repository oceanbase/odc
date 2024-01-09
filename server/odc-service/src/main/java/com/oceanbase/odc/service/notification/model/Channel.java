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
package com.oceanbase.odc.service.notification.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/4
 */
@Data
public class Channel {
    private Long id;
    private String name;
    private Long creatorId;
    private String creatorName;
    private Long organizationId;
    private Long projectId;
    private ChannelType type;
    private String description;
    private Date createTime;
    private Date updateTime;
    @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = DingTalkChannelConfig.class, name = "DingTalk"),
            @JsonSubTypes.Type(value = WebhookChannelConfig.class, name = "Feishu"),
            @JsonSubTypes.Type(value = WeComChannelConfig.class, name = "WeCom"),
            @JsonSubTypes.Type(value = WebhookChannelConfig.class, name = "Webhook")
    })
    private BaseChannelConfig channelConfig;
}
