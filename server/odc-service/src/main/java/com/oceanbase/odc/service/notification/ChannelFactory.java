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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.notification.model.ChannelConfig;
import com.oceanbase.odc.service.notification.model.ChannelType;

/**
 * @Author: Lebie
 * @Date: 2023/3/21 17:59
 * @Description: []
 */
@Service
@SkipAuthorize("odc internal usage")
public class ChannelFactory {
    @Autowired
    private UserRepository userRepository;

    public Channel generate(ChannelConfig channelConfig) {
        if (channelConfig.getType() == ChannelType.DingTalkGroupBot) {
            return new DingTalkBotChannel(channelConfig, userRepository);
        } else {
            throw new NotImplementedException();
        }
    }
}
