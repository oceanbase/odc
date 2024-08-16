/*
 * Copyright (c) 2024 OceanBase.
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

import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author keyang
 * @date 2024/08/16
 * @since 4.3.2
 */
@Getter
@EqualsAndHashCode
public class WorksheetId {
    private final Long projectId;
    private final Path path;

    public WorksheetId(Long projectId, Path path) {
        this.projectId = projectId;
        this.path = path;
    }

    public WorksheetId(String bucketName, String objectName) {
        this.projectId = WorksheetUtil.getProjectIdOfWorkSheets(bucketName);
        this.path = new Path(objectName);
    }
}
