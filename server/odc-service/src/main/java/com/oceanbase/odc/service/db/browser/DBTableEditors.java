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

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;

public class DBTableEditors {

    public static DBTableEditor create(ConnectType connectType, String dbVersion) {
        return DBBrowser.objectEditor().tableEditor()
                .setDbVersion(dbVersion)
                .setType(connectType.getDialectType().name())
                .create();
    }

    public static DBTableEditor create(ConnectionSession connectionSession) {
        return create(connectionSession.getConnectType(), ConnectionSessionUtil.getVersion(connectionSession));
    }

}
