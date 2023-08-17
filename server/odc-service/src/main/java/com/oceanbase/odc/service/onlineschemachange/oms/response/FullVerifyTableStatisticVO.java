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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.CheckerObjectStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.CheckerResultType;
import com.oceanbase.odc.service.onlineschemachange.oms.jackson.CustomEnumDeserializer;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
@JsonFormat(with = Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class FullVerifyTableStatisticVO {

    /**
     * 源表名
     */
    private String sourceTableName;

    /**
     * 源Schema名
     */
    private String sourceSchemaName;

    /**
     * 目标Schema名
     */
    private String destSchemaName;

    /**
     * 仅源端存在数据条数
     */
    private Long sourceOnlyCount;

    /**
     * 仅目的端存在数据条数
     */
    private Long destOnlyCount;

    /**
     * 两端不一致数据条数
     */
    private Long mismatchedCount;

    /**
     * 两端一致数据条数
     */
    private Long consistentCount;

    /**
     * 校验进度
     */
    private String progress;

    /**
     * 相关信息
     */
    private String message;

    /**
     * 迁移状态
     */

    @JsonDeserialize(using = CustomEnumDeserializer.CheckerObjectStatusDeserializer.class)
    private CheckerObjectStatus status;

    /**
     * 校验结果类别
     */
    @JsonDeserialize(using = CustomEnumDeserializer.CheckerResultTypeJsonDeserializer.class)
    private CheckerResultType resultType;

    /**
     * 校验结果类别描述
     */
    private String resultDesc;

    /**
     * 表失败信息和原因
     */
    private List<String> errorDetails;

}
