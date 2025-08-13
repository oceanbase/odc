/*
 * Copyright (c) 2025 OceanBase.
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
package com.oceanbase.odc.service.datasecurity.model;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 单表敏感列扫描请求
 *
 * @author Assistant
 * @date 2025/1/27
 */
@Data
public class SingleTableScanReq {

    /**
     * 数据库ID
     */
    @NotNull
    private Long databaseId;

    /**
     * 表名
     */
    @NotBlank
    private String tableName;

    /**
     * 扫描模式，默认为AI识别
     */
    @NotNull
    private ScanningModeType scanningMode = ScanningModeType.JOINT_RECOGNITION;

}