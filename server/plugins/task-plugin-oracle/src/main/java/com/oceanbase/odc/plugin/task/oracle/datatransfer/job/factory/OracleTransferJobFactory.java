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
package com.oceanbase.odc.plugin.task.oracle.datatransfer.job.factory;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.google.common.collect.Lists;
import com.oceanbase.odc.plugin.schema.oracle.OracleFunctionExtension;
import com.oceanbase.odc.plugin.schema.oracle.OraclePackageExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleProcedureExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleSequenceExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleSynonymExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleTableExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleTriggerExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleTypeExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.DataXTransferJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobConfiguration;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobContent.Parameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.MySQLReaderPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.MySQLWriterPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.TxtWriterPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.factory.BaseTransferJobFactory;
import com.oceanbase.odc.plugin.task.oracle.datatransfer.job.OracleSchemaExportJob;
import com.oceanbase.odc.plugin.task.oracle.datatransfer.job.OracleSqlScriptImportJob;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
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
    protected List<DBTableColumn> queryTableColumns(Connection connection, ObjectResult table) {
        return new OracleTableExtension()
                .getDetail(connection, table.getSchema(), table.getName()).getColumns();
    }

    @Override
    protected List<DataTransferObject> queryTransferObjects(Connection connection, boolean transferDDL) {
        List<DataTransferObject> objects = new ArrayList<>();
        new OracleTableExtension().list(connection, transferConfig.getSchemaName())
                .forEach(table -> objects.add(new DataTransferObject(ObjectType.TABLE, table.getName())));
        if (transferDDL) {
            new OracleViewExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(view -> objects.add(new DataTransferObject(ObjectType.VIEW, view.getName())));
            new OracleFunctionExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(func -> objects.add(new DataTransferObject(ObjectType.FUNCTION, func.getName())));
            new OracleProcedureExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(proc -> objects.add(new DataTransferObject(ObjectType.PROCEDURE, proc.getName())));
            new OracleSequenceExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(seq -> objects.add(new DataTransferObject(ObjectType.SEQUENCE, seq.getName())));
            new OracleTriggerExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(trg -> objects.add(new DataTransferObject(ObjectType.TRIGGER, trg.getName())));
            new OraclePackageExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(pkg -> objects.add(new DataTransferObject(ObjectType.PACKAGE, pkg.getName())));
            new OraclePackageExtension().listPackageBodies(connection, transferConfig.getSchemaName())
                    .forEach(pkg -> objects.add(new DataTransferObject(ObjectType.PACKAGE_BODY, pkg.getName())));
            new OracleSynonymExtension().list(connection, transferConfig.getSchemaName(), DBSynonymType.COMMON)
                    .forEach(syn -> objects.add(new DataTransferObject(ObjectType.SYNONYM, syn.getName())));
            new OracleSynonymExtension().list(connection, transferConfig.getSchemaName(), DBSynonymType.PUBLIC)
                    .forEach(syn -> objects.add(new DataTransferObject(ObjectType.SYNONYM, syn.getName())));
            new OracleTypeExtension().list(connection, transferConfig.getSchemaName())
                    .forEach(typ -> objects.add(new DataTransferObject(ObjectType.TYPE, typ.getName())));
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
        parameter.setSession(getSessionOptions(object));
        parameter.setWriteMode(null);
        parameter.setPreSql(null);
        parameter.setPostSql(null);
        writer.setName("oraclewriter");
        return new DataXTransferJob(object, jobConfiguration, workingDir, logDir);
    }

    @Override
    protected AbstractJob generateDataXExportJob(ObjectResult object, JobConfiguration jobConfiguration) {
        Parameter reader = jobConfiguration.getContent()[0].getReader();
        MySQLReaderPluginParameter readerParameter = (MySQLReaderPluginParameter) reader.getParameter();
        readerParameter.setSession(getSessionOptions(object));
        reader.setName("oraclereader");
        Parameter writer = jobConfiguration.getContent()[0].getWriter();
        TxtWriterPluginParameter writerParameter = (TxtWriterPluginParameter) writer.getParameter();
        writerParameter.setQuoteChar("\"");
        return new DataXTransferJob(object, jobConfiguration, workingDir, logDir);
    }

    private List<String> getSessionOptions(ObjectResult object) {
        ArrayList<String> sessionOptions = Lists.newArrayList(
                "alter session set CURRENT_SCHEMA=" + transferConfig.getSchemaName(),
                "alter session set nls_date_format='YYYY-MM-DD HH24:MI:SS'",
                "alter session set nls_timestamp_format='YYYY-MM-DD HH24:MI:SS:FF9'");
        if (transferConfig.isTruncateTableBeforeImport()) {
            OracleSqlBuilder builder = new OracleSqlBuilder();
            builder.append("TRUNCATE TABLE ").identifier(object.getName());
            sessionOptions.add(builder.toString());
        }
        return sessionOptions;
    }
}
