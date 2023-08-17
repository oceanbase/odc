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
package com.oceanbase.odc.service.common.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Properties for {@code Host}
 *
 * @author yh263208
 * @date 2022-03-24 16:36
 * @since ODC_release_3.3.0
 */
@Data
@RefreshScope
@Configuration
public class HostProperties {
    /**
     * odc 的默认端口号，如果用户没有手动指定 port 就使用这个值作为 odc 的端口
     */
    @Value("${server.port}")
    private String port;

    /**
     * 由于 odc 的部署环境无法预知，因此留一个口子给用户可以手动设定 odc 的 host 信息，防止程序自动获取的 ip 无法访问 造成的应用功能受阻。
     */
    @Value("${ODC_HOST:#{null}}")
    private String odcHost;

    /**
     * 由于 odc 可能在 docker 中部署，会进行端口映射，因此{@link HostProperties#getPort()} 可能无法表示 odc 的真实
     * 的端口，这里增加一个字段是为了让用户在特殊条件下能够手动指定 odc 的端口号，避免因为部署环境的问题导致的端口号无法 使用。
     */
    @Value("${ODC_MAPPING_PORT:#{null}}")
    private String odcMappingPort;
}
