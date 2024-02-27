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

import org.jetbrains.annotations.NotNull;

import com.oceanbase.tools.loaddump.client.LoadClient;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;
import com.oceanbase.tools.loaddump.context.TaskContext;

/**
 * {@link OceanBaseImportJob}
 *
 * @author yh263208
 * @date 2022-07-25 20:29
 * @since ODC_release_3.4.0
 */
public class OceanBaseImportJob extends BaseOceanBaseTransferJob<LoadParameter> {

    private final LoadClient loadClient;

    public OceanBaseImportJob(@NotNull LoadParameter parameter, boolean transferData, boolean transferSchema)
            throws Exception {
        super(parameter, transferData, transferSchema);
        loadClient = new LoadClient.Builder(parameter).build();
    }

    @Override
    protected TaskContext startTransferData() throws Exception {
        if (isExternalSql()) {
            return loadClient.loadSchema();
        }
        return loadClient.loadRecord();
    }

    @Override
    protected TaskContext startTransferSchema() throws Exception {
        if (isExternalSql()) {
            throw new IllegalArgumentException("Can not load schema when external sql");
        }
        return loadClient.loadSchema();
    }

    private boolean isExternalSql() {
        return parameter.getDataFormat() == DataFormat.MIX && parameter.isExternal();
    }
}
