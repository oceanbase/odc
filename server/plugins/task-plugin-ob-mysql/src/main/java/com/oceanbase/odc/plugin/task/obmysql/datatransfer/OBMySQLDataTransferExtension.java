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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.pf4j.Extension;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferExtensionPoint;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferJob;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.ExportOutput;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.UploadFileResult;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory.BaseParameterFactory;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory.DumpParameterFactory;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory.LoadParameterFactory;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.task.OceanBaseExportJob;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.task.OceanBaseImportJob;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.util.ConnectionUtil;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.util.PluginUtil;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;

import lombok.NonNull;

@Extension
public class OBMySQLDataTransferExtension implements DataTransferExtensionPoint {
    @Override
    public DataTransferJob generate(@NonNull DataTransferConfig config, @NonNull File workingDir,
            @NonNull File logDir, @NonNull List<URL> inputs) throws Exception {
        boolean transferData = config.isTransferData();
        boolean transferSchema = config.isTransferDDL();

        if (config.getTransferType() == DataTransferType.IMPORT) {
            BaseParameterFactory<LoadParameter> factory = new LoadParameterFactory(config, workingDir, logDir);
            LoadParameter parameter = factory.generate();
            return new OceanBaseImportJob(parameter, transferData, transferSchema, config.isUsePrepStmts());
        } else if (config.getTransferType() == DataTransferType.EXPORT) {
            BaseParameterFactory<DumpParameter> factory = new DumpParameterFactory(config, workingDir, logDir);
            DumpParameter parameter = factory.generate();
            return new OceanBaseExportJob(parameter, transferData, transferSchema, config.isUsePrepStmts());
        }

        throw new IllegalArgumentException("Illegal transfer type " + config.getTransferType());
    }

    @Override
    public Set<ObjectType> getSupportedObjectTypes(ConnectionInfo connectionInfo) throws SQLException {
        Set<ObjectType> types =
                SetUtils.hashSet(ObjectType.TABLE, ObjectType.VIEW, ObjectType.FUNCTION, ObjectType.PROCEDURE);

        try (SingleConnectionDataSource dataSource = ConnectionUtil.getDataSource(connectionInfo, "");
                Connection connection = dataSource.getConnection()) {
            String dbVersion = PluginUtil.getInformationExtension(connectionInfo).getDBVersion(connection);
            if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "4.0.0")) {
                types.add(ObjectType.SEQUENCE);
            }
        }
        return types;
    }

    @Override
    public Set<DataTransferFormat> getSupportedTransferFormats() {
        return SetUtils.hashSet(DataTransferFormat.SQL, DataTransferFormat.CSV, DataTransferFormat.EXCEL);
    }

    @Override
    public UploadFileResult getImportFileInfo(@NonNull URL url) throws URISyntaxException {
        File file = new File(url.toURI());
        if (StringUtils.endsWithIgnoreCase(file.getName(), ".zip")) {
            try {
                ExportOutput exportOutput = new ExportOutput(file);
                return UploadFileResult.ofExportOutput(file.getName(), exportOutput);
            } catch (Exception e) {
                return UploadFileResult.ofFail(ErrorCodes.ImportInvalidFileType, new Object[] {file.getPath()});
            }
        } else if (StringUtils.endsWithIgnoreCase(file.getName(), ".csv")) {
            return UploadFileResult.ofCsv(file.getName());
        } else if (StringUtils.endsWithIgnoreCase(file.getName(), ".sql")
                || StringUtils.endsWithIgnoreCase(file.getName(), ".txt")) {
            return UploadFileResult.ofSql(file.getName());
        }
        return UploadFileResult.ofFail(ErrorCodes.ImportInvalidFileType, new Object[] {file.getPath()});
    }
}
