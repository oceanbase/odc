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
package com.oceanbase.odc.service.onlineschemachange.oms.response;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
public class DataSourceConnExtraAttributesVO {

    /**
     * 集群
     */
    protected String cluster;

    /**
     * 租户
     */
    protected String tenant;

    /**
     * 是否逻辑源 公有云
     */
    protected Boolean isLogicSource;

    /**
     * 是否使用 LogProxy
     */
    protected Boolean useLogProxy;

    /**
     * drc_user 配置，优先级：数据源
     */
    protected String drcUser;
    /**
     * config_url
     */
    protected String configUrl;

    /**
     * log proxy 服务 ip
     */
    private String logProxyIp;

    /**
     * log proxy 服务端口
     */
    private Integer logProxyPort;

    /**
     * 对于同步 Dataworks 的接口，数据源是OMS自动生成的，没有用户账号密码不能链接用户OB
     */
    Boolean noUserAuth;
}
