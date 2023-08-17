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
package com.oceanbase.odc.service.datatransfer.task;

import static com.oceanbase.odc.service.datatransfer.model.DataTransferConstants.LOG_PATH_NAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.tools.loaddump.client.LoadClient;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;
import com.oceanbase.tools.loaddump.context.TaskContext;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ImportDataTransferTask}
 *
 * @author yh263208
 * @date 2022-07-25 20:25
 * @since ODC_release_3.4.0
 */
@Slf4j
public class ImportDataTransferTask extends BaseDataTransferTask<LoadParameter> {

    private final LoadClient loadClient;

    public ImportDataTransferTask(@NonNull LoadParameter parameter,
            boolean transferData, boolean transferSchema) throws Exception {
        super(parameter, transferData, transferSchema);
        loadClient = new LoadClient.Builder(parameter).build();
    }

    @Override
    protected TaskContext startTransferData(LoadParameter parameter) throws Exception {
        ThreadContext.put(LOG_PATH_NAME, parameter.getLogsPath());
        if (isExternalSql()) {
            return loadClient.loadSchema();
        }
        return loadClient.loadRecord();
    }

    @Override
    protected TaskContext startTransferSchema(LoadParameter parameter) throws Exception {
        if (isExternalSql()) {
            throw new IllegalArgumentException("Can not load schema when external sql");
        }
        ThreadContext.put(LOG_PATH_NAME, parameter.getLogsPath());
        return loadClient.loadSchema();
    }

    @Override
    protected void afterHandle(LoadParameter parameter, DataTransferTaskContext context,
            DataTransferTaskResult result) throws IOException {
        BaseDataTransferTask.validAllTasksSuccessed(context);
        String filePath = parameter.getFilePath();
        Verify.verify(StringUtils.isNotBlank(filePath), "File path is blank");
        File importPath = new File(filePath + File.separator + "data");
        if (importPath.exists()) {
            boolean deleteRes = FileUtils.deleteQuietly(importPath);
            log.info("Delete import directory, dir={}, result={}", importPath.getAbsolutePath(), deleteRes);
        }
        File workingDir = new File(filePath);
        if (!workingDir.exists()) {
            throw new FileNotFoundException("Working dir does not exist");
        }
        for (File subFile : workingDir.listFiles()) {
            if (subFile.isDirectory()) {
                continue;
            }
            boolean deleteRes = FileUtils.deleteQuietly(subFile);
            log.info("Delete import file, fileName={}, result={}", subFile.getName(), deleteRes);
        }
    }

    private boolean isExternalSql() {
        return parameter.getDataFormat() == DataFormat.MIX && parameter.isExternal();
    }

}
