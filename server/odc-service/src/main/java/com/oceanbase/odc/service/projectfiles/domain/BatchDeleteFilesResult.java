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
package com.oceanbase.odc.service.projectfiles.domain;

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
public class BatchDeleteFilesResult {
    List<ProjectFile> success;

    List<ProjectFile> failed;

    public BatchDeleteFilesResult() {
        success = new ArrayList<>();
        failed = new ArrayList<>();
    }

    public void addResult(BatchDeleteFilesResult result) {
        if (result == null) {
            return;
        }
        addSuccess(result.getSuccess());
        addFailed(result.getFailed());
    }

    public void addSuccess(List<ProjectFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        success.addAll(files);
    }

    public void addFailed(List<ProjectFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        failed.addAll(files);
    }
}
