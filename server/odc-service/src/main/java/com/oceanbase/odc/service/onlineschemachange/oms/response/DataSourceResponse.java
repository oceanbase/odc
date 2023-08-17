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

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
@JsonFormat(with = Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class DataSourceResponse {
    /**
     * 数据源终端标识
     */
    private String id;
    /**
     * 数据源名称
     */
    private String name;
    /**
     * 数据源类型
     */
    private String type;
    /**
     * 数据源角色，MASTER/SLAVE。
     */
    private String role;

    /**
     * 数据源Owner
     */
    private String owner;

    /**
     * 租户名称或ID （专有云、VPC 对应名称）
     */
    private String tenant;
    /**
     * 集群名称或ID (专有云、VPC 对应名称)
     */
    private String cluster;
    /**
     * 数据库名，若填写，后续迁移或者同步操作，只会针对该数据库操作； (专有云参数)
     */
    private String schema;

    /**
     * vpc id （公有云参数）
     */
    private String vpcId;
    /**
     * ip（专有云、VPC）
     */
    private String ip;
    /**
     * 端口（专有云、VPC）
     */
    private Integer port;
    /**
     * 数据库网关ID （公有云参数）
     */
    private String dGInstanceId;

    /**
     * 数据库连接用户名
     */
    private String userName;
    /**
     * 地域
     */
    private String region;
    /**
     * 自定义描述信息,最长 128 个字符
     */
    private String description;
    /**
     * 关联的OCP (专有云)
     */
    private String ocpName;
    /**
     * ORACLE的sid
     */
    private String oracleSid;
    /**
     * nls_length_semantics 属性，只针对 oracle、ob-oracle
     */
    private String oracleNlsLengthSemantics;
    /**
     * 操作系统，ob 没有。
     */
    private String operatingSystem;

    /**
     * 版本
     */
    private String version;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 编码
     */
    private String charset;

    /**
     * 是否允许尝试维持心跳。
     */
    private Boolean tryKeepHb;
    /**
     * 资源所有者
     */
    private String resourceOwner;

    /**
     * 创建时间 UTC
     */

    private LocalDateTime gmtCreate;
    /**
     * 修改时间 UTC
     */
    private LocalDateTime gmtModified;

    /**
     * 对应主备库ID
     */
    private String partnerId;

    /**
     * 连接额外属性（OceanBase、Kafka、RocketMQ）
     */
    private DataSourceConnExtraAttributesVO connExtraAttributes;

}
