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
package com.oceanbase.odc.service.notification.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.metadb.notification.ChannelEntity;
import com.oceanbase.odc.metadb.notification.ChannelPropertyEntity;
import com.oceanbase.odc.service.notification.model.BaseChannelConfig;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.DingTalkChannelConfig;
import com.oceanbase.odc.service.notification.model.WeComChannelConfig;
import com.oceanbase.odc.service.notification.model.WebhookChannelConfig;

/**
 * @Author: Lebie
 * @Date: 2023/3/21 18:08
 * @Description: []
 */
@Component
public class ChannelMapper {

    public Channel fromEntity(ChannelEntity entity) {
        Channel channel = new Channel();
        channel.setId(entity.getId());
        channel.setName(entity.getName());
        channel.setType(entity.getType());
        channel.setCreatorId(entity.getCreatorId());
        channel.setOrganizationId(entity.getOrganizationId());
        channel.setProjectId(entity.getProjectId());
        channel.setDescription(entity.getDescription());
        channel.setCreateTime(entity.getCreateTime());
        channel.setUpdateTime(entity.getUpdateTime());
        return channel;
    }

    public Channel fromEntityWithConfig(ChannelEntity entity) {
        Channel channel = fromEntity(entity);
        if (CollectionUtils.isNotEmpty(entity.getProperties())) {
            HashMap<String, Object> properties = entity.getProperties().stream()
                    .collect(HashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()), HashMap::putAll);
            Class<? extends BaseChannelConfig> clazz;
            switch (entity.getType()) {
                case DingTalk:
                    clazz = DingTalkChannelConfig.class;
                    break;
                case WeCom:
                    clazz = WeComChannelConfig.class;
                    break;
                case Feishu:
                case Webhook:
                    clazz = WebhookChannelConfig.class;
                    break;
                default:
                    throw new NotImplementedException();
            }
            channel.setChannelConfig(JsonUtils.fromJson(JsonUtils.toJson(properties), clazz));
        }
        return channel;
    }

    public ChannelEntity toEntity(Channel channel) {
        ChannelEntity entity = new ChannelEntity();
        entity.setId(channel.getId());
        entity.setName(channel.getName());
        entity.setType(channel.getType());
        entity.setCreatorId(channel.getCreatorId());
        entity.setOrganizationId(channel.getOrganizationId());
        entity.setProjectId(channel.getProjectId());
        entity.setDescription(channel.getDescription());

        BaseChannelConfig channelConfig = channel.getChannelConfig();
        if (Objects.nonNull(channelConfig)) {
            Map<String, Object> properties =
                    JsonUtils.fromJsonMap(JsonUtils.toJsonIgnoreNull(channelConfig), String.class, Object.class);
            if (channelConfig instanceof WebhookChannelConfig
                    && ((WebhookChannelConfig) channelConfig).getSign() != null) {
                properties.put("sign", ((WebhookChannelConfig) channelConfig).getSign());
            }
            entity.setProperties(properties.entrySet().stream()
                    .map(entry -> {
                        ChannelPropertyEntity property = new ChannelPropertyEntity();
                        property.setKey(entry.getKey());
                        property.setValue(entry.getValue());
                        property.setChannel(entity);
                        return property;
                    }).collect(Collectors.toList()));
        }

        return entity;
    }

}
