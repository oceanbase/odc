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

import lombok.Data;

/**
 * 数据库会话
 */
@Data
public class DBSession {
    /**
     * 数据库内部会话 ID, null if session not exists
     */
    private String id;
    /**
     * Session 用户名
     */
    private String username;
    /**
     * Session 默认数据库名称
     */
    private String databaseName;
    /**
     * Session 正在执行的 command
     */
    private String command;
    /**
     * Session 状态, An action, event, or state that indicates what the thread is doing.
     */
    private String state;
    /**
     * 事务状态
     */
    private DBTransState transState;
    /**
     * 事务 ID
     */
    private String transId;
    /**
     * SQL ID
     */
    private String sqlId;
    /**
     * Trace ID
     */
    private String traceId;
    /**
     * 当前正在执行的语句
     */
    private String activeQueries;
    /**
     * 当前 Session 最后执行的语句
     */
    private String latestQueries;
    /**
     * 客户端主机名称
     */
    private String host;
    /**
     * OB Proxy HOST
     */
    private String proxyHost;
    /**
     * 当前命令执行时间
     */
    private Integer executeTime;


    public static DBSession unknown() {
        DBSession session = new DBSession();
        session.setTransState(DBTransState.UNKNOWN);
        return session;
    }

    public enum DBTransState {
        IDLE,
        ACTIVE,
        TIMEOUT,
        UNKNOWN
    }
}
