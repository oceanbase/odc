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
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.UploadFileResult;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.datatransfer.DataTransferService;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import io.swagger.annotations.ApiOperation;

/**
 * {@link DataTransferController}
 *
 * @author yh263208
 * @date 2022-08-01 14:54
 * @since ODC_release_3.4.0
 */
@Validated
@RestController
@RequestMapping("/api/v2/dataTransfer")
public class DataTransferController {

    @Autowired
    private DataTransferService dataTransferService;

    /**
     * 上传导入文件
     *
     * @param file file to be uploaded
     * @return meta info of this file
     **/
    @ApiOperation(value = "uploadFile", notes = "上传导入文件")
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public SuccessResponse<UploadFileResult> upload(@RequestParam MultipartFile file) throws IOException {
        return Responses.single(dataTransferService.upload(file));
    }

    /**
     * 获取上传文件的元数据信息。供客户端模式下使用，这是由于其他 profile 模式下"上传文件接口"调用时就已经把文件的元数据返回，
     * 客户端模式没有上传操作，因此要获取文件的元数据信息需要额外调用。其他 profile 下也能调通此接口，只是没必要。
     *
     * @param fileName this param has to be the absolute path of a file for client mode
     * @return meta info of this file
     **/
    @ApiOperation(value = "getMetaInfo", notes = "上传导入文件")
    @RequestMapping(value = "/getMetaInfo", method = RequestMethod.GET)
    public SuccessResponse<UploadFileResult> getMetaInfo(@RequestParam String fileName) throws IOException {
        return Responses.single(dataTransferService.getMetaInfo(fileName));
    }

    /**
     * 获取可导出的对象名称
     *
     * @param databaseId
     * @return object names
     */
    @ApiOperation(value = "getExportObjects", notes = "查询可导出的对象列表")
    @RequestMapping(value = "/getExportObjects", method = RequestMethod.GET)
    public SuccessResponse<Map<ObjectType, Set<String>>> getExportObjects(
            @RequestParam(required = false) Long connectionId, @RequestParam Long databaseId,
            @RequestParam(required = false, name = "objectType") Set<ObjectType> objectType) {
        return Responses.single(dataTransferService.getExportObjectNames(databaseId, objectType));
    }

    /**
     * csv 文件导入时解析 csv 文件，获取CSV文件的列、首行数据信息
     *
     * @param csvConfig
     * @return list of {@link CsvColumnMapping}
     */
    @ApiOperation(value = "getCsvFileInfo", notes = "获取CSV文件的列、首行数据信息")
    @RequestMapping(value = "/getCsvFileInfo", method = RequestMethod.POST)
    public SuccessResponse<List<CsvColumnMapping>> getCsvFileInfo(
            @RequestBody CsvConfig csvConfig) throws IOException {
        return Responses.single(dataTransferService.getCsvFileInfo(csvConfig));
    }

}
