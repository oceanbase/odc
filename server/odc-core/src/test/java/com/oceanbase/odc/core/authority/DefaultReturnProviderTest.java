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
package com.oceanbase.odc.core.authority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.authority.auth.DefaultAuthorizerManager;
import com.oceanbase.odc.core.authority.auth.DefaultPermissionStrategy;
import com.oceanbase.odc.core.authority.auth.DefaultReturnValueProvider;
import com.oceanbase.odc.core.authority.auth.ReturnValueProvider;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.model.UsernamePrincipal;
import com.oceanbase.odc.core.authority.tool.TestAuthorizer;
import com.oceanbase.odc.core.authority.tool.TestResourceFactory;
import com.oceanbase.odc.core.authority.tool.TestResourceFactory.ProviderTestResource;
import com.oceanbase.odc.core.authority.tool.TestResourceFactory.ProviderTestResourceNest;

/**
 * Test object for <code>OdcReturnValueProvider</code>
 *
 * @author yh263208
 * @date 2021-08-04 14:31
 * @since ODC-release_3.2.0
 */
public class DefaultReturnProviderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void decide_rightUserprincipal_granted() {
        UsernamePrincipal principal = new UsernamePrincipal("David");
        Subject subject = new Subject(true, Collections.singleton(principal),
                Collections.emptySet(), Collections.emptySet());
        Object returnVal = getProvider().decide(subject, TestResourceFactory.getResource_1(), null);
        Assert.assertNotNull(returnVal);
    }

    @Test(expected = AccessDeniedException.class)
    public void decide_wrongUserprincipal_notGranted() {
        UsernamePrincipal principal = new UsernamePrincipal("David");
        Subject subject = new Subject(true, Collections.singleton(principal),
                Collections.emptySet(), Collections.emptySet());
        getProvider().decide(subject, TestResourceFactory.getResource_2(), null);
    }

    @Test
    public void decide_nestObject_decideValueSucceed() {
        UsernamePrincipal principal = new UsernamePrincipal("David");
        Subject subject = new Subject(true, Collections.singleton(principal),
                Collections.emptySet(), Collections.emptySet());
        ProviderTestResourceNest result = TestResourceFactory.getNestResource_4();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getResource_1());
        Assert.assertNotNull(result.getResource_2());
        Assert.assertNotNull(result.getResource_3());

        result = (ProviderTestResourceNest) getProvider().decide(subject, result, null);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getResource_1());
        Assert.assertNull(result.getResource_2());
        Assert.assertNull(result.getResource_3());
    }

    @Test
    public void decide_mapReturnValue_decideValueSucceed() {
        Map<String, ProviderTestResource> map = new HashMap<>();
        map.putIfAbsent("1", TestResourceFactory.getResource_1());
        map.putIfAbsent("2", TestResourceFactory.getResource_2());
        map.putIfAbsent("3", TestResourceFactory.getResource_3());
        UsernamePrincipal principal = new UsernamePrincipal("David");
        Subject subject =
                new Subject(true, Collections.singleton(principal), Collections.emptySet(), Collections.emptySet());
        map = (Map<String, ProviderTestResource>) getProvider().decide(subject, map, null);
        Assert.assertEquals(1, map.size());
        ProviderTestResource resource = map.get("1");
        Assert.assertEquals(TestResourceFactory.getResource_1().resourceId(), resource.resourceId());
    }

    @Test
    public void decide_listReturnValue_decideValueSucceed() {
        List<ProviderTestResource> list = new ArrayList<>(Arrays.asList(
                TestResourceFactory.getResource_1(),
                TestResourceFactory.getResource_2(),
                TestResourceFactory.getResource_3()));
        UsernamePrincipal principal = new UsernamePrincipal("David");
        Subject subject = new Subject(true, Collections.singleton(principal),
                Collections.emptySet(), Collections.emptySet());

        list = (List<ProviderTestResource>) getProvider().decide(subject, list, null);
        Assert.assertEquals(1, list.size());
        ProviderTestResource resource = list.get(0);
        Assert.assertEquals(TestResourceFactory.getResource_1().resourceId(), resource.resourceId());
    }

    @Test
    public void decide_listMultiReturnValue_decideValueSucceed() {
        List<SecurityResource> list = new ArrayList<>(Arrays.asList(
                TestResourceFactory.getResource_1(),
                TestResourceFactory.getResource_2(),
                TestResourceFactory.getResource_3(),
                TestResourceFactory.getNestResource_4()));
        UsernamePrincipal principal = new UsernamePrincipal("David");
        Subject subject = new Subject(true, Collections.singleton(principal),
                Collections.emptySet(), Collections.emptySet());

        list = (List<SecurityResource>) getProvider().decide(subject, list, null);
        Assert.assertEquals(2, list.size());
    }

    @Test
    public void decide_mapMultiReturnValue_decideValueSucceed() {
        Map<String, SecurityResource> map = new HashMap<>();
        map.putIfAbsent("1", TestResourceFactory.getResource_1());
        map.putIfAbsent("2", TestResourceFactory.getResource_2());
        map.putIfAbsent("3", TestResourceFactory.getResource_3());
        map.putIfAbsent("4", TestResourceFactory.getNestResource_4());
        UsernamePrincipal principal = new UsernamePrincipal("David");
        Subject subject = new Subject(true, Collections.singleton(principal),
                Collections.emptySet(), Collections.emptySet());

        map = (Map<String, SecurityResource>) getProvider().decide(subject, map, null);
        Assert.assertEquals(2, map.size());
        ProviderTestResource resource = (ProviderTestResource) map.get("1");
        Assert.assertEquals(TestResourceFactory.getResource_1().resourceId(), resource.resourceId());

        ProviderTestResourceNest result = (ProviderTestResourceNest) map.get("4");
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getResource_1());
        Assert.assertNull(result.getResource_2());
        Assert.assertNull(result.getResource_3());
    }

    private ReturnValueProvider getProvider() {
        DefaultAuthorizerManager authorizerManager =
                new DefaultAuthorizerManager(Collections.singletonList(new TestAuthorizer()));
        return new DefaultReturnValueProvider(authorizerManager, new DefaultPermissionStrategy());
    }

}
