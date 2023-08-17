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
package com.oceanbase.odc.service.onlineschemachange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.foreignkey.ForeignKeyHandler;
import com.oceanbase.odc.service.onlineschemachange.foreignkey.OBOracleForeignKeyHandler;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

/**
 * @author yaobin
 * @date 2023-08-07
 * @since 4.2.0
 */
public class OBOracleForeignKeyHandlerTest {
    private static SyncJdbcExecutor jdbcTemplate;
    private static ConnectionSession connectionSession;
    private static ConnectionConfig connectionConfig;
    private static String countryNameOrigin;
    private static String countryNameOld;
    private static String countryNameNew;
    private static String personTableName;
    private static ForeignKeyHandler foreignKeyHandler;

    @BeforeClass
    public static void init() {
        connectionConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_ORACLE);
        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        jdbcTemplate = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        countryNameOrigin = "COUNTRY";
        countryNameOld = "COUNTRY_OLD";
        countryNameNew = "COUNTRY_NEW";
        personTableName = "PERSON";
        createTableBefore();
        foreignKeyHandler = new OBOracleForeignKeyHandler(connectionSession);
    }

    public static void createTableBefore() {
        String createCountry = "CREATE TABLE country (\n"
                + "  id NUMBER NOT NULL,\n"
                + "  name VARCHAR2(255) NOT NULL,\n"
                + "  CONSTRAINT country_pk PRIMARY KEY (id)\n"
                + ")";
        String createPerson = "CREATE TABLE person (\n"
                + "id NUMBER NOT NULL,\n"
                + "country_id NUMBER NOT NULL,\n"
                + "name VARCHAR2(255) NOT NULL,\n"
                + "CONSTRAINT person_fk PRIMARY KEY(id),\n"
                + "CONSTRAINT person_country_fk FOREIGN KEY (country_id) REFERENCES country(id) \n"
                + ") ";
        jdbcTemplate.execute(createCountry);
        jdbcTemplate.execute(createPerson);
        String createCountryNew = "CREATE TABLE country_new (\n"
                + "  id INT NOT NULL,\n"
                + "  name VARCHAR(255) NOT NULL,\n"
                + "  CONSTRAINT country_new_pk PRIMARY KEY (id)\n"
                + ")";
        jdbcTemplate.execute(createCountryNew);
    }

    @Test
    public void test_disable_foreign_key() {
        List<DBTableConstraint> dbTableConstraints =
                getDbTableConstraints(personTableName, DBTableConstraint::getEnabled);
        Assert.assertTrue(dbTableConstraints.size() > 0);
        foreignKeyHandler.disableForeignKeyCheck(connectionConfig.getDefaultSchema(), personTableName);
        dbTableConstraints = getDbTableConstraints(personTableName, DBTableConstraint::getEnabled);
        Assert.assertEquals(0, dbTableConstraints.size());
        foreignKeyHandler.enableForeignKeyCheck(connectionConfig.getDefaultSchema(), personTableName);
        dbTableConstraints = getDbTableConstraints(personTableName, DBTableConstraint::getEnabled);
        Assert.assertTrue(dbTableConstraints.size() > 0);
    }

    @Test
    public void test_alterTableForeignKeyReference() {
        alterTableForeignKeyReference();
        dropAllForeignKeysOnTable();
    }

    private void alterTableForeignKeyReference() {
        String sql = String.format("rename %s to %s", countryNameOrigin, countryNameOld);
        jdbcTemplate.execute(sql);
        Optional<DBTableConstraint> dbTableConstraint =
                getDbTableConstraints(personTableName,
                        a -> Objects.equals(a.getReferenceTableName(), countryNameOld))
                                .stream().findAny();
        Assert.assertTrue(dbTableConstraint.isPresent());
        foreignKeyHandler.alterTableForeignKeyReference(connectionConfig.getDefaultSchema(),
                countryNameOld, countryNameNew);
        dbTableConstraint =
                getDbTableConstraints(personTableName,
                        a -> Objects.equals(a.getReferenceTableName(), countryNameNew))
                                .stream().findAny();
        Assert.assertTrue(dbTableConstraint.isPresent());
    }

    private void dropAllForeignKeysOnTable() {
        foreignKeyHandler.dropAllForeignKeysOnTable(connectionConfig.getDefaultSchema(), personTableName);
        Optional<DBTableConstraint> dbTableConstraint =
                getDbTableConstraints(personTableName, null)
                        .stream().findAny();
        Assert.assertFalse(dbTableConstraint.isPresent());
    }

    private List<DBTableConstraint> getDbTableConstraints(String tableName, Predicate<DBTableConstraint> predicate) {
        return DBSchemaAccessors.create(connectionSession)
                .listTableConstraints(connectionConfig.getDefaultSchema(), tableName)
                .stream().filter(a -> a.getType() == DBConstraintType.FOREIGN_KEY)
                .filter(a -> predicate == null || predicate.test(a))
                .collect(Collectors.toList());
    }
}
