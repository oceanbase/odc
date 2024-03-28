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
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.oceanbase.odc.plugin.schema.doris.DorisTableExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.MySQLSchemaExportJob;

/**
 * @author liuyizhuo.lyz
 * @date 2024/3/28
 */
public class DorisSchemaExportJob extends MySQLSchemaExportJob {

    public DorisSchemaExportJob(ObjectResult object,
            DataTransferConfig transferConfig, File workingDir,
            DataSource dataSource) {
        super(object, transferConfig, workingDir, dataSource);
    }

    @Override
    protected String queryDdlForDBObject() throws SQLException {
        if (!"TABLE".equals(object.getType())) {
            throw new UnsupportedOperationException("Only support table for doris");
        }
        try (Connection conn = dataSource.getConnection()) {
            return new DorisTableExtension().getDetail(conn, object.getSchema(), object.getName()).getDDL();
        }
    }
}
