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

package com.oceanbase.odc.service.schedule.flowtask;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.datatransfer.LocalFileManager;
import com.oceanbase.odc.service.datatransfer.dumper.AbstractOutputFile;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@FlowTaskPreprocessor(type = TaskType.IMPORT)
public class DataTransferPreprocessor implements Preprocessor {
    @Override
    public void process(CreateFlowInstanceReq req) {
        DataTransferParameter parameters = (DataTransferParameter) req.getParameters();

        if (parameters.isCompressed()) {
            try {
                validZipFile(parameters.getImportFileName(), parameters.isTransferData(), parameters.isTransferDDL());
            } catch (Exception e) {
                throw new UnexpectedException("Failed to validate zipfile", e);
            }
        }
    }

    private void validZipFile(List<String> fileNames, boolean isTransferData, boolean isTransferDDL) throws Exception {
        if (fileNames == null || fileNames.size() != 1) {
            log.warn("Single zip file is available, importFileNames={}", fileNames);
            throw new IllegalArgumentException("Single zip file is available");
        }
        String fileName = fileNames.get(0);
        LocalFileManager fileManager = SpringContextUtil.getBean(LocalFileManager.class);
        Optional<File> uploadFile = fileManager.findByName(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET, fileName);
        File from = uploadFile.orElseThrow(() -> new FileNotFoundException("File not found, " + fileName));

        DumperOutput dumperOutput = new DumperOutput(from);
        List<AbstractOutputFile> outputFiles = dumperOutput.getAllDumpFiles();
        Validate.isTrue(!outputFiles.isEmpty(), "No import object found");
        Validate.isTrue(!isTransferData || dumperOutput.isContainsData(), "Input does not contain data");
        Validate.isTrue(!isTransferDDL || dumperOutput.isContainsSchema(), "Input does not contain schema");
    }
}
