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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.security.auth.Subject;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.authority.auth.Authenticator;
import com.oceanbase.odc.core.authority.auth.DefaultAuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.EmptyAuthenticator;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.AuthenticationInfo;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;
import com.oceanbase.odc.core.authority.model.BasicAuthenticationToken;

/**
 * Test object for {@link Authenticator}
 *
 * @author yh263208
 * @date 2021-07-20 15:51
 * @since ODC_release_3.2.0
 */
public class DefaultAuthenticatorManagerTest {

    @Test
    public void authenticate_emptyAuthenticator_authencateSucceed() throws AuthenticationException {
        BasicAuthenticationToken token = new BasicAuthenticationToken("David", "123456");
        EmptyAuthenticator authenticator = new EmptyAuthenticator();
        AuthenticationInfo<? extends Principal, ?> info = authenticator.authenticate(token);
        Assert.assertEquals(token.getPrincipal(), info.getPrincipal());
        Assert.assertEquals(token.getCredential(), info.getCredential());
    }

    @Test
    public void authenticate_twoAuthenticationTokens_authencateSucceed() throws AuthenticationException {
        DefaultAuthenticatorManager manager = new DefaultAuthenticatorManager(Collections.emptyList());
        EmptyAuthenticator authenticator = new EmptyAuthenticator();
        manager.addAuthenticator(authenticator);
        List<BaseAuthenticationToken<? extends Principal, ?>> authenticationTokens = Arrays.asList(
                new BasicAuthenticationToken("David", "123456"), new BasicAuthenticationToken("Marry", "123456"));
        Subject subject = manager.authenticate(authenticationTokens);
        Assert.assertEquals(2, subject.getPrincipals().size());
    }

    @Test
    public void authenticate_twoAuthenticationTokensWithSampleName_authencateSucceed() throws AuthenticationException {
        DefaultAuthenticatorManager manager = new DefaultAuthenticatorManager(Collections.emptyList());
        EmptyAuthenticator authenticator = new EmptyAuthenticator();
        manager.addAuthenticator(authenticator);
        List<BaseAuthenticationToken<? extends Principal, ?>> authenticationTokens = Arrays.asList(
                new BasicAuthenticationToken("David", "123456"), new BasicAuthenticationToken("David", "123456"));
        Subject subject = manager.authenticate(authenticationTokens);
        Assert.assertEquals(1, subject.getPrincipals().size());
    }

    @Test
    public void authenticate_toString_authencateSucceed() {
        BaseAuthenticationToken<? extends Principal, ?> token = new BasicAuthenticationToken("David", "123456");
        Assert.assertEquals(token.getName(), "David");
        BaseAuthenticationToken<? extends Principal, ?> token1 = new BasicAuthenticationToken("David", "123456");
        Assert.assertNotEquals(null, token);
        Assert.assertEquals(token, token1);
        Assert.assertEquals(31 ^ token.getPrincipal().hashCode() ^ "123456".hashCode(), token.hashCode());
        Assert.assertTrue(token.toString().contains("Principal: UsernamePrincipal: David; Credentials: [PROTECTED];"));
    }

}
