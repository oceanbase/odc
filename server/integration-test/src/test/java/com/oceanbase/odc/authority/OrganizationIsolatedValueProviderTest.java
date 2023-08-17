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
package com.oceanbase.odc.authority;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.Subject;

import org.junit.Test;

import com.oceanbase.odc.core.authority.auth.DefaultAuthorizerManager;
import com.oceanbase.odc.core.authority.auth.DefaultPermissionStrategy;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.model.UsernamePrincipal;
import com.oceanbase.odc.service.iam.auth.DesktopAuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.OrganizationIsolatedValueProvider;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;

/**
 * Test object for {@link OrganizationIsolatedValueProvider}
 *
 * @author yh263208
 * @date 2021-08-04 14:31
 * @since ODC-release_3.2.0
 */
public class OrganizationIsolatedValueProviderTest {

    @Test
    public void testListReturnValueOrganizationIsolated_success() {
        UsernamePrincipal principal = new UsernamePrincipal("David");
        List<ApprovalFlowConfig> list = new LinkedList<>();
        ApprovalFlowConfig config = new ApprovalFlowConfig();
        config.setId(1L);
        config.setOrganizationId(1L);
        list.add(config);
        Subject subject =
                new Subject(true, Collections.singleton(principal), Collections.emptySet(), Collections.emptySet());
        getProvider().decide(subject, list, null);
    }

    @Test(expected = AccessDeniedException.class)
    public void testListReturnValueOrganizationIsolated_access_deny() {
        UsernamePrincipal principal = new UsernamePrincipal("David");
        List<ApprovalFlowConfig> list = new LinkedList<>();
        ApprovalFlowConfig config = new ApprovalFlowConfig();
        config.setId(2L);
        config.setOrganizationId(2L);
        list.add(config);
        Subject subject =
                new Subject(true, Collections.singleton(principal), Collections.emptySet(), Collections.emptySet());
        getProvider().decide(subject, list, null);
    }

    private OrganizationIsolatedValueProvider getProvider() {
        DefaultAuthorizerManager authorizerManager =
                new DefaultAuthorizerManager(Collections.singletonList(new TestAuthorizer()));
        return new OrganizationIsolatedValueProvider(authorizerManager, new DefaultPermissionStrategy(),
                new DesktopAuthenticationFacade());
    }

}
