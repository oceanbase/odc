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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.jackson.CustomEnumDeserializer;

import lombok.Data;

/**
 * 项目步骤信息
 *
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
@JsonFormat(with = Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class OmsProjectStepVO {
    /**
     * 步骤顺序
     */
    private Integer order;

    /**
     * 步骤名
     */
    @JsonDeserialize(using = CustomEnumDeserializer.OmsStepNameDeserializer.class)
    private OscStepName name;

    /**
     * 步骤描述（预检查/结构迁移/结构同步/全量迁移/全量同步/全量校验/索引迁移/增量日志拉取/增量同步/增量校验/正向切换）
     */
    private String description;

    /**
     * 步骤状态
     */
    @JsonDeserialize(using = CustomEnumDeserializer.OmsStepStatusDeserializer.class)
    private OmsStepStatus status;

    /**
     * 补充信息（json）
     */
    private OmsStepExtraInfoVO extraInfo;

    /**
     * 开始时间，UTC 格式："2020-05-22T17:04:18"
     */
    private LocalDateTime startTime;

    /**
     * 结束时间，UTC 格式："2020-05-22T17:04:18"
     */
    private LocalDateTime finishTime;
    /**
     * 步骤进度
     */
    private Integer progress;

    /**
     * 步骤详情,全量传输/增量传输/正向切换
     */
    @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "name",
            defaultImpl = BaseOmsProjectStepInfoVO.class)
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = FullTransferStepInfoVO.class, name = "FULL_TRANSFER"),
            @JsonSubTypes.Type(value = FullTransferStepInfoVO.class, name = "FULL_VERIFIER"),
            @JsonSubTypes.Type(value = BaseOmsProjectStepInfoVO.class, name = "TRANSFER_PRECHECK"),
            @JsonSubTypes.Type(value = BaseOmsProjectStepInfoVO.class, name = "TRANSFER_INCR_LOG_PULL"),
            @JsonSubTypes.Type(value = IncrTransferStepInfoVO.class, name = "INCR_TRANSFER"),
            @JsonSubTypes.Type(value = TransferAppSwitchStepInfoVO.class, name = "TRANSFER_APP_SWITCH")
    })
    private BaseOmsProjectStepInfoVO stepInfo;

}
