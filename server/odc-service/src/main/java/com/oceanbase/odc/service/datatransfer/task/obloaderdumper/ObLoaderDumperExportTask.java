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
package com.oceanbase.odc.service.datatransfer.task.obloaderdumper;

import static com.oceanbase.odc.service.datatransfer.model.DataTransferConstants.LOG_PATH_NAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.datatransfer.DataTransferAdapter;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.dumper.SchemaMergeOperator;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.tools.loaddump.client.DumpClient;
import com.oceanbase.tools.loaddump.common.enums.ServerMode;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.context.TaskContext;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ObLoaderDumperExportTask}
 *
 * @author yh263208
 * @date 2022-07-25 20:29
 * @since ODC_release_3.4.0
 */
@Slf4j
public class ObLoaderDumperExportTask extends BaseObLoaderDumperTransferTask<DumpParameter> {
    private static final Set<String> OUTPUT_FILTER_FILES = new HashSet<>();

    private final DumpClient dumpClient;
    private final DataTransferAdapter transferAdapter;

    public ObLoaderDumperExportTask(@NonNull DumpParameter parameter,
            boolean transferData, boolean transferSchema,
            @NonNull DataTransferAdapter transferAdapter) throws Exception {
        super(parameter, transferData, transferSchema);
        this.transferAdapter = transferAdapter;
        this.dumpClient = new DumpClient.Builder(parameter).build();
    }

    static {
        OUTPUT_FILTER_FILES.add(OdcConstants.PL_DEBUG_PACKAGE + "-schema.sql");
    }

    @Override
    protected TaskContext startTransferData(DumpParameter parameter) throws Exception {
        ThreadContext.put(LOG_PATH_NAME, parameter.getLogsPath());
        return dumpClient.dumpRecord();
    }

    @Override
    protected TaskContext startTransferSchema(DumpParameter parameter) throws Exception {
        ThreadContext.put(LOG_PATH_NAME, parameter.getLogsPath());
        return dumpClient.dumpSchema();
    }

    @Override
    protected void afterHandle(DumpParameter parameter, DataTransferTaskContext context,
            DataTransferTaskResult result) throws Exception {
        BaseObLoaderDumperTransferTask.validAllTasksSuccessed(context);
        String filePath = parameter.getFilePath();
        Verify.verify(StringUtils.isNotBlank(filePath), "File path is blank");
        File exportPath = new File(filePath + File.separator + "data");
        if (!exportPath.exists()) {
            throw new FileNotFoundException(exportPath + " is not found");
        }
        String bucket = exportPath.getParentFile().getName();
        File dest = new File(filePath + File.separator + bucket + "_export_file.zip");
        try {
            DumperOutput output = new DumperOutput(exportPath);
            output.toZip(dest, file -> !OUTPUT_FILTER_FILES.contains(file.getFileName()));
            if (mergeSchemaFiles) {
                File schemaFile = new File(filePath + File.separator + bucket + "_schema.sql");
                try {
                    ServerMode mode = parameter.getDatabase().getServerMode();
                    SchemaMergeOperator operator = new SchemaMergeOperator(output, parameter.getDatabaseName(), mode);
                    operator.mergeSchemaFiles(schemaFile, filename -> !OUTPUT_FILTER_FILES.contains(filename));
                    // delete zip file if merge succeeded
                    FileUtils.deleteQuietly(dest);
                    dest = schemaFile;
                } catch (Exception ex) {
                    log.warn("merge schema failed, origin files will still be used, reason=", ex);
                }
            }
            result.setExportZipFilePath(dest.getName());
        } finally {
            boolean deleteRes = FileUtils.deleteQuietly(exportPath);
            log.info("Delete export directory, dir={}, result={}", exportPath.getAbsolutePath(), deleteRes);
        }
        this.transferAdapter.afterHandle(parameter, context, result, dest);
    }

}
