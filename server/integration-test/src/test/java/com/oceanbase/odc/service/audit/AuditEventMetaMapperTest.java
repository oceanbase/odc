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
package com.oceanbase.odc.service.audit;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.metadb.audit.AuditEventMetaEntity;
import com.oceanbase.odc.service.audit.model.AuditEventMeta;
import com.oceanbase.odc.service.audit.util.AuditEventMetaMapper;

public class AuditEventMetaMapperTest {
    private AuditEventMetaMapper mapper = AuditEventMetaMapper.INSTANCE;

    @Test
    public void test_EntityToModel_Success() {
        AuditEventMetaEntity entity = new AuditEventMetaEntity();
        entity.setSidExtractExpression("test");
        entity.setMethodSignature("controller");
        entity.setEnabled(true);
        entity.setType(AuditEventType.PERSONAL_CONFIGURATION);
        entity.setAction(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION);
        entity.setInConnection(true);

        AuditEventMeta expected = AuditEventMeta.builder()
                .action(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION)
                .type(AuditEventType.PERSONAL_CONFIGURATION)
                .enabled(true)
                .inConnection(true)
                .methodSignature("controller")
                .sidExtractExpression("test")
                .build();

        AuditEventMeta actual = mapper.entityToModel(entity);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test_ModelToEntity_Success() {
        AuditEventMeta model = AuditEventMeta.builder()
                .action(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION)
                .type(AuditEventType.PERSONAL_CONFIGURATION)
                .enabled(true)
                .inConnection(true)
                .methodSignature("controller")
                .sidExtractExpression("test")
                .build();

        AuditEventMetaEntity expected = new AuditEventMetaEntity();
        expected.setSidExtractExpression("test");
        expected.setMethodSignature("controller");
        expected.setEnabled(true);
        expected.setType(AuditEventType.PERSONAL_CONFIGURATION);
        expected.setAction(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION);
        expected.setInConnection(true);

        AuditEventMetaEntity actual = mapper.modelToEntity(model);
        Assert.assertEquals(expected, actual);
    }

}
