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
package com.oceanbase.odc.service.sqlcheck.rule.checkRationality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.common.BraceBlock;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/6/18 19:52
 * @since: 4.3.4
 */
public class DBTableCheckRationalityChecker implements DBObjectExistChecker, DBObjectExtractor {

    @Override
    public Boolean checkObjectExistence(DBObjectIdentity dbObjectIdentity, JdbcOperations jdbcOperations) {
        return jdbcOperations
                .execute((ConnectionCallback<Boolean>) con -> SchemaPluginUtil.getTableExtension(DialectType.OB_MYSQL)
                        .list(con,
                                dbObjectIdentity.getSchemaName(), DBObjectType.TABLE)
                        .stream().anyMatch(
                                table -> table.getName().equals(dbObjectIdentity.getName())));
    }

    @Override
    public Boolean checkObjectNonExistence(DBObjectIdentity dbObjectIdentity, JdbcOperations jdbcOperations) {
        return true;
    }

    @Override
    public List<DBObjectIdentity> ExtractShouldExistedDBObjects(Supplier<String> schemaSupplier, Statement statement) {
        List<DBObjectIdentity> shouldExistedTables = new ArrayList<>();
        if (statement.getClass() == Select.class) {
            // todo 这里校验表对象存在的合理性
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            List<FromReference> froms = selectBody.getFroms();
            // todo 获取需要校验存在的表对象
            if (CollectionUtils.isNotEmpty(froms)) {
                for (FromReference from : froms) {
                    if (null != from) {
                        if (from.getClass() == NameReference.class) {
                            NameReference nameReference = (NameReference) from;
                            if (null != nameReference.getSchema()) {
                                shouldExistedTables
                                        .add(DBObjectIdentity.of(nameReference.getSchema(), DBObjectType.TABLE,
                                                StringUtils.unquoteMySqlIdentifier(nameReference.getRelation())));
                            } else {
                                shouldExistedTables.add(DBObjectIdentity.of(schemaSupplier.get(), DBObjectType.TABLE,
                                        StringUtils.unquoteMySqlIdentifier(nameReference.getRelation())));
                            }
                        } else if (from.getClass() == JoinReference.class) {

                        } else if (from.getClass() == BraceBlock.class) {

                        } else if (from.getClass() == ExpressionReference.class) {

                        }
                    }
                }
            }
        } else if (statement.getClass() == CreateTable.class) {

        }
        return shouldExistedTables;
    }

    @Override
    public List<DBObjectIdentity> ExtractShouldNotExistedDBObjects(Supplier<String> schemaSupplier,
            Statement statement) {
        return Collections.emptyList();
    }

}
