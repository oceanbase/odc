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
package com.oceanbase.odc.service.flow.task.model;

import java.util.List;

import com.oceanbase.odc.core.flow.model.FlowTaskResult;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2021/3/15
 */
@Data
public class DatabaseChangeResult implements FlowTaskResult {
    private List<String> records;
    private Integer successCount;
    private Integer failCount;
    private String zipFileDownloadUrl;
    private String zipFileId;
    private String jsonFileName;
    private Long jsonFileBytes;
    private boolean isContainQuery;
    private Long resultPreviewMaxSizeBytes;
    private String errorRecordsFilePath;
    private RollbackPlanTaskResult rollbackPlanResult;
}
