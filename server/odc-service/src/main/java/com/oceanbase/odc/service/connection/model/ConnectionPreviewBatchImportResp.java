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
package com.oceanbase.odc.service.connection.model;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odc.core.shared.constant.ErrorCode;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class ConnectionPreviewBatchImportResp {
    /**
     * 当文件不合法时，这里给出具体不合法的原因
     */
    private ErrorCode errorCode = null;
    private String errorMessage = null;
    /**
     * 导入数据对象
     */
    private List<BatchImportConnection> batchImportConnectionList = new ArrayList<>();

    public static ConnectionPreviewBatchImportResp ofFail(@NonNull ErrorCode errorCode) {
        ConnectionPreviewBatchImportResp metaInfo = new ConnectionPreviewBatchImportResp();
        metaInfo.errorCode = errorCode;
        metaInfo.errorMessage = errorCode.getLocalizedMessage(new Object[] {});
        return metaInfo;
    }

    public static ConnectionPreviewBatchImportResp ofConnectionExcel(@NonNull List<BatchImportConnection> list) {
        ConnectionPreviewBatchImportResp excelInfo = new ConnectionPreviewBatchImportResp();
        excelInfo.batchImportConnectionList.addAll(list);
        return excelInfo;
    }
}


