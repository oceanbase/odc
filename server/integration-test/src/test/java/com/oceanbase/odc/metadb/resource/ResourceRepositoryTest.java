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
package com.oceanbase.odc.metadb.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceState;

/**
 * Test cases for {@link ResourceRepository}
 *
 * @author yh263208
 * @date 2024-09-07 14:23
 * @since ODC_release_4.3.2
 */
public class ResourceRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ResourceRepository repository;

    @Before
    public void setUp() {
        this.repository.deleteAll();
    }

    @Test
    public void save_normalEntity_saveSucceed() {
        ResourceEntity saved = getEntity("GCP");
        saved = this.repository.save(saved);
        Optional<ResourceEntity> optional = this.repository.findById(saved.getId());
        Assert.assertEquals(saved.getId(), optional.get().getId());
    }

    @Test
    public void findByResourceID_resourceExists_findSucceed() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        ResourceLocation location = new ResourceLocation("HangZhou", "AWS");
        Optional<ResourceEntity> optional = this.repository.findByResourceID(
                new ResourceID(location, "POD", "default", "pod_name_unique"));
        Assert.assertEquals(aws, optional.get());
    }

    @Test
    public void findByResourceID_resourceNonExists_findNothing() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        ResourceLocation location = new ResourceLocation("HangZhou", "ooo");
        Optional<ResourceEntity> optional = this.repository.findByResourceID(
                new ResourceID(location, "POD", "default", "pod_name_unique"));
        Assert.assertFalse(optional.isPresent());
    }

    @Test
    public void deleteResource_resourceExists_deleteSucceed() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        ResourceLocation location = new ResourceLocation("HangZhou", "AWS");
        int affectRows = this.repository.deleteResource(
                new ResourceID(location, "POD", "default", "pod_name_unique"));
        List<ResourceEntity> actual = this.repository.findAll();
        Assert.assertEquals(1, affectRows);
        Assert.assertEquals(Collections.singletonList(gcp), actual);
    }

    @Test
    public void deleteResource_resourceNonExists_deleteNothing() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        ResourceLocation location = new ResourceLocation("HangZhou", "kkk");
        int affectRows = this.repository.deleteResource(
                new ResourceID(location, "POD", "default", "pod_name_unique"));
        List<ResourceEntity> actual = this.repository.findAll();
        Assert.assertEquals(0, affectRows);
        Assert.assertEquals(2, actual.size());
    }

    @Test
    public void updateResourceStatus_resourceExists_updateSucceed() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        ResourceLocation location = new ResourceLocation("HangZhou", "AWS");
        int affectRows = this.repository.updateResourceStatus(
                new ResourceID(location, "POD", "default", "pod_name_unique"), ResourceState.AVAILABLE);
        Optional<ResourceEntity> actual = this.repository.findById(aws.getId());
        Assert.assertEquals(1, affectRows);
        Assert.assertEquals(ResourceState.AVAILABLE, actual.get().getStatus());
    }

    @Test
    public void updateResourceStatus_resourceNonExists_updateNothing() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        ResourceLocation location = new ResourceLocation("HangZhou", "kkk");
        int affectRows = this.repository.updateResourceStatus(
                new ResourceID(location, "POD", "default", "pod_name_unique"), ResourceState.AVAILABLE);
        Assert.assertEquals(0, affectRows);
    }

    @Test
    public void updateStatusById_resourceExists_updateSucceed() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        int affectRows = this.repository.updateStatusById(aws.getId(), ResourceState.AVAILABLE);
        Optional<ResourceEntity> actual = this.repository.findById(aws.getId());
        Assert.assertEquals(1, affectRows);
        Assert.assertEquals(ResourceState.AVAILABLE, actual.get().getStatus());
    }

    @Test
    public void updateStatusById_resourceNonExists_updateNothing() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        int affectRows = this.repository.updateStatusById(12345L, ResourceState.AVAILABLE);
        Assert.assertEquals(0, affectRows);
    }

    @Test
    public void updateStatusByIdIn_resourceExists_updateSucceed() {
        ResourceEntity gcp = getEntity("GCP");
        gcp = this.repository.save(gcp);

        ResourceEntity aws = getEntity("AWS");
        aws = this.repository.save(aws);

        int affectRows = this.repository.updateStatusByIdIn(
                Arrays.asList(aws.getId(), gcp.getId()), ResourceState.AVAILABLE);
        List<ResourceState> actual = this.repository.findAll().stream()
                .map(ResourceEntity::getStatus).collect(Collectors.toList());
        Assert.assertEquals(2, affectRows);
        Assert.assertEquals(Arrays.asList(ResourceState.AVAILABLE, ResourceState.AVAILABLE), actual);
    }

    private ResourceEntity getEntity(String group) {
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceType("POD");
        resourceEntity.setResourceProperties("{}");
        resourceEntity.setResourceName("pod_name_unique");
        resourceEntity.setNamespace("default");
        resourceEntity.setRegion("HangZhou");
        resourceEntity.setEndpoint("jbdc:oceanbase://xx.xx.xx.xx:xx");
        resourceEntity.setGroupName(group);
        resourceEntity.setStatus(ResourceState.CREATING);
        return resourceEntity;
    }

}
