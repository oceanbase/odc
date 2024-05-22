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

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.generator.DBTablePartitionEditorGenerator;

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
            return new DBTablePartitionEditorGenerator().createForOBMySQL(dbVersion);
        } else if (connectType == ConnectType.MYSQL) {
            return new DBTablePartitionEditorGenerator().createForMySQL(dbVersion);
        } else if (connectType == ConnectType.ODP_SHARDING_OB_MYSQL) {
            return new DBTablePartitionEditorGenerator().createForODPOBMySQL(dbVersion);
        } else if (connectType == ConnectType.OB_ORACLE || connectType == ConnectType.CLOUD_OB_ORACLE) {
            return new DBTablePartitionEditorGenerator().createForOBOracle(dbVersion);
        } else if (connectType == ConnectType.ORACLE) {
            return new DBTablePartitionEditorGenerator().createForOracle(dbVersion);
        } else if (connectType == ConnectType.DORIS) {
            return new DBTablePartitionEditorGenerator().createForDoris(dbVersion);
        } else {
            throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }
}
