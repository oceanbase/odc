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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.model.OdcDBVariable;
import com.oceanbase.odc.service.session.interceptor.NlsFormatInterceptor;
import com.oceanbase.tools.dbbrowser.model.DBVariable;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@SkipAuthorize("inside connect session")
public class DBVariablesService {

    // 重要的数据库变量，在展示时会优先展示
    private static final List<String> IMPORTANT_DB_VARIABLES = new ArrayList<>();
    // value为枚举的数据库变量，定义value枚举，方便用户直接选择
    private static final Map<String, List<String>> DB_VARIABLE_CANDIDATE_VALUE = new HashMap<>();
    // 时间类型数据库变量的单位
    private static final Map<String, String> DB_VARIABLE_VALUE_UNIT = new HashMap<>();

    static {
        IMPORTANT_DB_VARIABLES.add("autocommit");
        IMPORTANT_DB_VARIABLES.add("connect_timeout");
        IMPORTANT_DB_VARIABLES.add("interactive_timeout");
        IMPORTANT_DB_VARIABLES.add("last_insert_id");
        IMPORTANT_DB_VARIABLES.add("max_allowed_packet");
        IMPORTANT_DB_VARIABLES.add("ob_compatibility_mode");
        IMPORTANT_DB_VARIABLES.add("ob_max_parallel_degree");
        IMPORTANT_DB_VARIABLES.add("ob_query_timeout");
        IMPORTANT_DB_VARIABLES.add("ob_read_consistency");
        IMPORTANT_DB_VARIABLES.add("ob_route_policy");
        IMPORTANT_DB_VARIABLES.add("ob_trx_timeout");
        IMPORTANT_DB_VARIABLES.add("tx_isolation");

        // mysql mode、oracle mode 目前是一致的
        DB_VARIABLE_CANDIDATE_VALUE.put("autocommit", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_read_consistency", Arrays.asList("STRONG", "WEAK"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_route_policy",
                Arrays.asList("STRONG_CONSISTENCY", "READONLY_ZONE_FIRST", "UNMERGE_ZONE_FIRST", "NON_RW_SEPARATION"));
        DB_VARIABLE_CANDIDATE_VALUE.put("tx_isolation",
                Arrays.asList("READ-UNCOMMITTED", "READ-COMMITTED", "REPEATABLE-READ", "SERIALIZABLE"));
        DB_VARIABLE_CANDIDATE_VALUE.put("recyclebin", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_create_table_strict_mode", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_early_lock_release", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_aggregation_pushdown", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_blk_nestedloop_join", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_hash_group_by", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_index_direct_select", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_jit", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_plan_cache", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_sql_audit", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_trace_log", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_transformation", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_transmission_checksum", Arrays.asList("ON", "OFF"));
        DB_VARIABLE_CANDIDATE_VALUE.put("ob_enable_truncate_flashback", Arrays.asList("ON", "OFF"));

        DB_VARIABLE_VALUE_UNIT.put("connect_timeout", "s");
        DB_VARIABLE_VALUE_UNIT.put("interactive_timeout", "s");
        DB_VARIABLE_VALUE_UNIT.put("ob_query_timeout", "us");
        DB_VARIABLE_VALUE_UNIT.put("ob_trx_timeout", "us");
        DB_VARIABLE_VALUE_UNIT.put("net_read_timeout", "s");
        DB_VARIABLE_VALUE_UNIT.put("net_write_timeout", "s");
        DB_VARIABLE_VALUE_UNIT.put("wait_timeout", "s");
        DB_VARIABLE_VALUE_UNIT.put("ob_trx_idle_timeout", "us");
    }

    public List<OdcDBVariable> list(ConnectionSession connectionSession, String variableScope) {
        List<DBVariable> dbVariables;
        DBSchemaAccessor accessor =
                DBSchemaAccessors.create(connectionSession, ConnectionSessionConstants.CONSOLE_DS_KEY);
        if ("session".equals(variableScope)) {
            dbVariables = accessor.showSessionVariables();
        } else if ("global".equals(variableScope)) {
            dbVariables = accessor.showGlobalVariables();
        } else {
            dbVariables = accessor.showVariables();
        }
        List<OdcDBVariable> resultDbVirableList = new ArrayList<>();
        List<OdcDBVariable> tempResultDbVirableList = new ArrayList<>();
        dbVariables.forEach(var -> {
            OdcDBVariable variableResponse = new OdcDBVariable();
            variableResponse.setName(var.getName());
            variableResponse.setValue(var.getValue());
            String key = var.getName().toLowerCase();
            // 设置变量类型
            if (DB_VARIABLE_CANDIDATE_VALUE.containsKey(key)) {
                variableResponse.setValueType(OdcConstants.DB_VARIABLE_TYPE_ENUM);
                variableResponse.setValueEnums(DB_VARIABLE_CANDIDATE_VALUE.get(key));
            } else if (DataTypeUtil.isNumericValue(var.getValue())) {
                variableResponse.setValueType(OdcConstants.DB_VARIABLE_TYPE_NUMERIC);
                if (DB_VARIABLE_VALUE_UNIT.containsKey(key)) {
                    variableResponse.setUnit(DB_VARIABLE_VALUE_UNIT.get(key));
                }
            } else {
                variableResponse.setValueType(OdcConstants.DB_VARIABLE_TYPE_STRING);
            }
            // 常用的变量放在前面展示
            if (IMPORTANT_DB_VARIABLES.contains(key)) {
                resultDbVirableList.add(variableResponse);
            } else {
                tempResultDbVirableList.add(variableResponse);
            }
        });
        resultDbVirableList.addAll(tempResultDbVirableList);
        return resultDbVirableList;
    }

    public boolean update(@NonNull ConnectionSession session, @NonNull OdcDBVariable resource) {
        String dml = getUpdateDml(resource.getVariableScope(), resource);
        if (StringUtils.isEmpty(dml)) {
            return false;
        }
        session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(dml);
        NlsFormatInterceptor.setNlsFormat(session, SqlTuple.newTuple(dml));
        return true;
    }

    private String getUpdateDml(@NonNull String variableScope, @NonNull OdcDBVariable resource) {
        String value = resource.getValue();
        if (!DataTypeUtil.isNumericValue(value)) {
            value = "'" + value + "'";
        }
        return String.format("set %s %s=%s", variableScope, resource.getKey(), value);
    }

}
