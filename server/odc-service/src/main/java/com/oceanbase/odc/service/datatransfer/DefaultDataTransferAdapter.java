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

package com.oceanbase.odc.service.datatransfer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultDataTransferAdapter implements DataTransferAdapter {

    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    @Override
    public Long getMaxDumpSizeBytes() {
        return 2 * 1024 * 1024 * 1024L;
    }

    @Override
    public File preHandleWorkDir(DataTransferConfig config, String bucket, File workDir) throws IOException {
        return workDir;
    }

    @Override
    public void afterHandle(DataTransferConfig config, DataTransferTaskResult result, File exportFile)
            throws IOException {
        if (!cloudObjectStorageService.supported()) {
            return;
        }
        try {
            String objectName = cloudObjectStorageService.uploadTemp(exportFile.getName(), exportFile);
            log.info("Upload the data file to the oss successfully, objectName={}", objectName);
            result.setExportZipFilePath(objectName);
        } finally {
            /**
             * 公有云模式下本地导出文件和目录都不必要保存，直接删除
             */
            boolean deleteRes = FileUtils.deleteQuietly(exportFile);
            log.info("Temporary data file deleted, filePath={}, result={}", exportFile.getName(), deleteRes);
        }
    }

}

