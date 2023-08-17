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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.objectstorage.LocalFileTransferService;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2021/9/6 下午5:11
 * @Description: [通用文件上传/下载接口]
 */
@RestController
@RequestMapping("/api/v2/objectstorage")
public class FileController {
    @Autowired
    private FileManager fileManager;

    @Autowired
    private LocalFileTransferService localFileTransferService;

    @ApiOperation(value = "download", notes = "下载文件")
    @RequestMapping(value = "/{bucket}/files/{id:.+}", method = RequestMethod.GET)
    public Object download(@PathVariable FileBucket bucket, @PathVariable String id) throws IOException {
        return fileManager.download(bucket, id);
    }

    @RequestMapping(value = "/files/{id:.+}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) throws IOException {
        return localFileTransferService.download(id);
    }

    @RequestMapping(value = "/{bucket}/files/batchUpload", method = RequestMethod.POST)
    public ListResponse<ObjectMetadata> batchUpload(@PathVariable String bucket,
            @RequestParam("file") List<MultipartFile> files) {
        return Responses.list(localFileTransferService.batchUpload(bucket, files));
    }
}


