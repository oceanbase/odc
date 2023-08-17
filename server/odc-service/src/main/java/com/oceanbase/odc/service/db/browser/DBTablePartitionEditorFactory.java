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
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan2277PartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleDBTablePartitionEditor;

/**
 * @Author: Lebie
 * @Date: 2022/8/17 下午11:38
 * @Description: []
 */
public class DBTablePartitionEditorFactory extends DBObjectEditorFactory<DBTablePartitionEditor> {

    public DBTablePartitionEditorFactory(ConnectType connectType, String dbVersion) {
        super(connectType, dbVersion);
    }

    @Override
    public DBTablePartitionEditor create() {
        PreConditions.notNull(connectType, "connectType");
        if (connectType == ConnectType.OB_MYSQL || connectType == ConnectType.CLOUD_OB_MYSQL) {
            if (VersionUtils.isLessThan(dbVersion, "2.2.77")) {
                return new OBMySQLLessThan2277PartitionEditor();
            } else {
                return new MySQLDBTablePartitionEditor();
            }
        } else if (connectType == ConnectType.MYSQL || connectType == ConnectType.ODP_SHARDING_OB_MYSQL) {
            return new MySQLDBTablePartitionEditor();
        } else if (connectType == ConnectType.ORACLE || connectType == ConnectType.OB_ORACLE
                || connectType == ConnectType.CLOUD_OB_ORACLE) {
            return new OracleDBTablePartitionEditor();
        } else {
            throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }
}
