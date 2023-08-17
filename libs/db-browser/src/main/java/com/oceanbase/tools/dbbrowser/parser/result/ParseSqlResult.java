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
package com.oceanbase.tools.dbbrowser.parser.result;

import java.util.List;

import com.oceanbase.tools.dbbrowser.model.DBIndex;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.parser.listener.MysqlModeSqlParserListener;
import com.oceanbase.tools.dbbrowser.parser.listener.MysqlModeSqlParserListener.ColumnDefinition;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModeSqlParserListener;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wenniu.ly
 * @date 2021/8/25
 */

@Getter
@Setter
public class ParseSqlResult extends BasicResult {

    private boolean selectStmt;
    private boolean withForUpdate;
    // for mysql
    private boolean limitClause;
    private List<ColumnDefinition> columns;
    // for oracle
    private boolean fetchClause;
    private boolean whereClause;
    private List<DBIndex> indexes;
    private List<DBTableConstraint> foreignConstraint;

    public ParseSqlResult(OracleModeSqlParserListener listener) {
        super(listener);
        this.selectStmt = listener.isSelectStmt();
        this.withForUpdate = listener.isWithForUpdate();
        this.fetchClause = listener.isFetchNext();
        this.whereClause = listener.isWhereClause();
        this.indexes = listener.getIndexes();
        this.foreignConstraint = listener.getForeignConstraint();
    }

    public ParseSqlResult(MysqlModeSqlParserListener listener) {
        super(listener);
        this.selectStmt = listener.isSelectStmt();
        this.withForUpdate = listener.isWithForUpdate();
        this.limitClause = listener.isLimitClause();
        this.indexes = listener.getIndexes();
        this.columns = listener.getColumns();
        this.foreignConstraint = listener.getForeignConstraint();
    }

    public boolean isSupportLimit() {
        return this.selectStmt && !this.withForUpdate;
    }

    public boolean isSupportAddROWID() {
        return this.selectStmt;
    }
}
