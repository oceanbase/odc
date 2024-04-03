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
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/31
 */
public abstract class BaseSchemaExportJob extends AbstractJob {

    protected final DataTransferConfig transferConfig;
    protected final File workingDir;
    protected final DataSource dataSource;

    public BaseSchemaExportJob(ObjectResult object, DataTransferConfig transferConfig, File workingDir,
            DataSource dataSource) {
        super(object);
        this.transferConfig = transferConfig;
        this.workingDir = workingDir;
        this.dataSource = dataSource;
    }


    @Override
    public void run() throws Exception {
        increaseTotal(1);
        /*
         * build ddl
         */
        StringBuilder content = new StringBuilder();
        // 1. append DROP statement
        if (transferConfig.isWithDropDDL()) {
            content.append(getDropStatement());
        }
        // 2. append DELIMITER if it is a PL SQL
        if (isPlObject()) {
            content.append(Constants.PL_DELIMITER_STMT);
        }
        // 3. append CREATE statement
        content.append(queryDdlForDBObject());
        // 4. append '$$' if it is a PL SQL; The end of sql assembling
        if (isPlObject()) {
            content.append(Constants.LINE_BREAKER).append(Constants.DEFAULT_PL_DELIMITER);
        }
        /*
         * touch file
         */
        File output = Paths.get(workingDir.getPath(), "data", object.getType(),
                object.getName() + Constants.DDL_SUFFIX).toFile();
        if (!output.getParentFile().exists()) {
            FileUtils.forceMkdir(output.getParentFile());
        }
        FileUtils.touch(output);
        /*
         * overwrite content
         */
        FileUtils.write(output, content.toString(), transferConfig.getEncoding().getAlias(), false);
        object.setExportPaths(Collections.singletonList(output.toURI().toURL()));

        setStatus(Status.SUCCESS);
        increaseCount(1);
    }

    abstract protected String getDropStatement();

    abstract protected boolean isPlObject();

    abstract protected String queryDdlForDBObject() throws SQLException;

}
