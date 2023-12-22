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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.ArrayUtils;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.plugin.schema.mysql.MySQLFunctionExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLProcedureExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLTableExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.DataFile;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.ConfigurationResolver;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.DataXTransferJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobConfiguration;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

public class TransferJobFactory {

    private final DataTransferConfig transferConfig;
    private final File workingDir;
    private final File logDir;
    private final List<URL> inputs;
    private final String jdbcUrl;

    public TransferJobFactory(DataTransferConfig transferConfig, File workingDir, File logDir, List<URL> inputs,
            String jdbcUrl) {
        this.transferConfig = transferConfig;
        this.workingDir = workingDir;
        this.logDir = logDir;
        this.inputs = inputs;
        this.jdbcUrl = jdbcUrl;
    }

    public List<AbstractJob> generateSchemaTransferJobs(DataSource dataSource) throws Exception {
        List<AbstractJob> jobs = new ArrayList<>();
        /*
         * import
         */
        if (transferConfig.getTransferType() == DataTransferType.IMPORT) {
            return inputs.stream()
                    .filter(url -> url.getFile().endsWith(Constants.DDL_SUFFIX))
                    .map(url -> {
                        try {
                            File schemaFile = new File(url.toURI());
                            String filename = schemaFile.getName();
                            String objectName = filename.substring(0, filename.indexOf(Constants.DDL_SUFFIX));
                            ObjectResult object = new ObjectResult(transferConfig.getSchemaName(), objectName,
                                    schemaFile.getParentFile().getName().toUpperCase());
                            return new SqlScriptImportJob(object, transferConfig, url, dataSource);
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .sorted(Comparator
                            .comparingInt(job -> ArrayUtils.indexOf(Constants.DEPENDENCIES, job.getObject().getType())))
                    .collect(Collectors.toList());
        }
        /*
         * export
         */
        List<DataTransferObject> objects;
        if (transferConfig.isExportAllObjects()) {
            try (Connection conn = dataSource.getConnection()) {
                objects = queryTransferObjects(conn, true);
            }
        } else {
            objects = new ArrayList<>(transferConfig.getExportDbObjects());
        }
        objects.forEach(o -> {
            ObjectResult object = new ObjectResult(transferConfig.getSchemaName(), o.getObjectName(),
                    o.getDbObjectType().getName());
            AbstractJob job = new MySQLSchemaExportJob(object, transferConfig, workingDir, dataSource);
            jobs.add(job);
        });
        return jobs;
    }

    public List<AbstractJob> generateDataTransferJobs(DataSource dataSource) throws Exception {
        List<AbstractJob> jobs = new ArrayList<>();
        /*
         * import
         */
        if (transferConfig.getTransferType() == DataTransferType.IMPORT) {
            for (URL url : inputs) {
                File file = new File(url.toURI());
                ObjectResult object;
                if (transferConfig.isCompressed()) {
                    Matcher matcher = DataFile.FILE_PATTERN.matcher(file.getName());
                    if (!matcher.matches()) {
                        continue;
                    }
                    object = new ObjectResult(transferConfig.getSchemaName(), matcher.group(1), "TABLE");
                } else if (transferConfig.getDataTransferFormat() == DataTransferFormat.SQL) {
                    object = new ObjectResult(transferConfig.getSchemaName(), file.getName(), "FILE");
                } else {
                    Verify.singleton(transferConfig.getExportDbObjects(), "table");
                    object = new ObjectResult(transferConfig.getSchemaName(),
                            transferConfig.getExportDbObjects().get(0).getObjectName(), "TABLE");
                }

                if (transferConfig.getDataTransferFormat() == DataTransferFormat.CSV) {
                    try (Connection conn = dataSource.getConnection()) {
                        List<DBTableColumn> columns = new MySQLTableExtension()
                                .getDetail(conn, object.getSchema(), object.getName()).getColumns();
                        JobConfiguration jobConf = ConfigurationResolver
                                .buildJobConfigurationForImport(transferConfig, jdbcUrl, object, url, columns);
                        jobs.add(new DataXTransferJob(object, jobConf, workingDir, logDir));
                    }
                } else {
                    jobs.add(new SqlScriptImportJob(object, transferConfig, url, dataSource));
                }
            }
            return jobs;
        }
        /*
         * export
         */
        try (Connection conn = dataSource.getConnection()) {
            List<DataTransferObject> objects;
            if (transferConfig.isExportAllObjects()) {
                objects = queryTransferObjects(conn, false);
            } else {
                objects = new ArrayList<>(transferConfig.getExportDbObjects());
            }
            for (DataTransferObject object : objects) {
                ObjectResult table = new ObjectResult(transferConfig.getSchemaName(), object.getObjectName(),
                        object.getDbObjectType().getName());
                /*
                 * when exporting data, table column names are needed for csv headers and insertion building
                 */
                List<DBTableColumn> columns;
                if (Objects.nonNull(transferConfig.getQuerySql())) {
                    columns = transferConfig.getColumns();
                } else {
                    columns = new MySQLTableExtension()
                            .getDetail(conn, table.getSchema(), table.getName()).getColumns();
                }
                AbstractJob job = new DataXTransferJob(table, ConfigurationResolver
                        .buildJobConfigurationForExport(workingDir, transferConfig, jdbcUrl, table.getName(), columns),
                        workingDir, logDir);
                jobs.add(job);
            }
        }
        return jobs;
    }

    private List<DataTransferObject> queryTransferObjects(Connection connection, boolean transferDDL) {
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

}
