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
package com.oceanbase.odc.service.sqlcheck.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.checkRationality.DBColumnCheckRationalityChecker;
import com.oceanbase.odc.service.sqlcheck.rule.checkRationality.DBObjectCheckRationalityContext;
import com.oceanbase.odc.service.sqlcheck.rule.checkRationality.DBTableCheckRationalityChecker;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/6/12 19:25
 * @since: 4.3.4
 */
public class MySQLCheckRationalityForDBObjects implements SqlCheckRule {

    private final Supplier<String> schemaSupplier;

    private final JdbcOperations jdbcOperations;
    // 前端勾选的数据库对象类型，勾选后就做此数据库对象类型的存在合理性校验
    // 目前写死做测试用，后续需要从前端动态赋值
    List<DBObjectType> supportedDBObjectType = Arrays.asList(DBObjectType.TABLE, DBObjectType.COLUMN);

    public MySQLCheckRationalityForDBObjects(Supplier<String> schemaSupplier, JdbcOperations jdbcOperations) {
        this.schemaSupplier = schemaSupplier;
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.CHECK_RATIONALITY_FOR_DB_OBJECTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        // todo 获取需要校验存在的表对象
        List<CheckViolation> checkViolationlist = new ArrayList<>();
        DBObjectCheckRationalityContext dbObjectCheckRationalityContext = context.getDbObjectCheckRationalityContext();
        // todo 这里校验表对象存在的合理性
        if (supportedDBObjectType.contains(DBObjectType.TABLE)) {
            DBTableCheckRationalityChecker dbTableCheckRationalityChecker = new DBTableCheckRationalityChecker();
            // todo 获取需要校验存在的表对象
            List<DBObjectIdentity> shouldExistedTable =
                    dbTableCheckRationalityChecker.ExtractShouldExistedDBObjects(schemaSupplier, statement);
            // todo 校验表对象是否存在
            for (DBObjectIdentity dbObjectIdentity : shouldExistedTable) {
                Boolean existed = dbTableCheckRationalityChecker.checkObjectExistence(dbObjectIdentity, jdbcOperations);
                if (Boolean.FALSE.equals(existed)) {
                    checkViolationlist.add(
                            SqlCheckUtil.buildViolation(statement.getText(), statement, getType(),
                                    new Object[] {}));
                }
            }
            // todo 获取需要检验不存在的表对象
            List<DBObjectIdentity> shouldNotExistedTables =
                    dbTableCheckRationalityChecker.ExtractShouldExistedDBObjects(schemaSupplier, statement);
            // todo 执行校验表对象不存在逻辑
            for (DBObjectIdentity shouldNotExistedTable : shouldNotExistedTables) {
                Boolean existed =
                        dbTableCheckRationalityChecker.checkObjectNonExistence(shouldNotExistedTable, jdbcOperations);
                if (Boolean.FALSE.equals(existed)) {
                    checkViolationlist.add(
                            SqlCheckUtil.buildViolation(statement.getText(), statement, getType(),
                                    new Object[] {}));
                }
            }
        }
        // todo 这里校验列对象存在的合理性
        else if (supportedDBObjectType.contains(DBObjectType.COLUMN)) {
            DBColumnCheckRationalityChecker dbColumnCheckRationalityChecker = new DBColumnCheckRationalityChecker();
            // todo 获取需要校验存在的表对象
            List<DBObjectIdentity> shouldExistedColumn =
                    dbColumnCheckRationalityChecker.ExtractShouldExistedDBObjects(schemaSupplier, statement);
            // todo 校验表对象是否存在
            for (DBObjectIdentity dbObjectIdentity : shouldExistedColumn) {
                Boolean existed =
                        dbColumnCheckRationalityChecker.checkObjectExistence(dbObjectIdentity, jdbcOperations);
                if (Boolean.FALSE.equals(existed)) {
                    checkViolationlist.add(
                            SqlCheckUtil.buildViolation(statement.getText(), statement, getType(),
                                    new Object[] {}));
                }
            }
            // todo 获取需要检验不存在的表对象
            List<DBObjectIdentity> shouldNotExistedColumn =
                    dbColumnCheckRationalityChecker.ExtractShouldExistedDBObjects(schemaSupplier, statement);
            // todo 执行校验表对象不存在逻辑
            for (DBObjectIdentity shouldNotExistedTable : shouldNotExistedColumn) {
                Boolean existed =
                        dbColumnCheckRationalityChecker.checkObjectNonExistence(shouldNotExistedTable, jdbcOperations);
                if (Boolean.FALSE.equals(existed)) {
                    checkViolationlist.add(
                            SqlCheckUtil.buildViolation(statement.getText(), statement, getType(),
                                    new Object[] {}));
                }
            }
        } else if (supportedDBObjectType.contains(DBObjectType.INDEX)) {
            // todo 这里校验索引对象存在的合理性
        }
        return checkViolationlist;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL);
    }
}
