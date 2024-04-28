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
package com.oceanbase.odc.service.common.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.lang.Validate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author wenniu.ly
 * @date 2021/11/3
 */
public class PackageValidator {

    /**
     * check if the package object is existed
     *
     * @param connectionSession
     * @param objectName
     * @param objectType
     * @param owner
     * @return boolean result
     * @exception Exception exception will be thrown when error occured
     */
    public static boolean exists(ConnectionSession connectionSession, String objectName, DBObjectType objectType,
            String owner) throws Exception {
        Validate.notNull(connectionSession, "connectionSession can not be null for PackageValidator.exist");
        Validate.notEmpty(objectName, "ObjectName can not be empty for PackageValidator.exist");
        Validate.notNull(objectType, "ObjectType can not be null for PackageValidator.exist");
        Validate.notEmpty(owner, "DatabaseOwner can not be empty for PackageValidator.exist");

        String validateSql =
                String.format("select count(1) from all_source where owner=%s and name=%s and type=%s",
                        StringUtils.quoteOracleValue(owner),
                        StringUtils.quoteOracleValue(objectName), StringUtils.quoteOracleValue(objectType.getName()));

        Integer totalCount = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY)
                .queryForObject(validateSql,
                        (rs, i) -> {
                            return rs.getInt(1);
                        });

        return totalCount != null && totalCount >= 1;
    }

    /**
     * check if the package object is existed and with valid status
     *
     * @param connectionSession
     * @param objectName
     * @param objectType
     * @param owner
     * @return boolean result
     * @exception Exception exception will be thrown when error occured
     */
    public static boolean isValid(ConnectionSession connectionSession, String objectName, DBObjectType objectType,
            String owner) throws Exception {
        boolean exist = exists(connectionSession, objectName, objectType, owner);
        if (!exist) {
            return false;
        }
        String validateSql =
                String.format(
                        "select object_name, status from all_objects "
                                + "WHERE object_name = %s and object_type = %s and owner = %s;",
                        StringUtils.quoteOracleValue(objectName), StringUtils.quoteOracleValue(objectType.getName()),
                        StringUtils.quoteOracleValue(owner));
        String validStr = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY)
                .queryForObject(validateSql,
                        (rs, i) -> {
                            return rs.getString("status");
                        });

        return OdcConstants.PL_OBJECT_STATUS_VALID.equalsIgnoreCase(validStr);

    }

    public static boolean isVersionValid(ConnectionSession connectionSession, String objectName,
            String versionInfo, String userName)
            throws Exception {
        String sourceTable = "all_source";
        String objectsTable = "all_objects";
        if ("SYS".equalsIgnoreCase(userName)) {
            sourceTable = "dba_source";
            objectsTable = "dba_objects";
        }
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        String sql = String.format(
                "select s.* , o.created, o.last_ddl_time, o.status from  (select * from %s where object_type='PACKAGE BODY') o "
                        + "right join %s s on s.name = o.object_name and s.owner = o.owner "
                        + "and s.type =o.object_type "
                        + "where s.owner=%s and s.name=%s;",
                objectsTable, sourceTable,
                StringUtils.quoteOracleValue(schema),
                StringUtils.quoteOracleValue(objectName));
        String ddl = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY)
                .query(sql, rs -> (rs.next() ? rs.getString(5) : null));
        return StringUtils.isNotBlank(ddl) && ddl.contains(versionInfo);
    }

    public static boolean isVersionValid(Connection connection, String schema, String objectName, String versionInfo)
            throws Exception {
        String sourceTable = "all_source";
        String objectsTable = "all_objects";

        String sql = String.format(
                "select s.* , o.created, o.last_ddl_time, o.status from  (select * from %s where object_type='PACKAGE BODY') o "
                        + "right join %s s on s.name = o.object_name and s.owner = o.owner "
                        + "and s.type =o.object_type "
                        + "where s.owner=%s and s.name=%s;",
                objectsTable, sourceTable,
                StringUtils.quoteOracleValue(schema),
                StringUtils.quoteOracleValue(objectName));
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                if (!resultSet.next()) {
                    throw new NullPointerException();
                }
                String ddl = resultSet.getString("TEXT");
                return StringUtils.isNotBlank(ddl) && ddl.contains(versionInfo);
            }
        }
    }
}
