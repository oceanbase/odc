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

import com.oceanbase.odc.metadb.worksheet.CollaborationWorksheetEntity;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
public class WorksheetConverter {
    public static WorksheetMetaResp convertEntityToMetaResp(CollaborationWorksheetEntity entity) {
        Path path = new Path(entity.getPath());
        return WorksheetMetaResp.builder().projectId(entity.getProjectId())
                .path(entity.getPath())
                .type(path.getType())
                .objectId(entity.getObjectId())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public static WorksheetResp convertEntityToResp(CollaborationWorksheetEntity entity, String contentDownloadUrl) {
        Path path = new Path(entity.getPath());
        return WorksheetResp.builder().projectId(entity.getProjectId())
                .path(entity.getPath())
                .type(path.getType())
                .objectId(entity.getObjectId())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .version(entity.getVersion())
                .contentDownloadUrl(contentDownloadUrl)
                .build();
    }
}
