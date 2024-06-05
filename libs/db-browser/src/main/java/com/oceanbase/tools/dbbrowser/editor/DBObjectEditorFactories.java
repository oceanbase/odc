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
package com.oceanbase.tools.dbbrowser.editor;

public class DBObjectEditorFactories {

    public DBTableEditorFactory tableEditor() {
        return new DBTableEditorFactory();
    }

    public DBTableIndexEditorFactory tableIndexEditor() {
        return new DBTableIndexEditorFactory();
    }

    public DBTableColumnEditorFactory tableColumnEditor() {
        return new DBTableColumnEditorFactory();
    }

    public DBTableConstraintEditorFactory tableConstraintEditor() {
        return new DBTableConstraintEditorFactory();
    }

    public DBTablePartitionEditorFactory tablePartitionEditor() {
        return new DBTablePartitionEditorFactory();
    }

    public DBSynonymEditorFactory synonymEditor() {
        return new DBSynonymEditorFactory();
    }

    public DBSequenceEditorFactory sequenceEditor() {
        return new DBSequenceEditorFactory();
    }

    public DBObjectOperatorFactory objectOperator() {
        return new DBObjectOperatorFactory();
    }

}
