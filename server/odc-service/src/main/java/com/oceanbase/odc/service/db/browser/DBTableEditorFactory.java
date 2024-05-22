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
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.generator.DBTableEditorGenerator;

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
        switch (connectType) {
            case OB_MYSQL:
            case CLOUD_OB_MYSQL:
                return new DBTableEditorGenerator().createForOBMySQL(dbVersion);
            case ODP_SHARDING_OB_MYSQL:
                return new DBTableEditorGenerator().createForODPOBMySQL(dbVersion);
            case DORIS:
                return new DBTableEditorGenerator().createForDoris(dbVersion);
            case MYSQL:
                return new DBTableEditorGenerator().createForMySQL(dbVersion);
            case CLOUD_OB_ORACLE:
            case OB_ORACLE:
                return new DBTableEditorGenerator().createForOBOracle(dbVersion);
            case ORACLE:
                return new DBTableEditorGenerator().createForOracle(dbVersion);
            default:
                throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }

}
