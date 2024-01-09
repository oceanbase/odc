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
package com.oceanbase.odc.plugin.schema.api;

import java.sql.Connection;

import org.pf4j.ExtensionPoint;

import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

/**
 * @author jingtian
 * @date 2024/1/5
 * @since ODC_release_4.2.4
 */
public interface SchemaBrowserExtensionPoint extends ExtensionPoint {
    DBSchemaAccessor getDBSchemaAccessor(Connection connection);

    DBTableEditor getDBTableEditor(Connection connection);
}
