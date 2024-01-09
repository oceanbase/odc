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
package com.oceanbase.odc.plugin.schema.oboracle;

import java.sql.Connection;
import java.util.List;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.api.PackageExtensionPoint;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.oracle.OraclePackageTemplate;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBOraclePackageExtension implements PackageExtensionPoint {

    @Override
    public List<DBPLObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName) {
        return getSchemaAccessor(connection).listPackages(schemaName);
    }

    @Override
    public List<DBPLObjectIdentity> listPackageBodies(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).listPackageBodies(schemaName);
    }

    @Override
    public DBPackage getDetail(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull String packageName) {
        return getSchemaAccessor(connection).getPackage(schemaName, packageName);
    }

    @Override
    public void dropPackage(@NonNull Connection connection, String schemaName, @NonNull String packageName) {
        OracleObjectOperator operator = new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
        operator.drop(DBObjectType.PACKAGE, null, packageName);
    }

    @Override
    public void dropPackageBody(@NonNull Connection connection, String schemaName, @NonNull String packageName) {
        OracleObjectOperator operator = new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
        operator.drop(DBObjectType.PACKAGE_BODY, null, packageName);
    }

    @Override
    public String generateCreateTemplate(@NonNull Connection connection, @NonNull DBPackage dbPackage) {
        OraclePackageTemplate template = new OraclePackageTemplate(JdbcOperationsUtil.getJdbcOperations(connection));
        return template.generateCreateObjectTemplate(dbPackage);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }
}
