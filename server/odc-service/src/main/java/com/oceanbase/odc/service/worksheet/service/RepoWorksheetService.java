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
package com.oceanbase.odc.service.worksheet.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.BatchOperateWorksheetsResult;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;

/**
 * Worksheets下的文件的处理
 *
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Service
public class RepoWorksheetService implements WorksheetService {

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Worksheet createWorksheet(Long projectId, Path createPath, String objectKey) {
        return null;
    }

    @Override
    public Worksheet getWorksheetDetails(Long projectId, Path path) {

        return null;
    }

    @Override
    public List<Worksheet> listWorksheets(Long projectId, Path path) {

        return new ArrayList<>();
    }

    @Override
    public List<Worksheet> searchWorksheets(Long projectId, String nameLike, int limit) {
        return Collections.emptyList();
    }

    @Override
    public BatchOperateWorksheetsResult batchUploadWorksheets(Long projectId, BatchCreateWorksheets batchCreateWorksheets) {
        return null;
    }

    @Override
    public BatchOperateWorksheetsResult batchDeleteWorksheets(Long projectId, Set<Path> paths) {
        return new BatchOperateWorksheetsResult();
    }

    @Override
    public List<Worksheet> renameWorksheet(Long projectId, Path path, Path destination) {
        return Collections.emptyList();
    }

    @Override
    public List<Worksheet> editWorksheet(Long projectId, Path path, Path destination, String objectKey, Long readVersion) {
        return Collections.emptyList();
    }

    @Override
    public String batchDownloadWorksheets(Long projectId, Set<String> paths) {
        return "";
    }
}
