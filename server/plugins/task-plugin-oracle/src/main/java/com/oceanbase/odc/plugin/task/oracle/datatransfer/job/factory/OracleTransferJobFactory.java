/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.plugin.task.oracle.datatransfer.job.factory;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import com.oceanbase.odc.plugin.schema.mysql.MySQLFunctionExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLProcedureExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLTableExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.DataXTransferJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobConfiguration;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobContent.Parameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.MySQLReaderPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.MySQLWriterPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.factory.BaseTransferJobFactory;
import com.oceanbase.odc.plugin.task.oracle.datatransfer.job.OracleSchemaExportJob;
import com.oceanbase.odc.plugin.task.oracle.datatransfer.job.OracleSqlScriptImportJob;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/1
 */
public class OracleTransferJobFactory extends BaseTransferJobFactory {

    public OracleTransferJobFactory(DataTransferConfig transferConfig, File workingDir, File logDir, List<URL> inputs) {
        super(transferConfig, workingDir, logDir, inputs);
    }

    @Override
    protected List<DataTransferObject> queryTransferObjects(Connection connection, boolean transferDDL) {
        // TODO: use OracleSchemaExtension
        List<DataTransferObject> objects = new ArrayList<>();
        new MySQLTableExtension().list(connection, transferConfig.getSchemaName())
                .forEach(table -> objects.add(new DataTransferObject(ObjectType.TABLE, table.getName())));
        if (transferDDL) {
            new MySQLViewExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(view -> objects.add(new DataTransferObject(ObjectType.VIEW, view.getName())));
            new MySQLFunctionExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(func -> objects.add(new DataTransferObject(ObjectType.FUNCTION, func.getName())));
            new MySQLProcedureExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(proc -> objects.add(new DataTransferObject(ObjectType.PROCEDURE, proc.getName())));
        }
        return objects;
    }

    @Override
    protected AbstractJob generateSqlScriptImportJob(ObjectResult object, URL url, DataSource dataSource) {
        return new OracleSqlScriptImportJob(object, transferConfig, url, dataSource);
    }

    @Override
    protected AbstractJob generateSchemaExportJob(ObjectResult object, DataSource dataSource) {
        return new OracleSchemaExportJob(object, transferConfig, workingDir, dataSource);
    }

    @Override
    protected AbstractJob generateDataXImportJob(ObjectResult object, JobConfiguration jobConfiguration) {
        Parameter writer = jobConfiguration.getContent()[0].getWriter();
        MySQLWriterPluginParameter parameter = (MySQLWriterPluginParameter) writer.getParameter();
        parameter.setSession(getSessionOptions());
        writer.setName("oraclewriter");
        return new DataXTransferJob(object, jobConfiguration, workingDir, logDir);
    }

    @Override
    protected AbstractJob generateDataXExportJob(ObjectResult object, JobConfiguration jobConfiguration) {
        Parameter reader = jobConfiguration.getContent()[0].getReader();
        MySQLReaderPluginParameter parameter = (MySQLReaderPluginParameter) reader.getParameter();
        parameter.setSession(getSessionOptions());
        reader.setName("oraclereader");
        return new DataXTransferJob(object, jobConfiguration, workingDir, logDir);
    }

    private List<String> getSessionOptions() {
        return Arrays.asList(
                "alter session set CURRENT_SCHEMA=" + transferConfig.getSchemaName(),
                "alter session set NLS_DATE_FORMAT='yyyy-mm-dd hh24:mi:ss'",
                "alter session set NLS_TIMESTAMP_FORMAT='yyyy-mm-dd hh24:mi:ss'",
                "alter session set NLS_TIMESTAMP_TZ_FORMAT='yyyy-mm-dd hh24:mi:ss'");
    }
}
