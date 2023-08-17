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

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.notification.ChannelEntity;
import com.oceanbase.odc.service.notification.model.ChannelConfig;

/**
 * @Author: Lebie
 * @Date: 2023/3/21 18:08
 * @Description: []
 */
@Component
public class ChannelMapper {

    public ChannelConfig fromEntity(ChannelEntity entity) {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setId(entity.getId());
        channelConfig.setName(entity.getName());
        channelConfig.setType(entity.getType());
        if (CollectionUtils.isNotEmpty(entity.getProperties())) {
            Map<String, String> properties =
                    entity.getProperties().stream().collect(HashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()),
                            HashMap::putAll);
            channelConfig.setProperties(properties);
        }
        return channelConfig;
    }
}
