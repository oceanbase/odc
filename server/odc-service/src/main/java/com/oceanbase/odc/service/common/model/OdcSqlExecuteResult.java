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
package com.oceanbase.odc.service.common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.oceanbase.tools.dbbrowser.parser.constant.GeneralSqlType;

import lombok.Data;

/**
 * @auther kuiseng.zhb
 */
@Deprecated
@Data
public class OdcSqlExecuteResult {

    private String messages;
    private String statementWarnings;
    private List<String> columns;
    private List<String> columnLabels;
    private Map<String, Object> types;
    private Map<String, String> typeNames;
    private Integer total;
    private List<List<Object>> rows = new ArrayList<>();
    private float elapsedTime;
    private String executeSql;
    private String originSql;
    private String track;
    private boolean status = true;
    private String sqlType;
    private String dbObjectType;
    private String dbObjectName;
    private List<String> dbObjectNameList;
    private String traceId;
    private String dbmsOutput;

    // time cost info
    private long executeTimestamp; // timestamp to process query in UTC timezone
    private long queryCostMillis; // ob query time cost
    private long odcProcessCostMillis = 0L;

    private GeneralSqlType generalSqlType;
    private OdcResultSetMetaData odcResultSetMetaData;

    public void setDbObjectName(String name) {
        this.dbObjectNameList = Arrays.asList(name);
        this.dbObjectName = name;
    }

    public static OdcSqlExecuteResult empty(String sql) {
        OdcSqlExecuteResult result = new OdcSqlExecuteResult();
        result.setQueryCostMillis(0);
        result.setOdcProcessCostMillis(0);
        result.setStatus(true);
        result.setExecuteTimestamp(System.currentTimeMillis());
        result.setTotal(0);
        result.setExecuteSql(sql);
        result.setOriginSql(sql);
        result.setTrack("<empty statement>");
        result.setStatementWarnings("<empty statement>");
        return result;
    }

    public static OdcSqlExecuteResult success(String sql) {
        OdcSqlExecuteResult result = empty(sql);
        result.setTrack(null);
        result.setStatementWarnings(null);
        return result;
    }
}
