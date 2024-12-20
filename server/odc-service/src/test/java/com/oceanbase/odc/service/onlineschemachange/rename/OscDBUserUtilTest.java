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

import com.oceanbase.odc.core.shared.constant.DialectType;

/**
 * @author longpeng.zlp
 * @date 2024/8/2 10:14
 * @since 4.3.1
 */
public class OscDBUserUtilTest {
    @Test
    public void testIsLockUserRequired() {
        Assert.assertFalse(OscDBUserUtil.isLockUserRequired(DialectType.OB_MYSQL, () -> "4.2.5",
                () -> LockTableSupportDecider.DEFAULT_LOCK_TABLE_DECIDER));
        Assert.assertTrue(OscDBUserUtil.isLockUserRequired(DialectType.OB_MYSQL, () -> "4.2.0",
                () -> LockTableSupportDecider.DEFAULT_LOCK_TABLE_DECIDER));
    }
}
