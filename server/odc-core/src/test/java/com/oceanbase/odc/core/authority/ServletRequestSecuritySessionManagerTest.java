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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.factory.DefaultSecuritySessionFactory;
import com.oceanbase.odc.core.authority.session.manager.ServletRequestSecuritySessionManager;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

public class ServletRequestSecuritySessionManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void startSession_servletRequestBaseSecuritySessionManager_startSessionSucceed() {
        // Init a session manager
        Map<String, Object> context = new HashMap<>();
        context.putIfAbsent(SecurityConstants.CONTEXT_SESSION_TIMEOUT_KEY, 3000);
        ServletRequestSecuritySessionManager manager =
                new ServletRequestSecuritySessionManager(new DefaultSecuritySessionFactory());

        // Mock Http request, response, session
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRemoteHost()).thenReturn("127.0.0.1");
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession(false)).thenReturn(httpSession);

        // Capture arguments
        ArgumentCaptor<SecuritySession> capture = ArgumentCaptor.forClass(SecuritySession.class);
        ArgumentCaptor<String> capture1 = ArgumentCaptor.forClass(String.class);

        // Assert arguments
        SecuritySession session = manager.start(request, response, context);
        Mockito.verify(httpSession).setAttribute(capture1.capture(), capture.capture());
        Assert.assertEquals(capture.getValue().getId(), session.getId());
    }

    @Test
    public void getSession_servletRequestBaseSecuritySessionManager_getSessionSucceed() {
        // Init a session manager
        Map<String, Object> context = new HashMap<>();
        context.putIfAbsent(SecurityConstants.CONTEXT_SESSION_TIMEOUT_KEY, 3000);
        ServletRequestSecuritySessionManager manager =
                new ServletRequestSecuritySessionManager(new DefaultSecuritySessionFactory());

        // Mock Http request, response, session
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRemoteHost()).thenReturn("127.0.0.1");
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession(false)).thenReturn(httpSession);

        // Capture arguments
        ArgumentCaptor<SecuritySession> capture = ArgumentCaptor.forClass(SecuritySession.class);
        ArgumentCaptor<String> capture1 = ArgumentCaptor.forClass(String.class);
        SecuritySession session = manager.start(request, response, context);
        Mockito.verify(httpSession).setAttribute(capture1.capture(), capture.capture());
        Mockito.when(httpSession.getAttribute(SecurityConstants.HTTPSESSION_SECURITY_SESSION_KEY))
                .thenReturn(capture.getValue());

        // Assert logic
        SecuritySession session1 = manager.getSession(request, response);
        Assert.assertEquals(session1, session);
    }

    @Test
    public void enableAsyncRefreshSessionManager_servletRequestBaseSecuritySessionManager_notSupported() {
        ServletRequestSecuritySessionManager manager =
                new ServletRequestSecuritySessionManager(new DefaultSecuritySessionFactory());
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Not support");
        manager.enableAsyncRefreshSessionManager();
    }

    @Test
    public void retrieveAllSessions_servletRequestBaseSecuritySessionManager_notSupported() {
        ServletRequestSecuritySessionManager manager =
                new ServletRequestSecuritySessionManager(new DefaultSecuritySessionFactory());
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Not support");
        manager.retrieveAllSessions();
    }

    @Test
    public void removeCertainSession_servletRequestBaseSecuritySessionManager_notSupported() {
        ServletRequestSecuritySessionManager manager =
                new ServletRequestSecuritySessionManager(new DefaultSecuritySessionFactory());
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Not support");
        SecuritySession session = Mockito.mock(SecuritySession.class);
        Mockito.when(session.getId()).thenReturn("123456");
        manager.removeCertainSession(session);
    }

}
