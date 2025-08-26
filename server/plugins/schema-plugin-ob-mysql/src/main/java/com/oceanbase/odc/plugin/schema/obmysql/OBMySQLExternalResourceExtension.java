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
package com.oceanbase.odc.plugin.schema.obmysql;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import org.pf4j.Extension;

import com.oceanbase.odc.plugin.schema.api.ExternalResourceExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.model.DBExternalResource;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceDetailParam;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceStreamHolder;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceUploadParam;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/8/22 15:33
 * @since: 4.4.1
 */
@Extension
public class OBMySQLExternalResourceExtension implements ExternalResourceExtensionPoint {

    @Override
    public Boolean upload(@NotNull Connection connection, @NotNull DBExternalResourceUploadParam param)
            throws IOException {
        return getSchemaAccessor(connection).uploadExternalResource(param);
    }

    @Override
    public DBExternalResourceStreamHolder download(@NotNull Connection connection, @NotEmpty String schemaName,
            @NotEmpty String resourceName) {
        return getSchemaAccessor(connection).downloadExternalResource(schemaName, resourceName);
    }

    @Override
    public List<DBObjectIdentity> list(@NotNull Connection connection, @NotEmpty String schemaName) {
        return getSchemaAccessor(connection).listExternalResources(schemaName);
    }

    @Override
    public DBExternalResource getDetail(@NotNull Connection connection, @NotNull DBExternalResourceDetailParam param) {
        return getSchemaAccessor(connection).getExternalResource(param);
    }

    @Override
    public Boolean drop(@NotNull Connection connection, @NotEmpty String schemaName, @NotEmpty String resourceName) {
        return getSchemaAccessor(connection).deleteExternalResource(schemaName, resourceName);
    }

    private DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

}
