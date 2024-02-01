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

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleColumnEditor;

/**
 * @Author: Lebie
 * @Date: 2022/7/19 下午10:36
 * @Description: []
 */
public class DBTableColumnEditorFactory extends DBObjectEditorFactory<DBTableColumnEditor> {

    public DBTableColumnEditorFactory(ConnectType connectType, String dbVersion) {
        super(connectType, dbVersion);
    }

    @Override
    public DBTableColumnEditor create() {
        switch (connectType) {
            case MYSQL:
            case DORIS:
            case OB_MYSQL:
            case CLOUD_OB_MYSQL:
            case ODP_SHARDING_OB_MYSQL:
                return new MySQLColumnEditor();
            case CLOUD_OB_ORACLE:
            case OB_ORACLE:
            case ORACLE:
                return new OracleColumnEditor();
            default:
                throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }
}
