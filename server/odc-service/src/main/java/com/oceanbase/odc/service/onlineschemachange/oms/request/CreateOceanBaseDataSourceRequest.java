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
package com.oceanbase.odc.service.onlineschemachange.oms.request;

import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import com.oceanbase.odc.service.onlineschemachange.oms.annotation.OmsEnumsCheck;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsOceanBaseType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class CreateOceanBaseDataSourceRequest extends BaseOmsRequest {
    /**
     * 数据源名称
     */
    @NotBlank
    @Length(max = 128)
    private String name;
    /**
     * 类型 OceanBaseDataSourceType
     */
    @OmsEnumsCheck(fieldName = "type", enumClass = OmsOceanBaseType.class)
    private String type;
    /**
     * 租户名称或 ID（专有云、VPC 对应租户名称 ）
     */
    private String tenant;
    /**
     * 集群名称或 ID (专有云、VPC 对应集群名称)
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
     * logProxy IP (VPC)
     */
    private String logProxyIp;
    /**
     * logProxy Port (VPC)
     */
    private Integer logProxyPort;
    /**
     * 数据库连接用户名
     */
    @NotBlank
    private String userName;
    /**
     * 数据库连接密码，使用base64编码
     */
    private String password;
    /**
     * 地域（专有云）
     */
    private String region;
    /**
     * 自定义描述信息,最长 128 个字符
     */
    @Length(max = 128)
    private String description;
    /**
     * 关联的OCP (专有云)
     */
    private String ocpName;
    /**
     * 未关联OCP时可填 主要用于获取 OceanBase 数据库底层服务器的真实地址。
     */
    private String configUrl;
    /**
     * sys 租户下用户名 (专有云、VPC) 注意：本用户必须创建于业务集群 sys 租户下
     * （如果本数据源作为源端时，您选择了结构迁移/结构同步/增量同步，或作为目标端时，您选择了反向增量，请填写如下信息 ， 作用是读取 OceanBase 数据库的增量日志数据和数据库对象结构信息 ）
     */
    private String drcUserName;
    /**
     * sys 租户下用户名密码,使用base64编码）(专有云、VPC)
     */
    private String drcPassword;
    /**
     * __oceanbase_inner_drc_user 的密码,使用base64编码
     * 本用户用于支持无唯一键的表数据迁移（如果本数据源作为目标端，则非必填）。请注意：本用户需要在当前数据源所在的租户下创建
     */
    private String innerDrcPassword;
}
