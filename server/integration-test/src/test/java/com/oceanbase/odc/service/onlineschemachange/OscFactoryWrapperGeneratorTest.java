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

import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlConstants;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapper;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapperGenerator;
import com.oceanbase.odc.service.onlineschemachange.ddl.TableNameDescriptor;
import com.oceanbase.odc.service.onlineschemachange.ddl.TableNameDescriptorFactory;

import cn.hutool.core.lang.Assert;

/**
 * @author yaobin
 * @date 2023-08-31
 * @since 4.2.0
 */
public class OscFactoryWrapperGeneratorTest {
    private static String oraclePrefix;
    private static String mysqlPrefix;
    private static String newTableSuffix;
    private static String renamedTableSuffix;

    @BeforeClass
    public static void before() {
        oraclePrefix = DdlConstants.OSC_TABLE_NAME_PREFIX_OB_ORACLE;
        mysqlPrefix = DdlConstants.OSC_TABLE_NAME_PREFIX;
        newTableSuffix = DdlConstants.NEW_TABLE_NAME_SUFFIX;
        renamedTableSuffix = DdlConstants.RENAMED_TABLE_NAME_SUFFIX;
    }

    @Test
    public void test_ob_oracle_table_name() {
        TableNameDescriptorFactory tableNameDescriptorFactory = getTableNameDescriptorFactory(DialectType.OB_ORACLE);
        TableNameDescriptor descriptor = tableNameDescriptorFactory.getTableNameDescriptor("t");
        Assert.equals("t", descriptor.getOriginTableNameUnwrapped());
        Assert.equals(oraclePrefix + "t" + newTableSuffix,
                descriptor.getNewTableName());
        Assert.equals(oraclePrefix + "t" + newTableSuffix,
                descriptor.getNewTableNameUnWrapped());
        Assert.equals(oraclePrefix + "t" + renamedTableSuffix,
                descriptor.getRenamedTableName());

    }

    @Test
    public void test_ob_oracle_table_name_quote() {
        TableNameDescriptorFactory tableNameDescriptorFactory = getTableNameDescriptorFactory(DialectType.OB_ORACLE);
        TableNameDescriptor descriptor = tableNameDescriptorFactory.getTableNameDescriptor("\"t\"");
        Assert.equals("t", descriptor.getOriginTableNameUnwrapped());
        Assert.equals(quote(oraclePrefix + "t" + newTableSuffix),
                descriptor.getNewTableName());
        Assert.equals(oraclePrefix + "t" + newTableSuffix,
                descriptor.getNewTableNameUnWrapped());
        Assert.equals(
                quote(oraclePrefix + "t" + renamedTableSuffix),
                descriptor.getRenamedTableName());
    }


    @Test
    public void test_ob_mysql_table_name() {
        TableNameDescriptorFactory tableNameDescriptorFactory = getTableNameDescriptorFactory(DialectType.OB_MYSQL);
        TableNameDescriptor descriptor = tableNameDescriptorFactory.getTableNameDescriptor("t");
        Assert.equals("t", descriptor.getOriginTableNameUnwrapped());
        Assert.equals(mysqlPrefix + "t" + newTableSuffix,
                descriptor.getNewTableName());
        Assert.equals(mysqlPrefix + "t" + newTableSuffix,
                descriptor.getNewTableNameUnWrapped());
        Assert.equals(mysqlPrefix + "t" + renamedTableSuffix,
                descriptor.getRenamedTableName());

    }

    @Test
    public void test_ob_mysql_table_name_accent() {
        TableNameDescriptorFactory tableNameDescriptorFactory = getTableNameDescriptorFactory(DialectType.OB_ORACLE);
        TableNameDescriptor descriptor = tableNameDescriptorFactory.getTableNameDescriptor("`t`");
        Assert.equals("t", descriptor.getOriginTableNameUnwrapped());
        Assert.equals(accent(oraclePrefix + "t" + newTableSuffix),
                descriptor.getNewTableName());
        Assert.equals(oraclePrefix + "t" + newTableSuffix,
                descriptor.getNewTableNameUnWrapped());
        Assert.equals(
                accent(oraclePrefix + "t" + renamedTableSuffix),
                descriptor.getRenamedTableName());
    }

    private TableNameDescriptorFactory getTableNameDescriptorFactory(DialectType dialectType) {
        OscFactoryWrapper generate = OscFactoryWrapperGenerator.generate(dialectType);
        return generate.getTableNameDescriptorFactory();
    }

    private String quote(String v) {
        return "\"" + v + "\"";
    }

    private String accent(String v) {
        return "`" + v + "`";
    }
}
