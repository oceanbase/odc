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

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.service.onlineschemachange.oms.enums.ProjectTypeEnum;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yaobin
 * @date 2023-05-31
 * @since 4.2.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateProjectRequest extends BaseOmsRequest {

    /**
     * 项目 id
     */
    private String id;
    /**
     * 名称,不能包含空格
     */
    private String name = "odc_" + UUID.randomUUID().toString().replace("-", "");
    /**
     * 项目类型 数据迁移任务 MIGRATION, 数据同步任务 SYNC
     */
    private String type = ProjectTypeEnum.MIGRATION.name();

    /**
     * 公有云：传输实例规格，专有云不需要，公有云必填
     */
    private String workerGradeId;
    /*************************************** --数据源信息-- ***************************************/

    /**
     * 源端数据源ID
     */
    @NotBlank
    private String sourceEndpointId;
    /**
     * 目标端数据源ID
     */
    @NotBlank
    private String sinkEndpointId;

    /**
     * 传输对象映射
     */
    @Valid
    @NotNull
    private SpecificTransferMapping transferMapping;
    /**
     * 通用传输配置
     */
    @Valid
    @NotNull
    private CommonTransferConfig commonTransferConfig;
    /**
     * 是否启用结构传输
     */
    private Boolean enableStructTransfer = Boolean.FALSE;

    /**
     * 是否启用全量传输
     */
    private Boolean enableFullTransfer = Boolean.TRUE;
    /**
     * 是否启用全量校验
     */
    private Boolean enableFullVerify = Boolean.FALSE;
    /**
     * 全量传输配置
     */
    @NotNull
    @Valid
    private FullTransferConfig fullTransferConfig;
    /**
     * 是否启用增量传输
     */
    private Boolean enableIncrTransfer = Boolean.TRUE;
    /**
     * 是否启用反向增量传输
     */
    private Boolean enableReverseIncrTransfer = Boolean.FALSE;
    /**
     * 增量传输配置
     */
    @NotNull
    @Valid
    private IncrTransferConfig incrTransferConfig;
}
