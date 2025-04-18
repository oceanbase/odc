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
package com.oceanbase.odc.service.schedule.export.model;

import lombok.Data;

@Data
public class ImportTaskResult {

    /**
     * The unique ID of the exported file, which uniquely represents one task in one file
     */
    private String exportRowId;

    private Boolean success;

    private String remark;

    public static ImportTaskResult success(String exportRowId, String remark) {
        ImportTaskResult result = new ImportTaskResult();
        result.setSuccess(true);
        result.setRemark(remark);
        result.setExportRowId(exportRowId);
        return result;
    }

    public static ImportTaskResult failed(String exportRowId, String failedReason) {
        ImportTaskResult result = new ImportTaskResult();
        result.setRemark(failedReason);
        result.setSuccess(false);
        result.setExportRowId(exportRowId);
        return result;
    }
}
