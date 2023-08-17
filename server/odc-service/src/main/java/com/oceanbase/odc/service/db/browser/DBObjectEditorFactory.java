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
import com.oceanbase.tools.dbbrowser.editor.DBObjectEditor;

/**
 * @Author: Lebie
 * @Date: 2022/7/19 下午10:22
 * @Description: []
 */
public abstract class DBObjectEditorFactory<T extends DBObjectEditor> {
    protected ConnectType connectType;

    protected String dbVersion;

    public DBObjectEditorFactory(ConnectType connectType, String dbVersion) {
        this.connectType = connectType;
        this.dbVersion = dbVersion;
    }

    public abstract T create();
}
