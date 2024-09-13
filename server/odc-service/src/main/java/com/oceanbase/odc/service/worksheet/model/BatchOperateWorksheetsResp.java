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
package com.oceanbase.odc.service.worksheet.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperateWorksheetsResp {
    /**
     * Is the operation of all worksheets successfulFiles.
     */
    Boolean allSuccessful;
    List<WorksheetMetaResp> successfulFiles;
    List<WorksheetMetaResp> failedFiles;

    public static BatchOperateWorksheetsResp ofSuccess(List<WorksheetMetaResp> worksheets) {
        return new BatchOperateWorksheetsResp(true, worksheets, null);
    }

    public static BatchOperateWorksheetsResp ofFailed(List<WorksheetMetaResp> worksheets) {
        return new BatchOperateWorksheetsResp(false, null, worksheets);
    }

    public void addResult(BatchOperateWorksheetsResp result) {
        if (result == null) {
            return;
        }
        addSuccess(result.getSuccessfulFiles());
        addFailed(result.getFailedFiles());
    }

    public void addSuccess(List<WorksheetMetaResp> worksheets) {
        if (CollectionUtils.isEmpty(worksheets)) {
            return;
        }
        if (allSuccessful == null) {
            allSuccessful = true;
        }
        if (successfulFiles == null) {
            successfulFiles = new ArrayList<>();
        }
        successfulFiles.addAll(worksheets);
    }

    public void addSuccess(WorksheetMetaResp worksheet) {
        if (worksheet == null) {
            return;
        }
        if (allSuccessful == null) {
            allSuccessful = true;
        }
        if (successfulFiles == null) {
            successfulFiles = new ArrayList<>();
        }
        successfulFiles.add(worksheet);
    }

    public void addFailed(List<WorksheetMetaResp> worksheets) {
        if (CollectionUtils.isEmpty(worksheets)) {
            return;
        }
        allSuccessful = false;
        if (failedFiles == null) {
            failedFiles = new ArrayList<>();
        }
        failedFiles.addAll(worksheets);
    }

    public void addFailed(WorksheetMetaResp worksheet) {
        if (worksheet == null) {
            return;
        }
        allSuccessful = false;
        if (failedFiles == null) {
            failedFiles = new ArrayList<>();
        }
        failedFiles.add(worksheet);
    }
}
