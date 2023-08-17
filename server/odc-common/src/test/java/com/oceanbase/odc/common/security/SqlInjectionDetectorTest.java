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
package com.oceanbase.odc.common.security;

import org.junit.Assert;
import org.junit.Test;

public class SqlInjectionDetectorTest {

    @Test
    public void isSqlInjection_ReturnTrue() {
        Assert.assertTrue(SqlInjectionDetector.isSqlInjection("oms_target' and 'Ab'=sleep(5) and '1"));
        Assert.assertTrue(
                SqlInjectionDetector.isSqlInjection("'and(select*from(select+sleep(5))a/**/union/**/select+1)='"));
    }

    @Test
    public void isNotSqlInjection_ReturnFalse() {
        Assert.assertFalse(SqlInjectionDetector.isSqlInjection("metadb"));
        Assert.assertFalse(SqlInjectionDetector.isSqlInjection("lebie_meta"));
        Assert.assertFalse(SqlInjectionDetector.isSqlInjection("lebie_meta"));
        Assert.assertFalse(SqlInjectionDetector.isSqlInjection("_lebie_metadb_"));
    }
}
