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
package com.oceanbase.odc.metadb.dlm;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;

import cn.hutool.core.lang.Assert;

/**
 * @Author：tinker
 * @Date: 2024/1/23 15:20
 * @Descripition:
 */
public class DlmLimiterConfigRepositoryTest extends ServiceTestEnv {

    @Autowired
    private DlmLimiterConfigRepository dlmLimiterConfigRepository;

    @Test
    public void testFindByOrderIds() {
        Random rand = new Random();
        DlmLimiterConfigEntity entity = createEntity(rand.nextLong());
        DlmLimiterConfigEntity entity2 = createEntity(rand.nextLong());
        List<Long> ids = new LinkedList<>();
        ids.add(entity.getOrderId());
        ids.add(entity2.getOrderId());
        List<DlmLimiterConfigEntity> byOrderIdIn = dlmLimiterConfigRepository.findByOrderIdIn(ids);
        Assert.equals(byOrderIdIn.size(), 2);
    }

    public DlmLimiterConfigEntity createEntity(Long orderId) {
        DlmLimiterConfigEntity entity = new DlmLimiterConfigEntity();
        entity.setRowLimit(100);
        entity.setDataSizeLimit(100L);

        entity.setBatchSize(100);
        entity.setOrderId(orderId);
        dlmLimiterConfigRepository.save(entity);
        return entity;
    }

}
