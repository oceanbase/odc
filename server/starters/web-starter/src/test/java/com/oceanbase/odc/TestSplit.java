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
package com.oceanbase.odc;

import java.util.List;

import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.common.util.SqlUtils;

/**
 * @author yaobin
 * @date 2024-02-22
 * @since 4.2.4
 */
public class TestSplit {

    @Test
    public void test1() {
        String sql = " ALTER TABLE \"D\" ADD  C3 VARCHAR2(20); --添加列";

        List<String> split = SqlUtils.split(DialectType.OB_ORACLE, sql, ";", true);
        System.out.println(String.join(",", split));

        List<String> split2 = SqlUtils.split(DialectType.OB_ORACLE, sql, ";");

        System.out.println(String.join(",", split2));
    }
}
