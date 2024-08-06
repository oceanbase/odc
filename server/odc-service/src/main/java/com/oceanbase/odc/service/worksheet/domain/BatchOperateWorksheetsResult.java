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
package com.oceanbase.odc.service.worksheet.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import lombok.Getter;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
@Getter
public class BatchOperateWorksheetsResult {
    List<Worksheet> success;

    List<Worksheet> failed;

    public BatchOperateWorksheetsResult() {
        success = new ArrayList<>();
        failed = new ArrayList<>();
    }

    public void addResult(BatchOperateWorksheetsResult result) {
        if (result == null) {
            return;
        }
        addSuccess(result.getSuccess());
        addFailed(result.getFailed());
    }

    public void addSuccess(List<Worksheet> worksheets) {
        if (CollectionUtils.isEmpty(worksheets)) {
            return;
        }
        success.addAll(worksheets);
    }

    public void addSuccess(Worksheet worksheet) {
        if (worksheet == null) {
            return;
        }
        success.add(worksheet);
    }

    public void addFailed(List<Worksheet> worksheets) {
        if (CollectionUtils.isEmpty(worksheets)) {
            return;
        }
        failed.addAll(worksheets);
    }

    public void addFailed(Worksheet worksheet) {
        if (worksheet == null) {
            return;
        }
        failed.add(worksheet);
    }
}
