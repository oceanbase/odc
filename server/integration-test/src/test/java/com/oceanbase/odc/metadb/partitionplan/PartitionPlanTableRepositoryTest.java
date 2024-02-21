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
 * Test cases for {@link PartitionPlanTableRepositoryTest}
 *
 * @author yh263208
 * @date 2024-01-10 17:08
 * @since ODC_release_4.2.4
 */
public class PartitionPlanTableRepositoryTest extends ServiceTestEnv {

    @Autowired
    private PartitionPlanTableRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void save_saveOne_saveSucceed() {
        PartitionPlanTableEntity actual = createRoleEntity();
        actual.setId(null);
        actual = this.repository.save(actual);
        Optional<PartitionPlanTableEntity> expect = this.repository.findById(actual.getId());
        Assert.assertEquals(expect.get(), actual);
    }

    @Test
    public void batchCreate_saveAll_saveSucceed() {
        PartitionPlanTableEntity p1 = createRoleEntity();
        p1.setId(null);
        PartitionPlanTableEntity p2 = createRoleEntity();
        p2.setId(null);
        List<PartitionPlanTableEntity> actual = this.repository.batchCreate(Arrays.asList(p1, p2));
        List<PartitionPlanTableEntity> expect = this.repository.findAll();
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
    public void findByPartitionplanIdAndEnabled_noCandidate_returnNull() {
        PartitionPlanTableEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(false);
        actual = this.repository.save(actual);
        List<PartitionPlanTableEntity> expect = this.repository.findByPartitionPlanIdInAndEnabled(
                Collections.singletonList(actual.getPartitionPlanId()), true);
        Assert.assertTrue(expect.isEmpty());
    }

    @Test
    public void findByIdIn_candidateExists_returnNotNull() {
        PartitionPlanTableEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(false);
        actual = this.repository.save(actual);
        List<PartitionPlanTableEntity> expect = this.repository.findByIdIn(Collections.singletonList(actual.getId()));
        Assert.assertEquals(expect, Collections.singletonList(actual));
    }

    @Test
    public void findByPartitionplanIdAndEnabled_candidateExists_returnNotNull() {
        PartitionPlanTableEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        List<PartitionPlanTableEntity> expect = this.repository.findByPartitionPlanIdInAndEnabled(
                Collections.singletonList(actual.getPartitionPlanId()), true);
        Assert.assertEquals(expect, Collections.singletonList(actual));
    }

    @Test
    public void findByPartitionPlanIdIn_candidateExists_returnNotNull() {
        PartitionPlanTableEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        List<PartitionPlanTableEntity> expect = this.repository.findByPartitionPlanIdIn(
                Collections.singletonList(actual.getPartitionPlanId()));
        Assert.assertEquals(expect, Collections.singletonList(actual));
    }

    @Test
    public void updateEnabledByIdIn_convertToFalse_concertSucceed() {
        PartitionPlanTableEntity actual = createRoleEntity();
        actual.setId(null);
        actual.setEnabled(true);
        actual = this.repository.save(actual);
        this.repository.updateEnabledByIdIn(Collections.singletonList(actual.getId()), false);
        Optional<PartitionPlanTableEntity> optional = this.repository.findById(actual.getId());
        Assert.assertFalse(optional.get().getEnabled());
    }

    private PartitionPlanTableEntity createRoleEntity() {
        return TestRandom.nextObject(PartitionPlanTableEntity.class);
    }

}
