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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

/**
 * @author longpeng.zlp
 * @date 2024/10/28 15:38
 */
public class LockUserInterceptorTest {
    @Test
    public void testLockUserNameProcess() {
        LockUserInterceptor lockUserInterceptor = new LockUserInterceptor(Mockito.mock(ConnectionSession.class),
                Mockito.mock(DBSessionManageFacade.class));
        Assert.assertEquals(lockUserInterceptor.processLockUsers(null), Collections.emptyList());
        Assert.assertEquals(lockUserInterceptor.processLockUsers(Collections.emptyList()), Collections.emptyList());
        Assert.assertEquals(
                lockUserInterceptor.processLockUsers(
                        Arrays.asList("user1", "user2@", "user3@'%'", "user4@'127.0.0.1'")),
                Arrays.asList("user1", "user2@", "user2", "user3@'%'", "user3", "user4@'127.0.0.1'", "user4"));
    }
}
