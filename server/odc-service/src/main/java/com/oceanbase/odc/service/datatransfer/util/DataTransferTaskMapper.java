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

package com.oceanbase.odc.service.datatransfer.util;

import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.service.datatransfer.model.DataTransferType;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTask;
import com.oceanbase.odc.service.datatransfer.task.datax.DataXExportTask;
import com.oceanbase.odc.service.datatransfer.task.datax.DataXImportTask;
import com.oceanbase.odc.service.datatransfer.task.obloaderdumper.ObLoaderDumperExportTask;
import com.oceanbase.odc.service.datatransfer.task.obloaderdumper.ObLoaderDumperImportTask;
import com.oceanbase.odc.service.datatransfer.task.sql.SqlExportTask;
import com.oceanbase.odc.service.datatransfer.task.sql.SqlImportTask;

public class DataTransferTaskMapper {

    public Class<? extends DataTransferTask> map(DataTransferConfig config) {
        /*
         * OceanBase connection
         */
        if (config.getType().isOceanBase()) {
            return config.getTransferType() == DataTransferType.EXPORT ? ObLoaderDumperExportTask.class
                    : ObLoaderDumperImportTask.class;
        }
        /*
         * Any other
         */
        if (config.isTransferDDL() || config.getDataTransferFormat() == DataTransferFormat.SQL) {
            return config.getTransferType() == DataTransferType.EXPORT ? SqlExportTask.class : SqlImportTask.class;
        } else {
            return config.getTransferType() == DataTransferType.EXPORT ? DataXExportTask.class : DataXImportTask.class;
        }

    }

}
