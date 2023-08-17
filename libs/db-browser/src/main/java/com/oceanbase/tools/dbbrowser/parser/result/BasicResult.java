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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.listener.BasicParserListener;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2021/9/9
 */

@Data
public class BasicResult {

    private SqlType sqlType;
    private DBObjectType dbObjectType;
    private List<String> dbObjectNameList;
    private Boolean syntaxError;

    public BasicResult(BasicParserListener listener) {
        this.sqlType = listener.getSqlType();
        this.dbObjectType = listener.getDbObjectType();
        this.dbObjectNameList = listener.getDbObjectNameList();
    }

    public BasicResult(SqlType sqlType) {
        this.sqlType = sqlType;
    }

    public boolean isPlDdl() {
        if (this.sqlType != SqlType.CREATE) {
            return false;
        }
        Set<DBObjectType> plTypes = new HashSet<>();
        plTypes.add(DBObjectType.PROCEDURE);
        plTypes.add(DBObjectType.FUNCTION);
        plTypes.add(DBObjectType.PACKAGE);
        plTypes.add(DBObjectType.PACKAGE_BODY);
        plTypes.add(DBObjectType.TRIGGER);
        plTypes.add(DBObjectType.TYPE);
        plTypes.add(DBObjectType.ANONYMOUS_BLOCK);
        return plTypes.contains(this.dbObjectType);
    }

}
