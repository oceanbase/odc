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

import java.util.regex.Pattern;

import com.oceanbase.tools.loaddump.client.DumpClient;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.context.TaskContext;

import lombok.NonNull;

/**
 * {@link ObLoaderDumperExportTask}
 *
 * @author yh263208
 * @date 2022-07-25 20:25
 * @since ODC_release_3.4.0
 */
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
}
