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

import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/11/10 下午8:01
 * @Description: [用于查询 PL 对象无效时的详细错误信息]
 */
@Slf4j
public class PLObjectErrMsgUtils {

    public static String getOraclePLObjErrMsg(ConnectionSession session, String owner, String objectType,
            String objectName) throws Exception {
        return acquireErrorMessage(session, owner, objectType, objectName).getOrDefault(objectName, null);
    }

    public static Map<String, String> acquireErrorMessage(ConnectionSession session, String owner, String objectType,
            String objectName) throws Exception {
        Map<String, String> objectName2Text = new HashMap<>();
        if (DialectType.OB_ORACLE != session.getDialectType()) {
            return objectName2Text;
        }
        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(objectType)) {
            log.info("Skip acquire error message, owner={}, objectType={}, objectName={}",
                    owner, objectType, objectName);
            return objectName2Text;
        }
        String sql = generateOracleQuerySql(owner, objectType, objectName);
        session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).query(sql, resultSet -> {
            String name = resultSet.getString(1);
            String text = resultSet.getString(2);
            String lineStr = resultSet.getString(3);
            String positionStr = resultSet.getString(4);

            String errorMessage = generateErrorText(text, lineStr, positionStr);
            if (objectName2Text.containsKey(name)) {
                String originalValue = objectName2Text.get(name);
                objectName2Text.put(name, originalValue + "\n" + errorMessage);
            } else {
                objectName2Text.put(name, errorMessage);
            }
        });
        return objectName2Text;
    }

    private static String generateOracleQuerySql(String owner, String objectType, String objectName) {
        String sql;
        /**
         * all_errors 视图 ob 内部没有内部表没有对应的统计信息，因此直接查询生成的计划不优，导致一些客户很慢。这里通过绑定 outline 固定执行计划来解决这个问题，除非是 ob
         * 的bug，否则不会产生正确性问题，refer details from goc ticket, ticketId 2022091600000002799
         */
        if (DBObjectType.PACKAGE.name().equalsIgnoreCase(objectType)) {
            sql = String.format("select /*+leading(O)*/ name, text, line, position from all_errors "
                    + "where owner = %s and (type = 'PACKAGE' or type = 'PACKAGE BODY')",
                    StringUtils.quoteOracleValue(owner));
        } else {
            sql = String.format("select /*+leading(O)*/ name, text, line, position from "
                    + "all_errors where owner = %s and type = %s", StringUtils.quoteOracleValue(owner),
                    StringUtils.quoteOracleValue(objectType));
        }
        if (StringUtils.isNotBlank(objectName)) {
            // object name is optional
            sql = String.format(sql + " and name = %s", StringUtils.quoteOracleValue(objectName));
        }
        return sql;
    }

    private static String generateErrorText(String text, String lineStr, String positionStr) {
        if (StringUtils.isBlank(lineStr) || StringUtils.isBlank(positionStr)) {
            return text;
        }
        return String.format(text + " at line %s, position %s", lineStr, positionStr);
    }
}

