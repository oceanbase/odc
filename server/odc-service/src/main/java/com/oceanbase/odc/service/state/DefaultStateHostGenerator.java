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
package com.oceanbase.odc.service.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.session.factory.StateHostGenerator;

@Component
public class DefaultStateHostGenerator implements StateHostGenerator {

    private static final String IP_ADDRESS = "ipAddress";
    private static final String HOST_NAME = "hostName";

    @Autowired
    private HostProperties hostProperties;

    @Override
    public String getHost() {
        if (IP_ADDRESS.equalsIgnoreCase(hostProperties.getHostType())) {
            return hostProperties.getOdcHost() == null ? SystemUtils.getLocalIpAddress() : hostProperties.getOdcHost();
        }
        if (HOST_NAME.equalsIgnoreCase(hostProperties.getHostType())) {
            return SystemUtils.getHostName();
        }
        throw new IllegalStateException("unknown host type");
    }

}
