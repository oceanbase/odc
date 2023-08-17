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
package com.oceanbase.odc.service.connection.model;

import java.io.Serializable;

import lombok.Data;

/**
 * 租户连接 Endpoint 参数，包括实际地址、对应的 IC-Server 地址、IC 通道下的虚拟地址
 */
@Data
public class OBTenantEndpoint implements Serializable {
    /**
     * 连接模式，详见 {@link OceanBaseAccessMode}
     */
    private OceanBaseAccessMode accessMode;
    /**
     * 租户连接 Host，目标 OceanBase 租户的实际连接 Host，保留字段暂未使用
     */
    private String host;
    /**
     * 租户连接 Port，目标 OceanBase 租户的实际连接端口，保留字段暂未使用
     */
    private Integer port;

    /**
     * 代理服务 Host， 即 IC-Server 地址，配置到 JDBC url socksProxyHost 参数，多云适用
     */
    private String proxyHost;
    /**
     * 代理服务 端口， 即 IC-Server 端口，配置到 JDBC url socksProxyPort 参数，多云适用
     */
    private Integer proxyPort;

    /**
     * 虚拟 Host，通过代理服务访问的目标 Host，代替直连模式的 host，多云和阿里云公有云适用
     */
    private String virtualHost;
    /**
     * 虚拟 Port，通过代理服务访问的目标 Port，代替直连模式 port，多云和阿里云公有云适用
     */
    private Integer virtualPort;
}
