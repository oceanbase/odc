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
package com.oceanbase.odc.metadb.partitionplan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * Test cases for {@link PartitionPlanRepository}
 *
 * @author yh263208
 * @date 2024-01-10 16:57
 * @since ODC-release_4.2.4
 */
public class PartitionPlanRepositoryTest extends ServiceTestEnv {

    @Autowired
    private PartitionPlanRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void save_saveOne_saveSucceed() {
        PartitionPlanEntity actual = createRoleEntity();
        actual.setId(null);
        actual = this.repository.save(actual);
        Optional<PartitionPlanEntity> expect = this.repository.findById(actual.getId());
        Assert.assertEquals(expect.get(), actual);
    }

    @Test
    public void findByDatabaseIdIdAndEnabled_noCandidate_returnNull() {
        PartitionPlanEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(false);
        actual = this.repository.save(actual);
        List<PartitionPlanEntity> expect = this.repository.findByDatabaseIdAndEnabled(actual.getDatabaseId(), true);
        Assert.assertTrue(expect.isEmpty());
    }

    @Test
    public void findByDatabaseIdIdAndEnabled_candidateExists_returnNotNull() {
        PartitionPlanEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        List<PartitionPlanEntity> expect = this.repository.findByDatabaseIdAndEnabled(actual.getDatabaseId(), true);
        Assert.assertEquals(expect, Collections.singletonList(actual));
    }

    @Test
    public void findByIdIn_candidateExists_returnNotNull() {
        PartitionPlanEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        List<PartitionPlanEntity> expect = this.repository.findByIdIn(Collections.singletonList(actual.getId()));
        Assert.assertEquals(expect, Collections.singletonList(actual));
    }

    @Test
    public void updateEnabledByIds_convertToFalse_concertSucceed() {
        PartitionPlanEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        this.repository.updateEnabledAndLastModifierIdByIdIn(Collections.singletonList(actual.getId()), false, 123L);
        Optional<PartitionPlanEntity> optional = this.repository.findById(actual.getId());
        Assert.assertFalse(optional.get().getEnabled());
    }

    @Test
    public void findByFlowInstanceId_recordExists_findSucceed() {
        PartitionPlanEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        Optional<PartitionPlanEntity> expect = this.repository.findByFlowInstanceId(actual.getFlowInstanceId());
        Assert.assertEquals(expect.get(), actual);
    }

    @Test
    public void findByFlowInstanceId_recordNonExists_findNothing() {
        PartitionPlanEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        Optional<PartitionPlanEntity> expect = this.repository.findByFlowInstanceId(actual.getFlowInstanceId() + 1);
        Assert.assertFalse(expect.isPresent());
    }

    @Test
    public void batchCreate_saveAll_saveSucceed() {
        PartitionPlanEntity p1 = createRoleEntity();
        p1.setId(null);
        PartitionPlanEntity p2 = createRoleEntity();
        p2.setId(null);
        List<PartitionPlanEntity> actual = this.repository.batchCreate(Arrays.asList(p1, p2));
        List<PartitionPlanEntity> expect = this.repository.findAll();
        actual.forEach(p -> {
            p.setCreateTime(null);
            p.setUpdateTime(null);
        });
        expect.forEach(p -> {
            p.setCreateTime(null);
            p.setUpdateTime(null);
        });
        Assert.assertEquals(expect, actual);
    }

    private PartitionPlanEntity createRoleEntity() {
        return TestRandom.nextObject(PartitionPlanEntity.class);
    }

}
