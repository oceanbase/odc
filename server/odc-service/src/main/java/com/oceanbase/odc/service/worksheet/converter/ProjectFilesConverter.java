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
package com.oceanbase.odc.service.worksheet.converter;

import java.util.stream.Collectors;

import com.oceanbase.odc.service.worksheet.domain.BatchOperateWorksheetsResult;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
public class ProjectFilesConverter {
    public static WorksheetMetaResp convertToMetaResp(Worksheet worksheet) {
        return WorksheetMetaResp.builder().projectId(worksheet.getProjectId())
                .path(worksheet.getPath().getStandardPath())
                .type(worksheet.getPath().getType())
                .createTime(worksheet.getCreateTime())
                .updateTime(worksheet.getUpdateTime())
                .build();
    }

    public static WorksheetResp convertToResp(Worksheet worksheet) {
        return WorksheetResp.builder()
                .version(worksheet.getVersion())
                .content(worksheet.getContent())
                .projectId(worksheet.getProjectId())
                .path(worksheet.getPath().getStandardPath())
                .type(worksheet.getPath().getType())
                .createTime(worksheet.getCreateTime())
                .updateTime(worksheet.getUpdateTime())
                .build();
    }

    public static BatchOperateWorksheetsResp convertToBatchOperateResp(BatchOperateWorksheetsResult result) {
        return BatchOperateWorksheetsResp.builder()
                .isAllSuccessful(result.getFailed().isEmpty())
                .successFiles(result.getSuccess().stream()
                        .map(ProjectFilesConverter::convertToMetaResp).collect(Collectors.toList()))
                .failedFiles(result.getFailed().stream()
                        .map(ProjectFilesConverter::convertToMetaResp).collect(Collectors.toList()))
                .build();
    }
}
