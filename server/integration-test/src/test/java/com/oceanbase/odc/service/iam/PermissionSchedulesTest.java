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

import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;

/**
 * @author gaoda.xy
 * @date 2024/1/3 13:03
 */
public class PermissionSchedulesTest extends ServiceTestEnv {

    @Autowired
    private PermissionSchedules permissionSchedules;

    @Autowired
    private PermissionRepository permissionRepository;

    @Before
    public void setUp() {
        permissionRepository.deleteAll();
    }

    @Test
    public void test_clearExpiredPermission() {
        long currentTime = System.currentTimeMillis();
        createPermissionEntity("query", "ODC_DATABASE:1", new Date(currentTime - 92 * 24 * 60 * 60 * 1000L));
        createPermissionEntity("change", "ODC_DATABASE:2", new Date(currentTime - 91 * 24 * 60 * 60 * 1000L));
        createPermissionEntity("change", "ODC_DATABASE:2", new Date(currentTime - 89 * 24 * 60 * 60 * 1000L));
        createPermissionEntity("change", "ODC_DATABASE:2", new Date(currentTime - 88 * 24 * 60 * 60 * 1000L));
        Assert.assertEquals(4, permissionRepository.findAllNoCareExpireTime().size());
        permissionSchedules.clearExpiredPermission();
        Assert.assertEquals(2, permissionRepository.findAllNoCareExpireTime().size());
    }

    private void createPermissionEntity(String action, String resourceIdentifier, Date expireTime) {
        PermissionEntity entity = new PermissionEntity();
        entity.setType(PermissionType.PUBLIC_RESOURCE);
        entity.setAction(action);
        entity.setCreatorId(1L);
        entity.setOrganizationId(1L);
        entity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
        entity.setBuiltIn(false);
        entity.setResourceIdentifier(resourceIdentifier);
        entity.setExpireTime(expireTime);
        permissionRepository.saveAndFlush(entity);
    }

}
