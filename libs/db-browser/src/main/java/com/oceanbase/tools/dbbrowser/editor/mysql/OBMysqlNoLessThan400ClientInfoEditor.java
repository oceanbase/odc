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
package com.oceanbase.tools.dbbrowser.editor.mysql;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.editor.DBClientInfoEditor;
import com.oceanbase.tools.dbbrowser.model.DbClientInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OBMysqlNoLessThan400ClientInfoEditor implements DBClientInfoEditor {

    private final String SET_CLIENT_INFO_SQL =
            "call dbms_application_info.SET_MODULE(module_name => '%s', action_name => '%s');call dbms_application_info.set_client_info('%s'); ";
    private final JdbcOperations jdbcOperations;

    public OBMysqlNoLessThan400ClientInfoEditor(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public boolean setClientInfo(DbClientInfo clientInfo) {
        try {
            String sql = String.format(SET_CLIENT_INFO_SQL, clientInfo.getModule(), clientInfo.getAction(),
                    clientInfo.getContext());
            jdbcOperations.execute(sql);
            return true;
        } catch (Exception e) {
            log.info("set OBMysql clientInfo failed", e);
            return false;
        }
    }
}
