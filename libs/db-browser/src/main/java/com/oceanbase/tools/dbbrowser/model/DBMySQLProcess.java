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
package com.oceanbase.tools.dbbrowser.model;

import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.Data;

/**
 * Entity mapping to mysql information_schema.processlist
 */
@Data
public class DBMySQLProcess {
    /**
     * 数据库内部会话 ID, null if session not exists
     */
    private String id;
    /**
     * Session 用户名
     */
    private String user;
    /**
     * Session 默认数据库名称
     */
    private String db;
    /**
     * Session 正在执行的 command
     */
    private String command;
    /**
     * Session 状态, An action, event, or state that indicates what the thread is doing.
     */
    private String state;
    /**
     * 当前 Session 最后执行的语句
     */
    private String info;
    /**
     * Session 在当前状态的持续时间
     */
    private String time;
    /**
     * 客户端主机名称
     */
    private String host;

    /**
     * observer ip, refer to the column `Ip` in the result set of `SHOW FULL PROCESSLIST`
     */
    private String ip;

    /**
     * observer SQL port, refer to the column `Port` in the result set of `SHOW FULL PROCESSLIST`
     */
    private String port;

    public DBSession toDBSession() {
        DBSession session = new DBSession();
        session.setId(id);
        session.setUsername(user);
        session.setDatabaseName(db);
        session.setCommand(command);
        session.setState(state);
        session.setLatestQueries(info);
        session.setHost(host);
        session.setProxyHost(host);
        session.setExecuteTime(Integer.parseInt(time));
        session.setSvrIp(StringUtils.join(ip, ":", port));
        return session;
    }
}

