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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.task.obloaderdumper.DataTransferTaskContext;
import com.oceanbase.odc.service.flow.task.OssTaskReferManager;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.tools.loaddump.common.model.BaseParameter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"alipay"})
public class WebDataTransferAdapter implements DataTransferAdapter {

    @Autowired
    private OssTaskReferManager taskReferManager;
    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    @Override
    public Long getMaxDumpSizeBytes() {
        return 2 * 1024 * 1024 * 1024L;
    }

    @Override
    public File preHandleWorkDir(DataTransferConfig transferConfig,
            String bucket, File workDir) {
        return workDir;
    }

    @Override
    public void afterHandle(BaseParameter parameter, DataTransferTaskContext context,
            DataTransferTaskResult result, File exportFile) throws IOException {
        if (!cloudObjectStorageService.supported()) {
            return;
        }
        try {
            String objectName = cloudObjectStorageService.uploadTemp(exportFile.getName(), exportFile);
            log.info("Upload the data file to the oss successfully, objectName={}", objectName);
            taskReferManager.put(exportFile.getName(), objectName);
        } finally {
            /**
             * 公有云模式下本地导出文件和目录都不必要保存，直接删除
             */
            boolean deleteRes = FileUtils.deleteQuietly(exportFile);
            log.info("Temporary data file deleted, filePath={}, result={}", exportFile.getName(), deleteRes);
        }
    }

}
