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
package com.oceanbase.odc.metadb.iam;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;

public class LoginHistoryRepositoryTest extends ServiceTestEnv {

    @Autowired
    private LoginHistoryRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void lastLoginTimeByUserIds() {
        create();

        List<LastSuccessLoginHistory> histories = repository.lastSuccessLoginHistoryByUserIds(Arrays.asList(1L));

        Assert.assertEquals(1, histories.size());
    }

    LoginHistoryEntity create() {
        LoginHistoryEntity entity = new LoginHistoryEntity();
        entity.setLoginTime(OffsetDateTime.now());
        entity.setAccountName("user1");
        entity.setSuccess(true);
        entity.setUserId(1L);
        return repository.saveAndFlush(entity);
    }
}
