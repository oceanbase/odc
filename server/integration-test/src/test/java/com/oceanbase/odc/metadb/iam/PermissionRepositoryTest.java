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

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.PermissionType;

/**
 * @author gaoda.xy
 * @date 2024/1/3 12:42
 */
public class PermissionRepositoryTest extends ServiceTestEnv {

    @Autowired
    private PermissionRepository permissionRepository;

    @Before
    public void setUp() {
        permissionRepository.deleteAll();
    }

    @Test
    public void test_deleteByExpireTimeBefore() {
        long currentTime = System.currentTimeMillis();
        PermissionEntity entity = createPermissionEntity("query", "ODC_DATABASE:1", new Date(currentTime + 90 * 1000L));
        createPermissionEntity("change", "ODC_DATABASE:2", new Date(currentTime + 30 * 1000L));
        Assert.assertEquals(2, permissionRepository.findAll().size());
        permissionRepository.deleteByExpireTimeBefore(new Date(currentTime + 60 * 1000L));
        List<PermissionEntity> entities = permissionRepository.findAll();
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals(entity.getId(), entities.get(0).getId());
    }

    private PermissionEntity createPermissionEntity(String action, String resourceIdentifier, Date expireTime) {
        PermissionEntity entity = new PermissionEntity();
        entity.setType(PermissionType.PUBLIC_RESOURCE);
        entity.setAction(action);
        entity.setCreatorId(1L);
        entity.setOrganizationId(1L);
        entity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
        entity.setBuiltIn(false);
        entity.setResourceIdentifier(resourceIdentifier);
        entity.setExpireTime(expireTime);
        return permissionRepository.saveAndFlush(entity);
    }

}
