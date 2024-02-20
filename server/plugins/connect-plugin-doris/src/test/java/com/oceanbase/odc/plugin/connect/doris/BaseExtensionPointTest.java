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
package com.oceanbase.odc.plugin.connect.doris;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Assert;

import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.database.TestDBType;

import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: BaseExtensionPointTest Package: com.oceanbase.odc.plugin.connect.doris Description:
 *
 * @Author: fenghao
 * @Create 2024/1/8 10:48
 * @Version 1.0
 */
@Slf4j
public class BaseExtensionPointTest {
    protected static <T> T getInstance(Class<T> extensionClass) {
        log.debug("Create instance for extension '{}'", extensionClass.getName());
        try {
            Constructor<T> constructor = extensionClass.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "NoParametersConstructor not been found in class " + extensionClass.getCanonicalName());
        }
    }

    protected Connection getConnection(TestDBType type) {
        try {
            return TestDBConfigurations.getInstance(type)
                    .getTestDorisConfiguration().getDataSource().getConnection();

        } catch (SQLException exception) {
            Assert.assertNull("Get connection from datasource occur exception " + exception.getMessage(), exception);
            return null;
        }
    }
}
