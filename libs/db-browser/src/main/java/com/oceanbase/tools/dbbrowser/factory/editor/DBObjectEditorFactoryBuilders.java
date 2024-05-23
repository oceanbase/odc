/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.tools.dbbrowser.factory.editor;

import com.oceanbase.tools.dbbrowser.factory.editor.index.DBTableIndexEditorFactoryBuilders;
import com.oceanbase.tools.dbbrowser.factory.editor.table.DBTableEditorFactoryBuilders;

public class DBObjectEditorFactoryBuilders {

    public DBTableEditorFactoryBuilders table() {
        return new DBTableEditorFactoryBuilders();
    }

    public DBTableIndexEditorFactoryBuilders tableIndex() {
        return new DBTableIndexEditorFactoryBuilders();
    }

    public <Builders> Builders tableColumn() {
        return null;
    }

    public <Builders> Builders tableConstraint() {
        return null;
    }

    public <Builders> Builders tablePartition() {
        return null;
    }

    public <Builders> Builders objectOperator() {
        return null;
    }

}
