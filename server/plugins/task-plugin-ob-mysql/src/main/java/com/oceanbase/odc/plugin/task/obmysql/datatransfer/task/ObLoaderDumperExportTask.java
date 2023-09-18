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

package com.oceanbase.odc.plugin.task.obmysql.datatransfer.task;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.TransferTaskStatus;
import com.oceanbase.tools.loaddump.client.DumpClient;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.context.TaskContext;

import lombok.NonNull;

public class ObLoaderDumperExportTask extends BaseObLoaderDumperTransferTask<DumpParameter> {
    private static final Pattern SCHEMA_FILE_PATTERN =
            Pattern.compile("^\"?(.+)\"?-schema\\.(sql)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_FILE_PATTERN =
            Pattern.compile("^\"?(.+)\"?(\\.[0-9]+){0,2}\\.(sql|csv|dat|txt)$", Pattern.CASE_INSENSITIVE);

    private final DumpClient dumpClient;

    public ObLoaderDumperExportTask(@NonNull DumpParameter parameter, boolean transferData, boolean transferSchema,
            boolean usePrepStmts) throws Exception {
        super(parameter, transferData, transferSchema, usePrepStmts);
        this.dumpClient = new DumpClient.Builder(parameter).build();
    }

    @Override
    protected TaskContext startTransferData() throws Exception {
        return dumpClient.dumpRecord();
    }

    @Override
    protected TaskContext startTransferSchema() throws Exception {
        return dumpClient.dumpSchema();
    }

    /**
     * Set exported paths for each object.
     * 
     * @param result
     */
    @Override
    protected void postHandle(TransferTaskStatus result) {
        File dataDir = Paths.get(parameter.getFilePath(), "data").toFile();
        result.getSchemaObjectsInfo().forEach(object -> findExportFiles(dataDir, object, SCHEMA_FILE_PATTERN));
        result.getDataObjectsInfo().forEach(object -> findExportFiles(dataDir, object, DATA_FILE_PATTERN));
    }

    private void findExportFiles(File root, ObjectStatus object, Pattern pattern) {
        String[] exported = root.list(((dir, name) -> {
            Matcher matcher = pattern.matcher(name);
            if (!matcher.matches()) {
                return false;
            }
            return Objects.equals(matcher.group(1), object.getName());
        }));
        if (ArrayUtils.isEmpty(exported)) {
            throw new UnexpectedException("No exported file found for object " + object.getSummary());
        }
        object.setExportPaths(exported);
    }

}
