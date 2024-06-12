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
package com.oceanbase.odc.service.permission;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.permission.common.PermissionCheckWhitelist;

/**
 * @author gaoda.xy
 * @date 2024/5/20 15:10
 */
public class PermissionCheckWhitelistTest extends ServiceTestEnv {

    @Autowired
    private PermissionCheckWhitelist permissionCheckWhitelist;

    @Test
    public void test_checkWhitelist_OBMySQL() {
        Set<String> whitelist = permissionCheckWhitelist.getDatabaseWhitelist(DialectType.OB_MYSQL);
        Assert.assertTrue(whitelist instanceof TreeSet);
        Assert.assertTrue(whitelist.contains("DBMS_STATS"));
        Assert.assertTrue(whitelist.contains("dbms_stats"));
    }

    @Test
    public void test_checkWhitelist_OBOracle() {
        Set<String> whitelist = permissionCheckWhitelist.getDatabaseWhitelist(DialectType.OB_ORACLE);
        Assert.assertTrue(whitelist instanceof TreeSet);
        Assert.assertTrue(whitelist.contains("DBMS_OUTPUT"));
        Assert.assertTrue(whitelist.contains("dbms_output"));
    }

    @Test
    public void test_checkWhitelist_Doris() {
        Set<String> whitelist = permissionCheckWhitelist.getDatabaseWhitelist(DialectType.DORIS);
        Assert.assertTrue(whitelist instanceof TreeSet);
        Assert.assertFalse(whitelist.contains("DBMS_OUTPUT"));
    }

}
