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
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.tools.dbbrowser.schema.mysql.MySQLNoGreaterThan5740SchemaAccessor;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

public class TransferJobFactory {
    private static final Pattern DATA_FILE_PATTERN =
            Pattern.compile("^\"?([^\\-\\.]+)\"?(\\.[0-9]+){0,2}\\.(sql|csv|dat|txt)$", Pattern.CASE_INSENSITIVE);

    private final DataTransferConfig transferConfig;
    private final File workingDir;
    private final List<URL> inputs;

    public TransferJobFactory(DataTransferConfig transferConfig, File workingDir, List<URL> inputs) {
        this.transferConfig = transferConfig;
        this.workingDir = workingDir;
        this.inputs = inputs;
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
                        File schemaFile = new File(url.getFile());
                        String filename = schemaFile.getName();
                        String objectName = filename.substring(0, filename.indexOf(Constants.DDL_SUFFIX));
                        ObjectResult object = new ObjectResult(transferConfig.getSchemaName(), objectName,
                                schemaFile.getParentFile().getName().toUpperCase());
                        return new SqlScriptImportJob(object, transferConfig, url, dataSource);
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
            objects = queryTransferObjects(dataSource);
        } else {
            objects = new ArrayList<>(transferConfig.getExportDbObjects());
        }
        objects.forEach(o -> {
            ObjectResult object = new ObjectResult(transferConfig.getSchemaName(), o.getObjectName(),
                    o.getDbObjectType().getName());
            AbstractJob job = new MySQLSchemaExportJobImpl(object, transferConfig, workingDir, dataSource);
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
                File file = new File(url.getFile());
                ObjectResult object;
                if (!transferConfig.isCompressed()) {
                    object = new ObjectResult(transferConfig.getSchemaName(), file.getName(), "FILE");
                } else {
                    Matcher matcher = DATA_FILE_PATTERN.matcher(file.getName());
                    if (file.getName().endsWith(Constants.DDL_SUFFIX) || !matcher.matches()) {
                        continue;
                    }
                    object = new ObjectResult(transferConfig.getSchemaName(), matcher.group(1), "TABLE");
                }

                if (transferConfig.getDataTransferFormat() == DataTransferFormat.CSV) {
                    // TODO use DataX
                } else {
                    jobs.add(new SqlScriptImportJob(object, transferConfig, url, dataSource));
                }
            }
            return jobs;
        }
        /*
         * export
         */
        // TODO use DataX
        return jobs;
    }

    private List<DataTransferObject> queryTransferObjects(DataSource dataSource) {
        List<DataTransferObject> objects = new ArrayList<>();
        MySQLNoGreaterThan5740SchemaAccessor accessor =
                new MySQLNoGreaterThan5740SchemaAccessor(new JdbcTemplate(dataSource));
        accessor.listTables(transferConfig.getSchemaName(), "")
                .forEach(table -> objects.add(new DataTransferObject(ObjectType.TABLE, table.getName())));
        if (transferConfig.isTransferDDL()) {
            accessor.listViews(transferConfig.getSchemaName())
                    .forEach(view -> objects.add(new DataTransferObject(ObjectType.VIEW, view.getName())));
            accessor.listFunctions(transferConfig.getSchemaName())
                    .forEach(func -> objects.add(new DataTransferObject(ObjectType.FUNCTION, func.getName())));
            accessor.listProcedures(transferConfig.getSchemaName())
                    .forEach(proc -> objects.add(new DataTransferObject(ObjectType.PROCEDURE, proc.getName())));
        }
        return objects;
    }

}
