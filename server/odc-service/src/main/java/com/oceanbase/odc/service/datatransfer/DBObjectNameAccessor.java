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
package com.oceanbase.odc.service.datatransfer;

import java.util.List;
import java.sql.Connection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.ConnectionCallback;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DBObjectNameAccessor}
 *
 * @author yh263208
 * @date 2022-08-01 17:25
 * @since ODC_release_3.4.0
 */
@Slf4j
public class DBObjectNameAccessor implements AutoCloseable {

    private final String schema;
    private final ConnectionSession session;
    private final DialectType dialectType;

    private DBObjectNameAccessor(@NonNull ConnectionSession session,
            @NonNull String schema) {
        this.schema = schema;
        this.session = session;
        this.dialectType = session.getDialectType();
    }

    public static DBObjectNameAccessor getInstance(@NonNull ConnectionConfig connection, @NonNull String schema) {
        ConnectionSession session = new DefaultConnectSessionFactory(connection).generateSession();
        return new DBObjectNameAccessor(session, schema);
    }

    public Set<String> getObjectNames(@NonNull ObjectType objectType) {
        switch (objectType) {
            case TABLE:
                return getTableNames();
            case VIEW:
                return getViewNames();
            case PROCEDURE:
                return getProcedureNames();
            case FUNCTION:
                return getFunctionNames();
            case TRIGGER:
                return getTriggerNames();
            case SEQUENCE:
                return getSequenceNames();
            case SYNONYM:
                return getSynonymNames();
            case PUBLIC_SYNONYM:
                return getPublicSynonymNames();
            case PACKAGE:
                return getPackageNames();
            case PACKAGE_BODY:
                return getPackageBodyNames();
            case TYPE:
                return getTypeNames();
            default:
                throw new UnsupportedOperationException("Unsupported object type " + objectType);
        }
    }

    public Set<String> getTableNames() {
        return queryNames(conn -> SchemaPluginUtil.getTableExtension(dialectType)
                .list(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                        .collect(Collectors.toSet());
    }

    public Set<String> getViewNames() {
        return queryNames(conn -> SchemaPluginUtil.getViewExtension(dialectType)
                .list(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .collect(Collectors.toSet());
    }

    public Set<String> getTriggerNames() {
        return queryNames(conn -> SchemaPluginUtil.getTriggerExtension(dialectType)
                .list(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .collect(Collectors.toSet());
    }

    public Set<String> getFunctionNames() {
        return queryNames(conn -> SchemaPluginUtil.getFunctionExtension(dialectType)
                .list(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .collect(Collectors.toSet());
    }

    public Set<String> getProcedureNames() {
        return queryNames(conn -> SchemaPluginUtil.getProcedureExtension(dialectType)
                .list(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .collect(Collectors.toSet());
    }

    public Set<String> getSequenceNames() {
        return queryNames(conn -> SchemaPluginUtil.getSequenceExtension(dialectType)
                .list(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .collect(Collectors.toSet());
    }

    public Set<String> getSynonymNames() {
        return queryNames(conn -> SchemaPluginUtil.getSynonymExtension(dialectType)
                .list(conn, schema, DBSynonymType.COMMON)).stream()
                        .map(DBObjectIdentity::getName)
                        .collect(Collectors.toSet());
    }

    public Set<String> getPublicSynonymNames() {
        return queryNames(conn -> SchemaPluginUtil.getSynonymExtension(dialectType)
                .list(conn, schema, DBSynonymType.PUBLIC)).stream()
                        .map(DBObjectIdentity::getName)
                        .collect(Collectors.toSet());
    }

    public Set<String> getPackageNames() {
        return queryNames(conn -> SchemaPluginUtil.getPackageExtension(dialectType)
                .list(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .filter(name -> !StringUtils.equalsIgnoreCase(name, OdcConstants.PL_DEBUG_PACKAGE))
                        .collect(Collectors.toSet());
    }

    public Set<String> getPackageBodyNames() {
        return queryNames(conn -> SchemaPluginUtil.getPackageExtension(dialectType)
                .listPackageBodies(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .filter(name -> !StringUtils.equalsIgnoreCase(name, OdcConstants.PL_DEBUG_PACKAGE))
                        .collect(Collectors.toSet());
    }

    public Set<String> getTypeNames() {
        return queryNames(conn -> SchemaPluginUtil.getTypeExtension(dialectType)
                .list(conn, schema)).stream()
                        .map(DBObjectIdentity::getName)
                        .collect(Collectors.toSet());
    }

    private <T extends DBObjectIdentity> List<T> queryNames(ConnectionCallback<List<T>> consumer) {
        return session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY)
                .execute(consumer);
    }

    @Override
    public void close() {
        this.session.expire();
    }

}
