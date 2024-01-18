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
package com.oceanbase.odc.service.db.browser;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLTableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400TableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLTableEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleTableEditor;

/**
 * @Author: Lebie
 * @Date: 2022/7/20 上午12:45
 * @Description: []
 */
public class DBTableEditorFactory extends DBObjectEditorFactory<DBTableEditor> {
    public DBTableEditorFactory(ConnectType connectType, String dbVersion) {
        super(connectType, dbVersion);
    }

    @Override
    public DBTableEditor create() {
        PreConditions.notNull(connectType, "connectType");
        DBObjectEditorFactory<DBTableColumnEditor> columnEditorFactory =
                new DBTableColumnEditorFactory(connectType,
                        dbVersion);
        DBObjectEditorFactory<DBTableIndexEditor> indexEditorFactory =
                new DBTableIndexEditorFactory(connectType,
                        dbVersion);
        DBObjectEditorFactory<DBTableConstraintEditor> constraintEditorFactory =
                new DBTableConstraintEditorFactory(connectType,
                        dbVersion);
        DBObjectEditorFactory<DBTablePartitionEditor> partitionEditorFactory =
                new DBTablePartitionEditorFactory(connectType, dbVersion);
        switch (connectType) {
            case OB_MYSQL:
            case CLOUD_OB_MYSQL:
            case ODP_SHARDING_OB_MYSQL:
                if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
                    return new OBMySQLLessThan400TableEditor(indexEditorFactory.create(), columnEditorFactory.create(),
                            constraintEditorFactory.create(), partitionEditorFactory.create());
                }
                return new OBMySQLTableEditor(indexEditorFactory.create(), columnEditorFactory.create(),
                        constraintEditorFactory.create(), partitionEditorFactory.create());
            case MYSQL:
                return new MySQLTableEditor(indexEditorFactory.create(), columnEditorFactory.create(),
                        constraintEditorFactory.create(), partitionEditorFactory.create());
            case CLOUD_OB_ORACLE:
            case OB_ORACLE:
            case ORACLE:
                return new OracleTableEditor(indexEditorFactory.create(), columnEditorFactory.create(),
                        constraintEditorFactory.create(), partitionEditorFactory.create());
            default:
                throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }

}
