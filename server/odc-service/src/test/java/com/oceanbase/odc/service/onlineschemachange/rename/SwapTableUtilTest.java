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
package com.oceanbase.odc.service.onlineschemachange.rename;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author longpeng.zlp
 * @date 2024/8/2 10:20
 * @since 4.3.1
 */
public class SwapTableUtilTest {
    @Test
    public void testEscapeName() {
        Assert.assertEquals(SwapTableUtil.escapeMySQLName("name   "), "`name   `");
        Assert.assertEquals(SwapTableUtil.escapeOracleName("name   "), "\"name   \"");
    }
}
