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
package com.oceanbase.tools.dbbrowser.util;

/**
 * @author jingtian
 */
public class MySQLSqlBuilder extends SqlBuilder {
    private final StringBuilder sb;

    public MySQLSqlBuilder() {
        this.sb = new StringBuilder();
    }

    @Override
    public SqlBuilder identifier(String identifier) {
        return append(StringUtils.quoteMysqlIdentifier(identifier));
    }

    @Override
    public SqlBuilder value(String value) {
        return append(StringUtils.quoteMysqlValue(value));
    }

    @Override
    public SqlBuilder defaultValue(String value) {
        return append(SqlUtils.quoteMysqlDefaultValue(value));
    }
}
