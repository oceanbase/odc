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

import com.oceanbase.tools.loaddump.client.DumpClient;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.context.TaskContext;

import lombok.NonNull;

/**
 * {@link OceanBaseExportJob}
 *
 * @author yh263208
 * @date 2022-07-25 20:25
 * @since ODC_release_3.4.0
 */
public class OceanBaseExportJob extends BaseOceanBaseTransferJob<DumpParameter> {
    private final DumpClient dumpClient;

    public OceanBaseExportJob(@NonNull DumpParameter parameter, boolean transferData, boolean transferSchema,
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
