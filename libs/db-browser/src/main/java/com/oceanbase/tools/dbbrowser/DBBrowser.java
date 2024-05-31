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
package com.oceanbase.tools.dbbrowser;

import com.oceanbase.tools.dbbrowser.editor.DBObjectEditorFactories;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorFactory;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessorFactory;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplateFactories;

public class DBBrowser {

    public static DBSchemaAccessorFactory schemaAccessor() {
        return new DBSchemaAccessorFactory();
    }

    public static DBObjectEditorFactories objectEditor() {
        return new DBObjectEditorFactories();
    }

    public static DBObjectTemplateFactories objectTemplate() {
        return new DBObjectTemplateFactories();
    }

    public static DBStatsAccessorFactory statsAccessor() {
        return new DBStatsAccessorFactory();
    }

}
