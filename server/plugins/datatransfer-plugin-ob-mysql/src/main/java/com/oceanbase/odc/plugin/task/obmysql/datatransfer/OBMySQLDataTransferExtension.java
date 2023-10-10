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

package com.oceanbase.odc.plugin.task.obmysql.datatransfer;

import org.pf4j.Extension;

import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferExtensionPoint;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferTask;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory.BaseParameterFactory;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory.DumpParameterFactory;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory.LoadParameterFactory;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.task.ObLoaderDumperExportTask;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.task.ObLoaderDumperImportTask;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;

@Extension
public class OBMySQLDataTransferExtension implements DataTransferExtensionPoint {

    @Override
    public DataTransferTask build(DataTransferConfig config) throws Exception {
        boolean transferData = config.isTransferData();
        boolean transferSchema = config.isTransferDDL();

        if (config.getTransferType() == DataTransferType.IMPORT) {
            BaseParameterFactory<LoadParameter> factory = new LoadParameterFactory(config);
            LoadParameter parameter = factory.generate();
            return new ObLoaderDumperImportTask(parameter, transferData, transferSchema, config.isUsePrepStmts());
        } else if (config.getTransferType() == DataTransferType.EXPORT) {
            BaseParameterFactory<DumpParameter> factory = new DumpParameterFactory(config);
            DumpParameter parameter = factory.generate();
            return new ObLoaderDumperExportTask(parameter, transferData, transferSchema, config.isUsePrepStmts());
        }

        throw new IllegalArgumentException("Illegal transfer type " + config.getTransferType());
    }

}
