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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.db.model.DBRecycleObject;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBRecyclebinService {

    private static final Set<String> SUPPORTED_TYPES = new HashSet<>();

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

    public String getFlashbackSql(@NonNull ConnectionSession session, List<DBRecycleObject> recycleObjects) {
        PreConditions.notEmpty(recycleObjects, "recycleObjectList");
        // FLASHBACK TABLE object_name TO BEFORE DROP [RENAME TO db_name.table_name];
        SqlBuilder sqlBuilder = getBuilder(session);
        for (DBRecycleObject recycleObject : recycleObjects) {
            PreConditions.notBlank(recycleObject.getObjName(), "recycleObject.objName");
            PreConditions.notBlank(recycleObject.getObjType(), "recycleObject.objType");
            String type = recycleObject.getObjType().toLowerCase();
            if (!SUPPORTED_TYPES.contains(type.toUpperCase())) {
                String supportedTypes = String.join(",", SUPPORTED_TYPES);
                sqlBuilder.append("-- ").append(ErrorCodes.ObInvalidObjectTypesForRecyclebin.getLocalizedMessage(
                        new Object[] {recycleObject.getObjName(), type.toUpperCase(), supportedTypes}));
                continue;
            }
            if ("view".equalsIgnoreCase(type)) {
                type = "table";
            }
            sqlBuilder.append("flashback ").append(type).append(" ");
            if (StringUtils.isNotBlank(recycleObject.getSchema())) {
                sqlBuilder.identifier(recycleObject.getSchema()).append(".");
            }
            sqlBuilder.identifier(recycleObject.getObjName()).append(" to before drop");
            if (StringUtils.isNotBlank(recycleObject.getNewName())) {
                sqlBuilder.append(" rename to ").identifier(recycleObject.getNewName());
            }
            sqlBuilder.append(";\r\n");
        }
        return sqlBuilder.toString();
    }

    public String getPurgeSql(@NonNull ConnectionSession session, List<DBRecycleObject> recycleObjects) {
        PreConditions.notEmpty(recycleObjects, "recycleObjectList");
        // purge table objName;
        SqlBuilder sqlBuilder = getBuilder(session);
        for (DBRecycleObject recycleObject : recycleObjects) {
            PreConditions.notBlank(recycleObject.getObjName(), "recycleObject.objName");
            PreConditions.notBlank(recycleObject.getObjType(), "recycleObject.objType");
            String type = recycleObject.getObjType().toLowerCase();
            if ("view".equalsIgnoreCase(type)) {
                type = "table";
            } else if ("normal index".equalsIgnoreCase(type)) {
                type = "index";
            }
            sqlBuilder.append("purge ").append(type).append(" ")
                    .identifier(recycleObject.getObjName()).append(";\r\n");
        }
        return sqlBuilder.toString();
    }

    public String getPurgeAllSql() {
        return "purge recyclebin;";
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
