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
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;

@Component
@Profile("clientMode")
public class DesktopDataTransferAdapter extends DefaultDataTransferAdapter {

    @Override
    public Long getMaxDumpSizeBytes() {
        return null;
    }

    @Override
    public File preHandleWorkDir(DataTransferParameter parameter, String bucket, File workDir) throws IOException {
        workDir = super.preHandleWorkDir(parameter, bucket, workDir);

        if (parameter.getTransferType() != DataTransferType.EXPORT) {
            return workDir;
        }
        // 客户端模式且为导出
        String exportFilePath = parameter.getExportFilePath();
        Validate.notNull(exportFilePath, "ExportFilePath can not be null");
        // 需要在用户传入的路径上增加 bucket，否则最终将在用户指定目录下形成 data 和 logs 两个子目录
        File newWorkingDir = new File(exportFilePath, DataTransferService.CLIENT_DIR_PREFIX + bucket);
        FileUtils.forceMkdir(newWorkingDir);
        return newWorkingDir;
    }

    @Override
    public void afterHandle(DataTransferParameter parameter, DataTransferTaskResult result, File exportFile) {
        result.setExportZipFilePath(exportFile.getAbsolutePath());
    }

}
