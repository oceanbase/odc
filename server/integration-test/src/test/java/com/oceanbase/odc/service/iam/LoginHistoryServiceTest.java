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
package com.oceanbase.odc.service.iam;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.iam.LastSuccessLoginHistory;
import com.oceanbase.odc.metadb.iam.LoginHistoryRepository;
import com.oceanbase.odc.service.iam.model.LoginHistory;

public class LoginHistoryServiceTest extends ServiceTestEnv {
    @Autowired
    private LoginHistoryService service;
    @Autowired
    private LoginHistoryRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void lastSuccessLoginHistoryByUserIds() {
        LoginHistory history = new LoginHistory();
        history.setSuccess(true);
        history.setUserId(1L);
        history.setOrganizationId(1L);
        history.setAccountName("user1");
        history.setLoginTime(OffsetDateTime.now());
        service.record(history);

        Map<Long, LastSuccessLoginHistory> userId2History = service.lastSuccessLoginHistoryByUserIds(
                Collections.singletonList(1L));

        Assert.assertEquals(1, userId2History.size());
    }
}
