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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.DataSourceManager;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.data.SqlDataImportJobImpl;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.data.SqlFileImportJobImpl;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.schema.MySQLSchemaExportJobImpl;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.schema.MySQLSchemaImportJobImpl;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.resource.LocalResource;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.resource.LocalResourceFinder;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.resource.Resource;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.resource.ResourceFinder;
import com.oceanbase.tools.dbbrowser.schema.mysql.MySQLNoGreaterThan5740SchemaAccessor;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

public class TransferJobFactory {

    private final DataTransferConfig transferConfig;
    private final File workingDir;
    private final ResourceFinder<Resource> resourceFinder;
    private final List<URL> inputs;

    public TransferJobFactory(DataTransferConfig transferConfig, File workingDir, List<URL> inputs) {
        this.transferConfig = transferConfig;
        this.workingDir = workingDir;
        this.inputs = inputs;
        this.resourceFinder = getResourceFinder();
    }

    public List<AbstractJob> generateSchemaTransferJobs() throws Exception {
        List<AbstractJob> jobs = new ArrayList<>();
        /*
         * import
         */
        if (transferConfig.getTransferType() == DataTransferType.IMPORT) {
            List<Resource> resources = resourceFinder.listSchemaResources();
            resources.stream()
                    .sorted(Comparator
                            .comparingInt(r -> ArrayUtils.indexOf(Constants.DEPENDENCIES, r.getObjectType())))
                    .forEach(r -> {
                        ObjectResult object = new ObjectResult(transferConfig.getSchemaName(), r.getObjectName(),
                                r.getObjectType());
                        AbstractJob job = new MySQLSchemaImportJobImpl(object, transferConfig, r);
                        jobs.add(job);
                    });
            return jobs;
        }
        /*
         * export
         */
        List<DataTransferObject> objects;
        if (transferConfig.isExportAllObjects()) {
            objects = queryTransferObjects();
        } else {
            objects = new ArrayList<>(transferConfig.getExportDbObjects());
        }
        objects.forEach(o -> {
            ObjectResult object = new ObjectResult(transferConfig.getSchemaName(), o.getObjectName(),
                    o.getDbObjectType().getName());
            AbstractJob job = new MySQLSchemaExportJobImpl(object, transferConfig, workingDir);
            jobs.add(job);
        });
        return jobs;
    }

    public List<AbstractJob> generateDataTransferJobs() throws Exception {
        List<AbstractJob> jobs = new ArrayList<>();
        /*
         * import
         */
        if (transferConfig.getTransferType() == DataTransferType.IMPORT) {
            List<Resource> resources =
                    transferConfig.isCompressed() ? resourceFinder.listRecordResources() : convertInputsToResources();
            if (transferConfig.getDataTransferFormat() == DataTransferFormat.CSV) {
                // TODO use DataX
            } else {
                resources.forEach(r -> {
                    ObjectResult object = new ObjectResult(transferConfig.getSchemaName(), r.getObjectName(),
                            r.getObjectType());
                    AbstractJob job =
                            transferConfig.isCompressed() ? new SqlDataImportJobImpl(object, transferConfig, r)
                                    : new SqlFileImportJobImpl(object, transferConfig, r);
                    jobs.add(job);
                });
            }
            return jobs;
        }
        /*
         * export
         */
        // TODO use DataX
        return jobs;
    }

    private ResourceFinder<Resource> getResourceFinder() {
        return new LocalResourceFinder(transferConfig, workingDir);
    }

    private List<DataTransferObject> queryTransferObjects() {
        List<DataTransferObject> objects = new ArrayList<>();
        DataSource dataSource = DataSourceManager.getInstance().get(transferConfig.getConnectionInfo());
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

    private List<Resource> convertInputsToResources() {
        if (CollectionUtils.isEmpty(inputs)) {
            return Collections.emptyList();
        }
        return inputs.stream().map(url -> {
            LocalResource resource;
            if (transferConfig.getDataTransferFormat() == DataTransferFormat.CSV) {
                String objectName = transferConfig.getExportDbObjects().get(0).getObjectName();
                resource = new LocalResource(Paths.get(url.getFile()), objectName, "TABLE");
            } else {
                String objectName = new File(url.getFile()).getName();
                resource = new LocalResource(Paths.get(url.getFile()), objectName, "FILE");
            }
            return resource;
        }).collect(Collectors.toList());
    }

}
