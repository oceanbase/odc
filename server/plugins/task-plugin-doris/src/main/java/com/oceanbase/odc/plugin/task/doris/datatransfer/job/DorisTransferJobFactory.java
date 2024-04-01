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
package com.oceanbase.odc.plugin.task.doris.datatransfer.job;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.oceanbase.odc.plugin.schema.doris.DorisTableExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.DataXTransferJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobConfiguration;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobContent.Parameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.MySQLWriterPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.factory.BaseTransferJobFactory;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

/**
 * @author liuyizhuo.lyz
 * @date 2024/3/28
 */
public class DorisTransferJobFactory extends BaseTransferJobFactory {

    public DorisTransferJobFactory(DataTransferConfig transferConfig,
            File workingDir, File logDir, List<URL> inputs) {
        super(transferConfig, workingDir, logDir, inputs);
    }

    @Override
    protected List<DBTableColumn> queryTableColumns(Connection connection, ObjectResult table) {
        return new DorisTableExtension()
                .getDetail(connection, table.getSchema(), table.getName()).getColumns();
    }

    @Override
    protected List<DataTransferObject> queryTransferObjects(Connection connection, boolean transferDDL) {
        return new DorisTableExtension().list(connection, transferConfig.getSchemaName()).stream()
                .map(table -> new DataTransferObject(ObjectType.TABLE, table.getName()))
                .collect(Collectors.toList());
    }

    @Override
    protected AbstractJob generateSqlScriptImportJob(ObjectResult object, URL url, DataSource dataSource) {
        return new DorisScriptImportJob(object, transferConfig, url, dataSource);
    }

    @Override
    protected AbstractJob generateSchemaExportJob(ObjectResult object, DataSource dataSource) {
        return new DorisSchemaExportJob(object, transferConfig, workingDir, dataSource);
    }

    @Override
    protected AbstractJob generateDataXImportJob(ObjectResult object, JobConfiguration jobConfiguration) {
        Parameter writer = jobConfiguration.getContent()[0].getWriter();
        MySQLWriterPluginParameter parameter = (MySQLWriterPluginParameter) writer.getParameter();
        parameter.setPreSql(null);
        parameter.setPostSql(null);
        return new DataXTransferJob(object, jobConfiguration, workingDir, logDir);
    }

    @Override
    protected AbstractJob generateDataXExportJob(ObjectResult object, JobConfiguration jobConfiguration) {
        return new DataXTransferJob(object, jobConfiguration, workingDir, logDir);
    }
}
