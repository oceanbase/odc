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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
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
    private final DBSchemaAccessor accessor;
    private final ConnectionSession session;

    private DBObjectNameAccessor(@NonNull DBSchemaAccessor accessor, @NonNull ConnectionSession session,
            @NonNull String schema) {
        this.schema = schema;
        this.accessor = accessor;
        this.session = session;
    }

    public static DBObjectNameAccessor getInstance(@NonNull ConnectionConfig connection, @NonNull String schema) {
        ConnectionSession session = new DefaultConnectSessionFactory(connection).generateSession();
        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        return new DBObjectNameAccessor(accessor, session, schema);
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
            default:
                throw new UnsupportedOperationException("Unsupported object type " + objectType);
        }
    }

    public Set<String> getTableNames() {
        return accessor.showTables(schema).stream()
                .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                .collect(Collectors.toSet());
    }

    public Set<String> getViewNames() {
        return accessor.listViews(schema).stream().map(DBObjectIdentity::getName).collect(Collectors.toSet());
    }

    public Set<String> getTriggerNames() {
        if (session.getDialectType().isMysql()) {
            return Collections.emptySet();
        }
        return accessor.listTriggers(schema).stream().map(DBObjectIdentity::getName).collect(Collectors.toSet());
    }

    public Set<String> getFunctionNames() {
        try {
            return accessor.listFunctions(schema).stream().map(DBObjectIdentity::getName).collect(Collectors.toSet());
        } catch (UnsupportedOperationException e) {
            return Collections.emptySet();
        }
    }

    public Set<String> getProcedureNames() {
        try {
            return accessor.listProcedures(schema).stream().map(DBObjectIdentity::getName).collect(Collectors.toSet());
        } catch (UnsupportedOperationException e) {
            return Collections.emptySet();
        }
    }

    public Set<String> getSequenceNames() {
        try {
            return accessor.listSequences(schema).stream().map(DBObjectIdentity::getName).collect(Collectors.toSet());
        } catch (UnsupportedOperationException e) {
            return Collections.emptySet();
        }
    }

    public Set<String> getSynonymNames() {
        if (session.getDialectType().isMysql()) {
            return Collections.emptySet();
        }
        return accessor.listSynonyms(schema, DBSynonymType.COMMON).stream().map(DBObjectIdentity::getName)
                .collect(Collectors.toSet());
    }

    public Set<String> getPublicSynonymNames() {
        if (session.getDialectType().isMysql()) {
            return Collections.emptySet();
        }
        return accessor.listSynonyms(schema, DBSynonymType.PUBLIC).stream().map(DBObjectIdentity::getName)
                .collect(Collectors.toSet());
    }

    public Set<String> getPackageNames() {
        if (session.getDialectType().isMysql()) {
            return Collections.emptySet();
        }
        return accessor.listPackages(schema).stream()
                .filter(i -> !StringUtils.equalsIgnoreCase(i.getName(), OdcConstants.PL_DEBUG_PACKAGE))
                .filter(e -> e.getType().name().equals(ObjectType.PACKAGE.name()))
                .map(DBObjectIdentity::getName).collect(Collectors.toSet());
    }

    public Set<String> getPackageBodyNames() {
        if (session.getDialectType().isMysql()) {
            return Collections.emptySet();
        }
        return accessor.listPackages(schema).stream()
                .filter(i -> !StringUtils.equalsIgnoreCase(i.getName(), OdcConstants.PL_DEBUG_PACKAGE))
                .filter(e -> e.getType().name().equals(ObjectType.PACKAGE_BODY.name())).map(DBObjectIdentity::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public void close() {
        this.session.expire();
    }

    public String getDBVersion() {
        return ConnectionSessionUtil.getVersion(session);
    }
}
