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
package com.oceanbase.odc.service.sqlplan.model;

import lombok.Data;

@Data
public class SqlPlanTaskResult {

    private long totalStatements = 0;

    private long finishedStatements = 0;

    private long succeedStatements = 0;

    private long failedStatements = 0;

    private String failedRecord;

    /**
     * sql execution json file download url
     */
    private String sqlExecuteJsonFileDownloadUrl;

    /**
     * DQL result set download url
     */
    private String csvResultSetZipDownloadUrl;

    /**
     * error record download url
     */
    private String errorRecordsFileDownloadUrl = null;


    private void incrementFinishedStatements() {
        this.finishedStatements++;
    }

    public void incrementSucceedStatements() {
        this.succeedStatements++;
        incrementFinishedStatements();
    }

    public void incrementFailedStatements() {
        this.failedStatements++;
        incrementFinishedStatements();
    }

}
