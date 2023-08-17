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

import org.apache.commons.lang.Validate;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.service.iam.util.ResourceContextUtil;
import com.oceanbase.odc.service.resourcegroup.model.ResourceContext;

/**
 * @author wenniu.ly
 * @date 2021/7/28
 */
public class ResourceContextUtilTest {

    @Test
    public void testSingleLevel() {
        String resourceIdentifier = "resource_group:10";
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
        Validate.notNull(resourceIdentifier);
        Validate.isTrue(resourceContext.getId() == 10);
        Validate.isTrue("resource_group".equals(resourceContext.getField()));
        Validate.isTrue(resourceContext.getSubContexts() == null);
    }

    @Test(expected = BadArgumentException.class)
    public void testSingleLevelWithIllegalMultiValue() {
        String resourceIdentifier = "resource_group:10,20";
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
    }

    @Test(expected = BadArgumentException.class)
    public void testSingleLevelWithIllegalField() {
        String resourceIdentifier = "resourc@e_group:10";
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
    }

    @Test(expected = BadArgumentException.class)
    public void testSingleLevelWithIllegalDelimiter() {
        String resourceIdentifier = "resource_group@10";
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
    }

    @Test
    public void testDoubleLevel() {
        String resourceIdentifier = "resource_group:10/public_connection:1,3,5";
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
        Validate.notNull(resourceIdentifier);
        Validate.isTrue(resourceContext.getId() == 10);
        Validate.isTrue("resource_group".equals(resourceContext.getField()));
        Validate.isTrue(resourceContext.getSubContexts().size() == 3);
        Validate.isTrue("public_connection".equals(resourceContext.getSubContexts().get(0).getField()));
    }

    @Test
    public void testDoubleLevelWithMultiElements() {
        String resourceIdentifier = "resource_group:10/public_connection:1,3,5;other_sub_resource:2,4,6";
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
        Validate.notNull(resourceIdentifier);
        Validate.isTrue(resourceContext.getId() == 10);
        Validate.isTrue("resource_group".equals(resourceContext.getField()));
        Validate.isTrue(resourceContext.getSubContexts().size() == 6);
        Validate.isTrue("other_sub_resource".equals(resourceContext.getSubContexts().get(5).getField()));
    }

    @Test
    public void testDoubleLevelWithAllElements() {
        String resourceIdentifier = "resource_group:10/public_connection:*";
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
        Validate.notNull(resourceIdentifier);
        Validate.isTrue(resourceContext.getId() == 10);
        Validate.isTrue("resource_group".equals(resourceContext.getField()));
        Validate.isTrue(resourceContext.getSubContexts().size() == 1);
    }

    @Test(expected = BadArgumentException.class)
    public void testDoubleLevelWithIllegalDelimiter() {
        String resourceIdentifier = "resource_group:10/public_connection@1,3,5";
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
    }

    @Test
    public void testGenerateResourceIdentifierString() {
        String resourceIdentifierStr =
                ResourceContextUtil.generateResourceIdentifierString(10L, ResourceType.ODC_RESOURCE_GROUP);
        Validate.isTrue("ODC_RESOURCE_GROUP:10/ODC_CONNECTION:*".equals(resourceIdentifierStr));
    }

    @Test
    public void testMatchResourceIdentifier() {
        String resourceIdentifier = "ODC_RESOURCE_GROUP:10";
        boolean match =
                ResourceContextUtil.matchResourceIdentifier(resourceIdentifier, 10L, ResourceType.ODC_RESOURCE_GROUP);
        Validate.isTrue(match);
    }
}
