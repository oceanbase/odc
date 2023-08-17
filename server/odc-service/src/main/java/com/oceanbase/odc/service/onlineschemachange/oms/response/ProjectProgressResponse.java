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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.ProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.jackson.CustomEnumDeserializer;

import lombok.Data;

/**
 * 项目进度详情
 *
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
@JsonFormat(with = Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class ProjectProgressResponse {
    /**
     * 项目ID
     */
    private String projectId;
    /**
     * 通用的项目状态枚举
     */
    @JsonDeserialize(using = CustomEnumDeserializer.ProjectStatusEnumDeserializer.class)
    private ProjectStatusEnum status;

    /**
     * 告警保障等级
     */
    private String alarmLevel;
    /**
     * 是否有正向增量同步
     */
    private Boolean enableIncrSync;

    /**
     * 当前进行到哪一步
     */
    private String currentStep;

    /**
     * <p>
     * 正向增量同步位点，根据源端类型的不同有 3 种不同格式需要用户感知和解析： 1）如果源端是 Store / Logproxy （无特殊说明 DB2 / OB / MYSQL / ORACLE
     * 增量）格式均是 unix timstamp，单位秒。 2）源端是 sybase，格式是 fileId,fileOffset，比如 100,30 这样的数据 3）源端是 geabase，后续补充。
     */
    private Long incrSyncCheckpoint;

    /**
     * 是否有结构传输
     */
    private Boolean enableStructTransfer;

    /**
     * 是否有全量数据传输
     */
    private Boolean enableFullTransfer;

    /**
     * 结构传输进度百分比，0~100
     */
    private Integer structTransferProgress;

    /**
     * 全量传输进度百分比，0~100
     */
    private Integer fullTransferProgress;

    /******************** - 迁移项目 - **********************/
    /**
     * 是否有反向增量同步
     */
    private Boolean enableReverseIncrTransfer;

    /**
     * 反向增量同步位点，unix timestamp，单位秒
     */
    private Long reverseIncrTransferCheckpoint;

}
