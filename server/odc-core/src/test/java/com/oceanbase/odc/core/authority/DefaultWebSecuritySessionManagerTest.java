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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.oceanbase.odc.core.authority.session.InMemorySecuritySessionRepository;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.factory.DefaultSecuritySessionFactory;
import com.oceanbase.odc.core.authority.session.manager.DefaultWebSecuritySessionManager;
import com.oceanbase.odc.core.authority.session.validate.ExpiredSecuritySessionValidator;
import com.oceanbase.odc.core.authority.util.SecurityConstants;
import com.oceanbase.odc.core.authority.util.WebUtil;

public class DefaultWebSecuritySessionManagerTest {

    @Test
    public void startSession_usingDefaultWebSecuritySessionManager_startSessionSucceed() {
        // Init a session manager
        DefaultWebSecuritySessionManager manager = new DefaultWebSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        manager.addSessionValidator(new ExpiredSecuritySessionValidator());

        // Mock Http request and response
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRemoteHost()).thenReturn("127.0.0.1");
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Capture arguments
        ArgumentCaptor<Cookie> capture = ArgumentCaptor.forClass(Cookie.class);
        Map<String, Object> context = new HashMap<>();
        context.putIfAbsent(SecurityConstants.CONTEXT_SESSION_TIMEOUT_KEY, 3000);
        SecuritySession session = manager.start(request, response, context);
        Mockito.verify(response).addCookie(capture.capture());

        // Assert logic
        Assert.assertNotNull(capture.getValue());
        Assert.assertEquals(capture.getValue().getValue(), session.getId());
    }

    @Test
    public void getSession_usingDefaultWebSecuritySessionManager_getSessionSucceed() {
        // Init a session manager
        Map<String, Object> context = new HashMap<>();
        context.putIfAbsent(SecurityConstants.CONTEXT_SESSION_TIMEOUT_KEY, 3000);
        DefaultWebSecuritySessionManager manager =
                new DefaultWebSecuritySessionManager(new DefaultSecuritySessionFactory(),
                        new InMemorySecuritySessionRepository());
        manager.addSessionValidator(new ExpiredSecuritySessionValidator());

        // Mock HTTP request and response
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRemoteHost()).thenReturn("127.0.0.1");
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Assert logic
        SecuritySession session = manager.start(request, response, context);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] {WebUtil.generateSecurityCookie(session)});
        Mockito.when(request.getRequestURI()).thenReturn("/");
        SecuritySession session1 = manager.getSession(request, response);
        Assert.assertEquals(session, session1);
    }

}
