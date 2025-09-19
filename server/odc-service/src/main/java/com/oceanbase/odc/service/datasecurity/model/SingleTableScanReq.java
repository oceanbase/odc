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
package com.oceanbase.odc.service.datasecurity.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * Single-table sensitive column scan request
 * 
 * @author fenyf
 * @date 2025/8/18 17:52
 */
@Data
public class SingleTableScanReq {

    @NotNull
    private Long databaseId;

    @NotBlank
    private String tableName;

    @NotNull
    private ScanningModeType scanningMode = ScanningModeType.JOINT_RECOGNITION;

}
