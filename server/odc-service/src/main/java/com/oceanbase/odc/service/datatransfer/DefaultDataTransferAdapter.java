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
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.flow.task.OssTaskReferManager;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("alipay")
public class DefaultDataTransferAdapter implements DataTransferAdapter {

    @Autowired
    private OssTaskReferManager taskReferManager;
    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    @Override
    public Long getMaxDumpSizeBytes() {
        return 2 * 1024 * 1024 * 1024L;
    }

    @Override
    public File preHandleWorkDir(DataTransferParameter parameter, String bucket, File workDir) throws IOException {
        // 目标目录可能已经存在且其中可能存留有导入导出历史脏数据，这里需要清理避免潜在问题，且为了影响最小化，只清理导入导出相关的目录
        String parent = new File(workDir, "data").getAbsolutePath();
        Arrays.stream(ObjectType.values()).map(ObjectType::getName).forEach(objectName -> {
            File target = new File(parent, objectName);
            if (target.exists() && target.isDirectory()) {
                boolean deleteRes = FileUtils.deleteQuietly(target);
                log.info("Delete object directory, dir={}, result={}", target.getAbsolutePath(), deleteRes);
            }
        });
        return workDir;
    }

    @Override
    public void afterHandle(DataTransferParameter parameter, DataTransferTaskResult result, File exportFile)
            throws IOException {
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

