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
package com.oceanbase.odc.service.worksheet;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlReq;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.ListWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.UpdateWorksheetReq;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;

/**
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@Service
public class WorksheetServiceFacadeImpl implements WorksheetServiceFacade {


    @Override
    public GenerateWorksheetUploadUrlResp generateUploadUrl(Long projectId, GenerateWorksheetUploadUrlReq req) {
        return null;
    }


    @Override
    public WorksheetMetaResp createWorksheet(Long projectId, String pathStr, String objectId) {
        return null;
    }


    @Override
    public WorksheetResp getWorksheetDetail(Long projectId, String pathStr) {
        return null;
    }

    @Override
    public List<WorksheetMetaResp> listWorksheets(Long projectId, ListWorksheetsReq req) {
        return null;
    }

    @Override
    public BatchOperateWorksheetsResp batchUploadWorksheets(Long projectId, BatchUploadWorksheetsReq req) {
        return null;
    }

    @Override
    public BatchOperateWorksheetsResp batchDeleteWorksheets(Long projectId, List<String> paths) {
        return null;
    }

    @Override
    public List<WorksheetMetaResp> renameWorksheet(Long projectId, String pathStr, String destinationPath) {
        return null;
    }


    @Override
    public List<WorksheetMetaResp> editWorksheet(Long projectId, String pathStr, UpdateWorksheetReq req) {
        return null;
    }


    @Override
    public String batchDownloadWorksheets(Long projectId, Set<String> paths) {
        return null;
    }
}
