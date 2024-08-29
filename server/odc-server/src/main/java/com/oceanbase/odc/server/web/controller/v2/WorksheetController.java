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
package com.oceanbase.odc.server.web.controller.v2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.validation.constraints.Size;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.worksheet.WorksheetServiceFacade;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlReq;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.ListWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.UpdateWorksheetReq;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;

import lombok.SneakyThrows;

/**
 * project worksheets management controller
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@RestController
@RequestMapping("/api/v2/project/{projectId}")
public class WorksheetController {

    @Resource
    private WorksheetServiceFacade worksheetServiceFacade;

    @PostMapping("/worksheets/generateUploadUrl")
    public SuccessResponse<GenerateWorksheetUploadUrlResp> generateUploadUrl(
            @PathVariable("projectId") Long projectId, @RequestBody GenerateWorksheetUploadUrlReq req) {
        return Responses.success(worksheetServiceFacade.generateUploadUrl(projectId, req));
    }

    @PostMapping("/worksheets")
    public SuccessResponse<WorksheetMetaResp> createWorksheet(
            @PathVariable("projectId") Long projectId,
            @RequestParam("path") String path,
            @RequestParam(value = "objectId", required = false) String objectId,
            @RequestParam(value = "size", required = false) Long size) {
        return Responses.success(worksheetServiceFacade.createWorksheet(projectId, path, objectId, size));
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    @GetMapping(value = "/worksheets/{path}")
    public SuccessResponse<WorksheetResp> getWorksheetDetail(
            @PathVariable("projectId") Long projectId,
            @PathVariable("path") String path) {
        String decodedPath = URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8));
        return Responses.success(worksheetServiceFacade.getWorksheetDetail(projectId, decodedPath));
    }

    @GetMapping("/worksheets")
    public ListResponse<WorksheetMetaResp> listWorksheets(
            @PathVariable("projectId") Long projectId,
            ListWorksheetsReq req) {
        return Responses.list(worksheetServiceFacade.listWorksheets(projectId, req));
    }

    @PostMapping("/worksheets/batchUpload")
    public SuccessResponse<BatchOperateWorksheetsResp> batchUploadWorksheets(
            @PathVariable("projectId") Long projectId,
            @RequestBody BatchUploadWorksheetsReq req) {
        return Responses.success(worksheetServiceFacade.batchUploadWorksheets(projectId, req));
    }

    @PostMapping("/worksheets/batchDelete")
    public SuccessResponse<BatchOperateWorksheetsResp> batchDeleteWorksheets(
            @PathVariable("projectId") Long projectId,
            @RequestBody @Size(min = 1, max = 100) List<String> paths) {
        return Responses.success(worksheetServiceFacade.batchDeleteWorksheets(projectId, paths));
    }

    @PostMapping("/worksheets/rename")
    public ListResponse<WorksheetMetaResp> renameWorksheet(
            @PathVariable("projectId") Long projectId,
            @RequestParam("path") String path,
            @RequestParam("destinationPath") String destinationPath) {
        return Responses.list(worksheetServiceFacade.renameWorksheet(projectId, path, destinationPath));
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    @PutMapping("/worksheets/{path}")
    public ListResponse<WorksheetMetaResp> editWorksheet(
            @PathVariable("projectId") Long projectId,
            @PathVariable("path") String path,
            @RequestBody UpdateWorksheetReq req) {
        String decodedPath = URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8));
        return Responses.list(worksheetServiceFacade.editWorksheet(projectId, decodedPath, req));
    }

    @PostMapping("/worksheets/batchDownload")
    public SuccessResponse<String> batchDownloadWorksheets(
            @PathVariable("projectId") Long projectId,
            @RequestBody Set<String> paths) {
        return Responses.success(worksheetServiceFacade.batchDownloadWorksheets(projectId, paths));
    }
}
