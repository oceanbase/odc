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
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400ConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleLessThan400ConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleConstraintEditor;

/**
 * @Author: Lebie
 * @Date: 2022/7/19 下午10:37
 * @Description: []
 */
public class DBTableConstraintEditorFactory extends DBObjectEditorFactory<DBTableConstraintEditor> {

    public DBTableConstraintEditorFactory(ConnectType connectType, String dbVersion) {
        super(connectType, dbVersion);
    }

    @Override
    public DBTableConstraintEditor create() {
        switch (connectType) {
            case MYSQL:
                return new MySQLConstraintEditor();
            case OB_MYSQL:
            case CLOUD_OB_MYSQL:
            case ODP_SHARDING_OB_MYSQL:
                if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
                    return new OBMySQLLessThan400ConstraintEditor();
                }
                return new MySQLConstraintEditor();
            case CLOUD_OB_ORACLE:
            case OB_ORACLE:
                if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
                    return new OBOracleLessThan400ConstraintEditor();
                }
                return new OracleConstraintEditor();
            case ORACLE:
                return new OracleConstraintEditor();
            default:
                throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }
}
