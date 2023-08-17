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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.authority.session.DelegateSecuritySession;
import com.oceanbase.odc.core.authority.session.EmptySecuritySessionEventListener;
import com.oceanbase.odc.core.authority.session.InMemorySecuritySessionRepository;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionManager;
import com.oceanbase.odc.core.authority.session.factory.DefaultSecuritySessionFactory;
import com.oceanbase.odc.core.authority.session.manager.DefaultSecuritySessionManager;
import com.oceanbase.odc.core.authority.session.validate.ExpiredSecuritySessionValidator;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

/**
 * Test object for {@link DefaultSecuritySessionManager}
 *
 * @author yh263208
 * @date 2021-07-15 17:54
 * @since ODC_release_3.2.0
 */
public class DefaultSecuritySessionManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void startSession_useDefaultSecuritySessionManager_startSucceed() {
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        AtomicInteger counter = new AtomicInteger(0);
        manager.addListener(new EmptySecuritySessionEventListener() {
            @Override
            public void onCreateEventSucceed(SecuritySession session, Map<String, Object> context) {
                counter.incrementAndGet();
                throw new RuntimeException("Test Exception");
            }
        });
        SecuritySession session = startSession(manager, 5000);
        Assert.assertTrue(session instanceof DelegateSecuritySession);
        Assert.assertEquals(session.getHost(), "127.0.0.1");
        Assert.assertEquals(session.getTimeoutMillis(), 5000);
        Assert.assertEquals(counter.get(), 1);
    }

    @Test
    public void getSession_useDefaultSecuritySessionManager_getSucceed() throws InterruptedException {
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        AtomicInteger counter = new AtomicInteger(0);
        manager.addListener(new EmptySecuritySessionEventListener() {
            @Override
            public void onGetEventSucceed(Serializable id, SecuritySession session) {
                counter.incrementAndGet();
                throw new RuntimeException("Test Exception");
            }

            @Override
            public void onGetEventFailed(Serializable id, Throwable e) {}
        });
        SecuritySession session = startSession(manager, 5000);
        Thread.sleep(200);
        Date oldAccessTime = session.getLastAccessTime();
        SecuritySession session1 = manager.getSession(session.getId());
        Assert.assertEquals(session, session1);
        Assert.assertEquals(counter.get(), 1);
        Assert.assertTrue(oldAccessTime.before(session1.getLastAccessTime()));
    }

    @Test
    public void start_usingDefaultTimeout_timeoutEqualsSettings() {
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        Map<String, Object> context = new HashMap<>();
        context.putIfAbsent(SecurityConstants.CONTEXT_SESSION_HOST_KEY, "127.0.0.1");
        SecuritySession session = manager.start(context);
        Assert.assertEquals(session.getTimeoutMillis(), SecurityConstants.DEFAULT_SESSION_TIMEOUT_MILLIS);
    }

    @Test
    public void getSession_usingInvalidKey_getNull() {
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        Assert.assertNull(manager.getSession("aaa"));
    }

    @Test
    public void expire_expireASession_expireSucceed() {
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        AtomicInteger counter = new AtomicInteger(0);
        manager.addListener(new EmptySecuritySessionEventListener() {
            @Override
            public void onExpiredEventSucceed(Serializable id, SecuritySession session) {
                counter.incrementAndGet();
                throw new RuntimeException("Test Exception");
            }

            @Override
            public void onExpiredEventFailed(Serializable id, SecuritySession session, Throwable e) {}
        });
        SecuritySession session = startSession(manager, 5000);
        Assert.assertFalse(session.isExpired());
        session.expire();
        SecuritySession session1 = manager.getSession(session.getId());
        Assert.assertNull(session1);
        Assert.assertEquals(counter.get(), 1);
        Assert.assertTrue(session.isExpired());
    }

    @Test
    public void getSession_sessionExpired_getInvalidSession() throws InterruptedException {
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        SecuritySession session = startSession(manager, 10);
        Thread.sleep(100);
        Assert.assertNull(manager.getSession(session.getId()));
    }

    @Test
    public void getSession_sessionExpiredWithValidator_getNull() throws InterruptedException {
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        manager.addSessionValidator(new ExpiredSecuritySessionValidator());
        SecuritySession session = startSession(manager, 10);
        Thread.sleep(100);
        Assert.assertNull(manager.getSession(session.getId()));
    }

    @Test
    public void enableAsyncRefreshSessionManager_noValidator_expThrown() {
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Validators can not be empty");
        manager.enableAsyncRefreshSessionManager();
    }

    @Test
    public void enableAsyncRefreshSessionManager_withValidator_refreshSucceed() throws InterruptedException {
        // Init a SecuritySession object with 3 seconds and 5 seconds timeout settings
        DefaultSecuritySessionManager manager = new DefaultSecuritySessionManager(
                new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository());
        manager.addSessionValidator(new ExpiredSecuritySessionValidator());
        SecuritySession session_200 = startSession(manager, 200);
        SecuritySession session_500 = startSession(manager, 500);

        manager.enableAsyncRefreshSessionManager();
        Thread.sleep(370);
        Assert.assertNull(manager.getSession(session_200.getId()));

        SecuritySession session_500_1 = manager.getSession(session_500.getId());
        Assert.assertEquals(session_500, session_500_1);
    }

    private SecuritySession startSession(SecuritySessionManager manager, long timeout) {
        Map<String, Object> context = new HashMap<>();
        context.putIfAbsent(SecurityConstants.CONTEXT_SESSION_TIMEOUT_KEY, timeout);
        context.putIfAbsent(SecurityConstants.CONTEXT_SESSION_HOST_KEY, "127.0.0.1");
        return manager.start(context);
    }

}
