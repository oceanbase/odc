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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.oceanbase.odc.core.shared.constant.ErrorCode;

import lombok.Getter;
import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/5/9 17:34
 */
@Getter
public class SensitiveColumnScanningTaskInfo {

    private final String taskId;
    private final Long projectId;
    private ScanningTaskStatus status;
    private final Integer allTableCount;
    private Integer finishedTableCount;
    private final List<SensitiveColumn> sensitiveColumns;
    private final Date createTime;
    private Date completeTime;
    private ErrorCode errorCode;
    private String errorMsg;

    public SensitiveColumnScanningTaskInfo(@NonNull String taskId, @NonNull Long projectId,
            @NonNull Integer allTableCount) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.status = ScanningTaskStatus.CREATED;
        this.allTableCount = allTableCount;
        this.finishedTableCount = 0;
        this.sensitiveColumns = new ArrayList<>();
        this.createTime = new Date();
    }

    public synchronized void addFinishedTableCount() {
        this.finishedTableCount = this.finishedTableCount + 1;
        if (finishedTableCount.equals(allTableCount)) {
            this.completeTime = new Date();
            this.status = ScanningTaskStatus.SUCCESS;
        }
    }

    public synchronized void addSensitiveColumns(List<SensitiveColumn> columns) {
        this.sensitiveColumns.addAll(columns);
    }

    public synchronized void setStatus(ScanningTaskStatus status) {
        this.status = status;
    }

    public synchronized void setCompleteTime(Date date) {
        this.completeTime = date;
    }

    public synchronized void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public synchronized void setErrorMsg(String msg) {
        this.errorMsg = msg;
    }

    public enum ScanningTaskStatus {
        CREATED,
        RUNNING,
        SUCCESS,
        FAILED;

        public boolean isCompleted() {
            return this == SUCCESS || this == FAILED;
        }
    }

}
