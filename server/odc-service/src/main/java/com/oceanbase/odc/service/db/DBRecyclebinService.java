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
package com.oceanbase.odc.service.db;

import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.service.db.model.DBRecycleObject;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBRecyclebinService {

    private static final Set<String> SUPPORTED_TYPES = new HashSet<>();
    private static final String EMPTY_SCHEMA = "";

    static {
        SUPPORTED_TYPES.add("TABLE");
        SUPPORTED_TYPES.add("VIEW");
        SUPPORTED_TYPES.add("TENANT");
        SUPPORTED_TYPES.add("DATABASE");
        SUPPORTED_TYPES.add("SCHEMA");
    }

    public List<DBRecycleObject> list(@NonNull ConnectionSession session) {
        if (session.getConnectType().isODPSharding()) {
            throw new UnsupportedException("RecycleBin not supported for ODP sharding mode");
        }
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        if (session.getDialectType().isOracle()) {
            SqlBuilder sqlBuilder = new OracleSqlBuilder();
            sqlBuilder.append("SELECT ")
                    .append("OBJECT_NAME,")
                    .append("ORIGINAL_NAME,")
                    .append("TYPE,")
                    .append("CREATETIME,")
                    .append("OWNER ")
                    .append("FROM DBA_RECYCLEBIN");
            return jdbcOperations.query(sqlBuilder.toString(), (rs, rowNum) -> {
                DBRecycleObject returnVal = new DBRecycleObject();
                returnVal.setObjName(rs.getString(1));
                returnVal.setObjType(rs.getString(3));
                returnVal.setOriginName(rs.getString(2));
                returnVal.setCreateTime(rs.getString(4));
                returnVal.setSchema(rs.getString(5));
                return returnVal;
            });
        } else if (session.getDialectType().isMysql()) {
            SqlBuilder sqlBuilder = new MySQLSqlBuilder();
            final boolean withDatabase;
            if (VersionUtils.isGreaterThanOrEqualsTo(ConnectionSessionUtil.getVersion(session), "2.2.0")) {
                withDatabase = true;
                sqlBuilder.append("select ")
                        .append("r.object_name,")
                        .append("r.original_name,")
                        .append("r.type,")
                        .append("r.gmt_create,")
                        .append("d.database_name ")
                        .append("from oceanbase.__all_recyclebin r,oceanbase.__all_database d ")
                        .append("where d.database_id=r.database_id");
            } else {
                withDatabase = false;
                sqlBuilder.append("show recyclebin");
            }
            return jdbcOperations.query(sqlBuilder.toString(), (rs, rowNum) -> {
                DBObjectType type;
                String schema = null;
                if (withDatabase) {
                    type = getObjectType(rs.getInt(3));
                    schema = rs.getString(5);
                } else {
                    type = DBObjectType.getEnumByName(rs.getString(3));
                }
                DBRecycleObject returnVal = new DBRecycleObject();
                returnVal.setObjName(rs.getString(1));
                returnVal.setObjType(type.getName());
                returnVal.setOriginName(rs.getString(2));
                returnVal.setCreateTime(rs.getString(4));
                returnVal.setSchema(schema);
                return returnVal;
            });
        } else {
            throw new IllegalArgumentException("Unsupported dialect type, " + session.getDialectType());
        }
    }

    public void flashback(@NonNull ConnectionSession session, List<DBRecycleObject> recycleObjects) {
        PreConditions.notEmpty(recycleObjects, "recycleObjectList");
        // FLASHBACK TABLE object_name TO BEFORE DROP [RENAME TO db_name.table_name];
        Map<String, List<String>> schema2Sqls = recycleObjects.stream().collect(Collectors.groupingBy(i -> {
            return StringUtils.isEmpty(i.getSchema()) ? EMPTY_SCHEMA : i.getSchema();
        })).entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().stream().map(i -> {
            SqlBuilder sqlBuilder = getBuilder(session);
            PreConditions.notBlank(i.getObjName(), "recycleObject.objName");
            PreConditions.notBlank(i.getObjType(), "recycleObject.objType");
            String type = i.getObjType().toLowerCase();
            if (!SUPPORTED_TYPES.contains(type.toUpperCase())) {
                throw new BadRequestException(ErrorCodes.ObInvalidObjectTypesForRecyclebin, new Object[] {
                        i.getObjName(), type.toUpperCase(), String.join(",", SUPPORTED_TYPES)}, "Illegal object");
            }
            if ("view".equalsIgnoreCase(type)) {
                type = "table";
            }
            sqlBuilder.append("flashback ").append(type).append(" ");
            if (StringUtils.isNotBlank(i.getSchema())) {
                sqlBuilder.identifier(i.getSchema()).append(".");
            }
            sqlBuilder.identifier(i.getObjName()).append(" to before drop");
            if (StringUtils.isNotBlank(i.getNewName())) {
                sqlBuilder.append(" rename to ").identifier(i.getSchema()).append(".").identifier(i.getNewName());
            }
            return sqlBuilder.toString();
        }).collect(Collectors.toList())));
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        jdbcOperations.execute((ConnectionCallback<Void>) con -> {
            for (Entry<String, List<String>> entry : schema2Sqls.entrySet()) {
                try (Statement statement = con.createStatement()) {
                    for (String sql : entry.getValue()) {
                        statement.execute(sql);
                    }
                }
            }
            return null;
        });
    }

    public void purgeObject(@NonNull ConnectionSession session, @NonNull List<DBRecycleObject> recycleObjects) {
        SessionExtensionPoint extensionPoint = ConnectionPluginUtil.getSessionExtension(session.getDialectType());
        if (extensionPoint == null) {
            throw new IllegalStateException("failed to get plugin");
        }
        Map<String, List<String>> schema2Sqls = recycleObjects.stream().collect(Collectors.groupingBy(i -> {
            return StringUtils.isEmpty(i.getSchema()) ? EMPTY_SCHEMA : i.getSchema();
        })).entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().stream().map(i -> {
            SqlBuilder sqlBuilder = getBuilder(session);
            PreConditions.notBlank(i.getObjName(), "recycleObject.objName");
            PreConditions.notBlank(i.getObjType(), "recycleObject.objType");
            String type = i.getObjType().toLowerCase();
            if ("view".equalsIgnoreCase(type)) {
                type = "table";
            } else if ("normal index".equalsIgnoreCase(type)) {
                type = "index";
            }
            return sqlBuilder.append("purge ").append(type).append(" ").identifier(i.getObjName()).toString();
        }).collect(Collectors.toList())));
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        jdbcOperations.execute((ConnectionCallback<Void>) con -> {
            String schema = extensionPoint.getCurrentSchema(con);
            try {
                for (Entry<String, List<String>> entry : schema2Sqls.entrySet()) {
                    if (!EMPTY_SCHEMA.equals(entry.getKey())) {
                        extensionPoint.switchSchema(con, entry.getKey());
                    }
                    try (Statement statement = con.createStatement()) {
                        for (String sql : entry.getValue()) {
                            statement.execute(sql);
                        }
                    }
                }
            } finally {
                if (StringUtils.isNotEmpty(schema)) {
                    extensionPoint.switchSchema(con, schema);
                }
            }
            return null;
        });
    }

    public void purgeAllObjects(@NonNull ConnectionSession session) {
        session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute("purge recyclebin");
    }

    private SqlBuilder getBuilder(ConnectionSession session) {
        if (session.getDialectType().isMysql()) {
            return new MySQLSqlBuilder();
        } else if (session.getDialectType().isOracle()) {
            return new OracleSqlBuilder();
        }
        throw new IllegalArgumentException("Unsupported dialect type, " + session.getDialectType());
    }

    private DBObjectType getObjectType(int type) {
        switch (type) {
            case 1:
                return DBObjectType.TABLE;
            case 2:
                return DBObjectType.INDEX;
            case 3:
                return DBObjectType.VIEW;
            case 4:
                return DBObjectType.DATABASE;
            default:
                return DBObjectType.OTHERS;
        }
    }

}
