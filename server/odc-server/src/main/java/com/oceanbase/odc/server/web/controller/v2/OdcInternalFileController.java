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

import java.io.IOException;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.flow.OdcInternalFileService;
import com.oceanbase.odc.service.flow.model.BinaryDataResult;

import io.swagger.annotations.ApiOperation;

/**
 * {@link OdcInternalFileController}
 *
 * @author gaoda.xy
 * @date 2022/10/17 21:50
 */
@RestController
@RequestMapping("/api/v2/internal/file")
public class OdcInternalFileController {
    @Autowired
    private OdcInternalFileService odcInternalFileService;

    /**
     * 下载导入文件
     *
     * @param taskId task id for which download import file
     * @param fileName import file name to download
     * @return import file which could be .zip, .sql or .csv format
     **/
    @ApiOperation(value = "downloadImportFile", notes = "下载导入文件，内部使用，仅用于导入任务")
    @RequestMapping(value = "/downloadImportFile", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> download(@RequestParam Long taskId, @RequestParam String fileName,
            @RequestParam String checkCode) throws IOException {
        List<BinaryDataResult> results = odcInternalFileService.downloadImportFile(taskId, fileName, checkCode);
        PreConditions.validExists(ResourceType.ODC_FILE, "taskId", taskId, () -> CollectionUtils.isNotEmpty(results));
        return WebResponseUtils.getFileAttachmentResponseEntity(
                new InputStreamResource(results.get(0).getInputStream()), (results.get(0).getName()));
    }
}
