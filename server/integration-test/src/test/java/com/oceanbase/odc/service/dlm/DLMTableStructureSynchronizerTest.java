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
package com.oceanbase.odc.service.dlm;

import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;

import cn.hutool.core.lang.Assert;

/**
 * @Author：tinker
 * @Date: 2024/8/6 12:12
 * @Descripition:
 */
public class DLMTableStructureSynchronizerTest {

    @Test
    public void isSupportedSyncTableStructure_success() {
        boolean supportedSyncTableStructure = DLMTableStructureSynchronizer
                .isSupportedSyncTableStructure(DialectType.MYSQL, "5.7.0", DialectType.MYSQL, "5.7.0");
        Assert.isTrue(supportedSyncTableStructure);
    }

    @Test
    public void isSupportedSyncTableStructure_failed() {
        boolean supportedSyncTableStructure = DLMTableStructureSynchronizer
                .isSupportedSyncTableStructure(DialectType.MYSQL, "5.6.0", DialectType.MYSQL, "5.7.0");
        Assert.isFalse(supportedSyncTableStructure);
    }
}
