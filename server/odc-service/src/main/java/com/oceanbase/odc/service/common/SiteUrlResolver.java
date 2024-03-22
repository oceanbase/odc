/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.common;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;

/**
 * @author liuyizhuo.lyz
 * @date 2024/3/21
 */
@Component
public class SiteUrlResolver {
    @Autowired
    private HostProperties hostProperties;
    @Autowired
    private SystemConfigService systemConfigService;

    public String getSiteUrl() {
        Configuration configuration = systemConfigService.queryByKey("odc.site.url");
        if (configuration != null && !StringUtils.contains(configuration.getValue(), "localhost")) {
            return configuration.getValue();
        }
        String host = Optional.ofNullable(hostProperties.getOdcHost()).orElse(SystemUtils.getLocalIpAddress());
        String port = Optional.ofNullable(hostProperties.getOdcMappingPort()).orElse(hostProperties.getPort());
        return String.format("%s:%s", host, port);
    }
}
