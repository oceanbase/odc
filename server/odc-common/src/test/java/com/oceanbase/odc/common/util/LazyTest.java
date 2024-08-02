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
package com.oceanbase.odc.common.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/11
 */
public class LazyTest {

    @Test
    public void test_LazyInitialize_Get_Success() {
        DummyCounter counter = new DummyCounter();
        Assert.assertEquals(0, counter.cnt);
        Lazy<DummyCounter> o = new Lazy<>(() -> {
            counter.count();
            return counter;
        });
        Assert.assertEquals(1, o.get().cnt);
        Assert.assertEquals(1, o.get().cnt);
    }

    private static class DummyCounter {

        private int cnt = 0;

        private void count() {
            cnt++;
        }

    }

}
