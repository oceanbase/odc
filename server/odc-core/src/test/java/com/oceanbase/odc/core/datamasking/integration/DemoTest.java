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
package com.oceanbase.odc.core.datamasking.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import com.oceanbase.odc.core.datamasking.config.FieldConfig;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datamasking.masker.DataMaskerFactory;
import com.oceanbase.odc.core.datamasking.masker.ValueMeta;

/**
 * @author wenniu.ly
 * @date 2022/8/23
 */

public class DemoTest {
    @Test
    public void test_null_csv_demo() {
        FieldConfig fieldConfig = FieldConfig.builder().fieldName("t1").algorithmType("null").build();
        MaskConfig maskConfig = new MaskConfig();
        maskConfig.addFieldConfig(fieldConfig);

        DataMaskerFactory factory = new DataMaskerFactory();
        AbstractDataMasker abstractDataMasker = factory.createDataMasker("single_value", maskConfig);
        ValueMeta valueMeta = new ValueMeta("string", "t1");
        String r = abstractDataMasker.mask("abc", valueMeta);

        System.out.println(r);
    }

    private ResultSet getResultSet() throws ClassNotFoundException, SQLException {
        String JDBC_URL = "jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_UPPER=false;";
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(JDBC_URL);
        Statement stmt = conn.createStatement();
        // drop table;
        stmt.execute("drop table if exists `wn_test`");
        // create table;
        stmt.execute("CREATE TABLE `wn_test` (\n"
                + "  `id` int(11) NOT NULL AUTO_INCREMENT,\n"
                + "  `a` varchar(50) DEFAULT NULL,\n"
                + "  `b` int(11) DEFAULT NULL,\n"
                + "  `c` varchar(100) DEFAULT NULL\n"
                + ")");
        // insert data
        stmt.execute("insert into `wn_test`(`id`,`a`,`b`,`c`) values(DEFAULT,'abcd',2,'1234567890')");
        stmt.execute("insert into `wn_test`(`id`,`a`,`b`,`c`) values(DEFAULT,'test_value_to_null',3,'1234567890')");
        stmt.execute(
                "insert into `wn_test`(`id`,`a`,`b`,`c`) values(DEFAULT,'initial_test_demo_use_value',2,'1234567890')");

        stmt.execute("select * from `wn_test`");
        ResultSet rs = stmt.getResultSet();
        return rs;
    }
}
