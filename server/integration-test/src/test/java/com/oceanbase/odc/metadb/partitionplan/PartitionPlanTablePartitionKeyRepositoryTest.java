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
 * Test cases for {@link PartitionPlanTablePartitionKeyRepository}
 *
 * @author yh263208
 * @date 2024-01-10 17:08
 * @since ODC_release_4.2.4
 */
public class PartitionPlanTablePartitionKeyRepositoryTest extends ServiceTestEnv {

    @Autowired
    private PartitionPlanTablePartitionKeyRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void save_saveOne_saveSucceed() {
        PartitionPlanTablePartitionKeyEntity actual = createRoleEntity();
        actual.setId(null);
        actual = this.repository.save(actual);
        Optional<PartitionPlanTablePartitionKeyEntity> expect = this.repository.findById(actual.getId());
        Assert.assertEquals(expect.get(), actual);
    }

    @Test
    public void batchCreate_saveAll_saveSucceed() {
        PartitionPlanTablePartitionKeyEntity p1 = createRoleEntity();
        p1.setId(null);

        PartitionPlanTablePartitionKeyEntity p2 = createRoleEntity();
        p2.setId(null);
        List<PartitionPlanTablePartitionKeyEntity> actual = this.repository.batchCreate(Arrays.asList(p1, p2));
        List<PartitionPlanTablePartitionKeyEntity> expect = this.repository.findAll();
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

    @Test
    public void findByPartitionplanTableIdInAndEnabled_noCandidate_returnNull() {
        PartitionPlanTablePartitionKeyEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(false);
        actual = this.repository.save(actual);
        List<PartitionPlanTablePartitionKeyEntity> expect = this.repository.findByPartitionplanTableIdInAndEnabled(
                Collections.singletonList(actual.getPartitionplanTableId()), true);
        Assert.assertTrue(expect.isEmpty());
    }

    @Test
    public void findByPartitionplanTableIdInAndEnabled_candidateExists_returnNotNull() {
        PartitionPlanTablePartitionKeyEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        List<PartitionPlanTablePartitionKeyEntity> expect = this.repository.findByPartitionplanTableIdInAndEnabled(
                Collections.singletonList(actual.getPartitionplanTableId()), true);
        Assert.assertEquals(expect, Collections.singletonList(actual));
    }

    private PartitionPlanTablePartitionKeyEntity createRoleEntity() {
        return TestRandom.nextObject(PartitionPlanTablePartitionKeyEntity.class);
    }

}
