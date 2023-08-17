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
package com.oceanbase.tools.dbbrowser.editor.oracle;

import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @Author: Lebie
 * @Date: 2022/7/21 下午10:56
 * @Description: []
 */
public class OBOracleIndexEditor extends OracleIndexEditor {

    @Override
    protected void appendIndexType(DBTableIndex index, SqlBuilder sqlBuilder) {
        DBIndexAlgorithm algorithm = index.getAlgorithm();
        if (algorithm == DBIndexAlgorithm.BTREE) {
            sqlBuilder.append(" USING BTREE");
        } else if (algorithm == DBIndexAlgorithm.HASH) {
            sqlBuilder.append(" USING HASH");
        } else {
            super.appendIndexType(index, sqlBuilder);
        }
    }

}
