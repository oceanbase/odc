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
package com.oceanbase.odc.service.pldebug.operator;

import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.common.util.PackageValidator;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.model.PLIdentity;
import com.oceanbase.odc.service.pldebug.util.PLUtils;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBPackageBasicInfo;
import com.oceanbase.tools.dbbrowser.model.DBPackageDetail;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-02-17
 * @since 4.1.0
 */
@Slf4j
public class OraclePLOperator implements DBPLOperator {
    final SyncJdbcExecutor syncJdbcExecutor;
    final ConnectionSession connectionSession;

    public OraclePLOperator(ConnectionSession connectionSession) {
        this.connectionSession = connectionSession;
        this.syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
    }

    @Override
    public Boolean isSupportPLDebug() {
        try {
            syncJdbcExecutor.execute("CALL DBMS_DEBUG.PING()");
        } catch (DataAccessException dataAccessException) {
            log.warn("CALL DBMS_DEBUG.PING() occur error {}", dataAccessException.getMessage());
            String result = dataAccessException.getMessage();
            if (result != null && result.contains("Unknown")) {
                return false;
            }
        }
        try {
            syncJdbcExecutor.execute("CALL DBMS_OUTPUT.NEW_LINE()");
        } catch (DataAccessException dataAccessException) {
            log.warn("CALL DBMS_OUTPUT.NEW_LINE() occur error {}", dataAccessException.getMessage());
            String result = dataAccessException.getMessage();
            if (result != null && result.contains("Unknown")) {
                return false;
            }
        }
        return true;

    }


    @Override
    public void createDebugPLPackage() {
        String owner = (String) connectionSession.getAttribute(ConnectionSessionConstants.CURRENT_SCHEMA_KEY);
        try {
            if (!PackageValidator.isValid(connectionSession, OdcConstants.PL_DEBUG_PACKAGE, DBObjectType.PACKAGE,
                    owner)) {
                syncJdbcExecutor.execute(OracleCreateDebugPLConstants.WRAPPED_DEBUG_PL_PACKAGE_HEAD);
            }

        } catch (Exception exception) {
            throw new OBException(ErrorCodes.ObCreatePlDebugPackageFailed,
                    new Object[] {exception.getMessage()}, "Valid PL debug package header error",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            ConnectionConfig connectionConfig =
                    (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
            if (!PackageValidator.isValid(connectionSession, OdcConstants.PL_DEBUG_PACKAGE, DBObjectType.PACKAGE_BODY,
                    owner)
                    || !PackageValidator.isVersionValid(connectionSession,
                            OdcConstants.PL_DEBUG_PACKAGE,
                            OracleCreateDebugPLConstants.PL_DEBUG_PACKAGE_VERSION_NOTE,
                            connectionConfig.getUsername())) {
                syncJdbcExecutor.execute(OracleCreateDebugPLConstants.WRAPPED_DEBUG_PL_PACKAGE_BODY);
            }

        } catch (Exception exception) {
            throw new OBException(ErrorCodes.ObCreatePlDebugPackageFailed,
                    new Object[] {exception.getMessage()}, "Valid PL debug package body error",
                    HttpStatus.BAD_REQUEST);
        }

    }

    @Override
    public Object getPLObject(PLIdentity plIdentity) {
        PreConditions.notNull(plIdentity, "PLIdentity");
        PreConditions.notBlank(plIdentity.getPlName(), "PLName");

        String sourceTalbe = "all_source";
        String objectsTable = "all_objects";
        if (PLUtils.isSys(connectionSession)) {
            sourceTalbe = "dba_source";
            objectsTable = "dba_objects";
        }
        String owner = plIdentity.getOwner();
        if (com.oceanbase.odc.common.util.StringUtils.isBlank(owner)) {
            owner = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        }
        String objectName = plIdentity.getPlName();
        DBObjectType referType = plIdentity.getObDbObjectType();

        DBObjectType type;
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("select s.* , o.created, o.last_ddl_time, o.status from (select * from").space()
                .append(objectsTable);
        if (Objects.nonNull(referType)) {
            sqlBuilder.space().append("where object_type=").value(referType.getName());
        }
        sqlBuilder.append(")").space().append("o join").space().append(sourceTalbe).space()
                .append("s on s.name = o.object_name and s.owner = o.owner and s.type = o.object_type ");
        sqlBuilder.append("where s.owner=").value(owner).space().append("and").space().append("s.name=")
                .value(objectName);
        StringBuilder ddlBuilder = new StringBuilder();
        type = syncJdbcExecutor.queryForObject(sqlBuilder.toString(), (resultSet, i) -> {
            String ddlString = resultSet.getString(5);
            ddlBuilder.append(ddlString);
            if (!ddlString.startsWith("create or replace ")) {
                ddlBuilder.insert(0, "create or replace ");
            }
            return DBObjectType.getEnumByName(resultSet.getString(3));
        });
        String ddl = ddlBuilder.toString();


        switch (type) {
            case PACKAGE:
                DBPackage packageHead = new DBPackage();
                packageHead.setPackageName(objectName);
                packageHead.setPackageType(DBObjectType.PACKAGE.getName());
                packageHead.setPackageHead(DBPackageDetail.of(DBPackageBasicInfo.of(ddl)));
                return packageHead;
            case PACKAGE_BODY:
                DBPackage packageBody = new DBPackage();
                packageBody.setPackageName(objectName);
                packageBody.setPackageType(DBObjectType.PACKAGE_BODY.getName());
                packageBody.setPackageBody(DBPackageDetail.of(DBPackageBasicInfo.of(ddl)));
                return packageBody;
            case FUNCTION:
                DBFunction function = new DBFunction();
                function.setFunName(objectName);
                function.setDdl(ddl);
                return function;
            case PROCEDURE:
                DBProcedure procedure = new DBProcedure();
                procedure.setProName(objectName);
                procedure.setDdl(ddl);
                return procedure;
            case TRIGGER:
                DBTrigger trigger = new DBTrigger();
                trigger.setTriggerName(objectName);
                trigger.setDdl(ddl);
                return trigger;
            case TYPE:
                DBType type1 = new DBType();
                type1.setTypeName(objectName);
                type1.setDdl(ddl);
                return type1;
            default:
                throw new UnsupportedException(String.format("%s is not supported to fetch PL Object", type));
        }
    }

}
