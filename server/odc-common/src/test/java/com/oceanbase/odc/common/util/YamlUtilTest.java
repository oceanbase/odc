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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;
import lombok.Setter;

/**
 * @author yaobin
 * @date 2023-07-28
 * @since 4.2.0
 */
public class YamlUtilTest {
    private static final String MIGRATE_CONFIG_FILE = "default-audit-event-meta.yml";

    @Test
    public void test() {
        List<AuditEventMetaEntity> expected = YamlUtils.fromYaml(MIGRATE_CONFIG_FILE,
                new TypeReference<List<AuditEventMetaEntity>>() {});

        Assert.assertFalse(CollectionUtils.isEmpty(expected));
        Assert.assertNotNull(expected.get(0).getMethodSignature());

    }

    @Getter
    @Setter
    private static class AuditEventMetaEntity {
        private Long id;
        private String methodSignature;
        private String type;
        private String action;
        private String sidExtractExpression;
        private Boolean inConnection;
        private Boolean enabled;
    }

}
