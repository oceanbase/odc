/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.tools.dbbrowser.template.mysql;

import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditorFactory;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.template.BaseMViewTemplate;
import com.oceanbase.tools.dbbrowser.template.BaseViewTemplate;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import org.apache.commons.lang3.Validate;

import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 16:45
 * @since: 4.3.4
 */
public class MysqlMViewTemplate implements DBObjectTemplate<DBMView> {
   private BaseViewTemplate mySQLViewTemplate;
   private DBTableEditor dbTableEditor;

   public  MysqlMViewTemplate() {
        mySQLViewTemplate = new MySQLViewTemplate();
        DBTableEditorFactory dbTableEditorFactory = new DBTableEditorFactory();
        dbTableEditorFactory.setDbVersion("4.3.3");
        dbTableEditor = dbTableEditorFactory.buildForOBMySQL();
    }

    @Override
    public String generateCreateObjectTemplate(DBMView dbObject) {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("create materialized view ")
            .identifier(dbObject.getMVName());
        String ddl = dbTableEditor.generateCreateObjectDDL(dbObject.generateDBTable());
//        dbTableEditor.getColumnEditor().generateCreateDefinitionDDL(dbObject.getCreateColumns());
        String partitionDDL = dbTableEditor.getPartitionEditor().generateCreateDefinitionDDL(dbObject.getPartition());
        mySQLViewTemplate.generateQueryStatement(dbObject.generateDBView(), sqlBuilder);
        return sqlBuilder.toString();
    }

}
