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
package com.oceanbase.odc.core.session;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.core.session.tool.TestConnectionSessionFactory;
import com.oceanbase.odc.core.session.tool.TestSessionEventListener;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.execute.task.DefaultSqlExecuteTaskManager;
import com.oceanbase.odc.core.task.TaskManager;

import lombok.NonNull;

/**
 * Test case for {@link ConnectionSession}
 *
 * @author yh263208
 * @date 2021-11-16 11:13
 * @since ODC_release_3.2.2
 */
public class ConnectionSessionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void create_getDialectType_dialectMatched() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();

        ConnectType dialectType = ConnectType.OB_ORACLE;
        ConnectionSession connectionSession = sessionManager.start(getSessionConnectionFactory(dialectType, true));
        Assert.assertEquals(dialectType.getDialectType(), connectionSession.getDialectType());
    }

    @Test
    public void create_getDefaultAutoCommit_trueAutoCommit() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        Assert.assertTrue(connectionSession.getDefaultAutoCommit());
    }

    @Test
    public void create_getDefaultAutoCommit_falseAutoCommit() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, false));

        Assert.assertFalse(connectionSession.getDefaultAutoCommit());
    }

    @Test
    public void create_getTimeoutMillis_sessionTimeoutMatched() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        long expect =
                TimeUnit.MILLISECONDS.convert(ConnectionSessionConstants.SESSION_EXPIRATION_TIME_SECONDS,
                        TimeUnit.SECONDS);
        Assert.assertEquals(expect, connectionSession.getTimeoutMillis());
    }

    @Test
    public void create_setAttribute_getSucceed() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        String key = "test_key";
        Long value = System.currentTimeMillis();
        connectionSession.setAttribute(key, value);
        Assert.assertEquals(value, connectionSession.getAttribute(key));
        Assert.assertTrue(connectionSession.getAttributeKeys().contains(key));
    }

    @Test
    public void create_removeAttribute_getNull() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        String key = "test_key";
        Long value = System.currentTimeMillis();
        connectionSession.setAttribute(key, value);
        connectionSession.removeAttribute(key);
        Assert.assertNull(connectionSession.getAttribute(key));
    }

    @Test
    public void create_getSyncJdbcExecutor_executeSucceed() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        String actual = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .query("select 1+4 from dual", rs -> {
                    Verify.verify(rs.next(), "Result");
                    return rs.getString(1);
                });
        Assert.assertEquals("5", actual);
    }

    @Test
    public void create_getSession_gotSucceed() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        Assert.assertEquals(connectionSession, sessionManager.getSession(connectionSession.getId()));
    }

    @Test
    public void create_expire_gotNull() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        // test expire session
        connectionSession.expire();
        Assert.assertNull(sessionManager.getSession(connectionSession.getId()));
    }

    @Test
    public void touch_touchExpiredSession_catchException() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        connectionSession.expire();
        thrown.expectMessage(String.format("Session: %s invalid, lastAccessTime", connectionSession.getId()));
        thrown.expect(ExpiredSessionException.class);
        connectionSession.touch();
    }

    @Test
    public void getAttributeKeys_getAttriExpiredSession_catchException() {
        ConnectionSessionManager sessionManager = getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        connectionSession.expire();
        thrown.expectMessage(String.format("Session: %s invalid, lastAccessTime", connectionSession.getId()));
        thrown.expect(ExpiredSessionException.class);
        connectionSession.getAttributeKeys();
    }

    @Test
    public void onCreateSucceed_callWhenSessionCreated_callSucceed() {
        BaseConnectionSessionManager sessionManager = (BaseConnectionSessionManager) getConnectionSessionManager();
        Holder<ConnectionSession> holder = new Holder<>();
        TestSessionEventListener listener = new TestSessionEventListener() {
            @Override
            public void onCreateSucceed(ConnectionSession session) {
                holder.setValue(session);
                throw new RuntimeException("test");
            }
        };
        sessionManager.addListener(listener);

        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));
        Assert.assertEquals(holder.getValue().getId(), connectionSession.getId());
        listener.doAssert();
    }

    @Test
    public void onCreateFailed_callWhenSessionFailed_callSucceed() {
        long timestamp = System.currentTimeMillis();
        BaseConnectionSessionManager sessionManager =
                new DefaultConnectionSessionManager(getTaskManager(), new InMemorySessionRepository() {
                    @Override
                    public String store(@NonNull ConnectionSession session) {
                        throw new RuntimeException(timestamp + "");
                    }
                });
        Holder<Throwable> holder = new Holder<>();
        TestSessionEventListener listener = new TestSessionEventListener() {
            @Override
            public void onCreateFailed(ConnectionSession session, Throwable e) {
                holder.setValue(e);
            }
        };
        sessionManager.addListener(listener);

        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));
        Assert.assertNull(connectionSession);
        listener.doAssert();
        Assert.assertEquals(holder.getValue().getMessage(), timestamp + "");
    }

    @Test
    public void onDeleteSucceed_callWhenSessionDelete_callSucceed() {
        BaseConnectionSessionManager sessionManager = (BaseConnectionSessionManager) getConnectionSessionManager();
        Holder<ConnectionSession> holder = new Holder<>();
        TestSessionEventListener listener = new TestSessionEventListener() {
            @Override
            public void onCreateSucceed(ConnectionSession session) {
                // ignore
            }

            @Override
            public void onDeleteSucceed(ConnectionSession session) {
                holder.setValue(session);
            }

            @Override
            public void onExpireSucceed(ConnectionSession session) {
                // ignore
            }

            @Override
            public void onExpire(ConnectionSession session) {
                // ignore
            }
        };
        sessionManager.addListener(listener);

        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));

        connectionSession.expire();
        listener.doAssert();
        Assert.assertEquals(connectionSession.getId(), holder.getValue().getId());
    }

    @Test
    public void onDeleteFailed_callWhenSessionDeleteFailed_callSucceed() {
        long timestamp = System.currentTimeMillis();
        BaseConnectionSessionManager sessionManager =
                new DefaultConnectionSessionManager(getTaskManager(), new InMemorySessionRepository() {
                    @Override
                    public void delete(@NonNull ConnectionSession session) {
                        throw new RuntimeException(timestamp + "");
                    }
                });

        Holder<Throwable> holder = new Holder<>();
        TestSessionEventListener listener = new TestSessionEventListener() {
            @Override
            public void onCreateSucceed(ConnectionSession session) {
                // ignore
            }

            @Override
            public void onDeleteFailed(String id, Throwable e) {
                holder.setValue(e);
            }

            @Override
            public void onExpireSucceed(ConnectionSession session) {
                // ignore
            }

            @Override
            public void onExpire(ConnectionSession session) {
                // ignore
            }
        };
        sessionManager.addListener(listener);

        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));
        connectionSession.expire();
        listener.doAssert();
        Assert.assertEquals(timestamp + "", holder.getValue().getMessage());
    }

    @Test
    public void onGetSucceed_callWhenSessionGet_callSucceed() {
        BaseConnectionSessionManager sessionManager = (BaseConnectionSessionManager) getConnectionSessionManager();
        Holder<ConnectionSession> holder = new Holder<>();
        TestSessionEventListener listener = new TestSessionEventListener() {
            @Override
            public void onCreateSucceed(ConnectionSession session) {
                // ignore
            }

            @Override
            public void onGetSucceed(ConnectionSession session) {
                holder.setValue(session);
            }
        };
        sessionManager.addListener(listener);

        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));
        connectionSession = sessionManager.getSession(connectionSession.getId());
        Assert.assertEquals(holder.getValue().getId(), connectionSession.getId());
        listener.doAssert();
    }

    @Test
    public void onGetFailed_callWhenSessionGetFailed_callSucceed() {
        long timestamp = System.currentTimeMillis();
        BaseConnectionSessionManager sessionManager =
                new DefaultConnectionSessionManager(getTaskManager(), new InMemorySessionRepository() {
                    @Override
                    public ConnectionSession get(@NonNull String sessionId) {
                        throw new RuntimeException(timestamp + "");
                    }
                });
        Holder<Throwable> holder = new Holder<>();
        TestSessionEventListener listener = new TestSessionEventListener() {
            @Override
            public void onCreateSucceed(ConnectionSession session) {
                // ignore
            }

            @Override
            public void onGetFailed(String id, Throwable e) {
                holder.setValue(e);
            }
        };
        sessionManager.addListener(listener);

        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));
        connectionSession = sessionManager.getSession(connectionSession.getId());
        Assert.assertNull(connectionSession);
        listener.doAssert();
        Assert.assertEquals(holder.getValue().getMessage(), timestamp + "");
    }

    @Test
    public void getSession_canNotPassTheValidator_getNull() {
        BaseValidatedConnectionSessionManager sessionManager =
                (BaseValidatedConnectionSessionManager) getConnectionSessionManager();
        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));
        Assert.assertEquals(connectionSession, sessionManager.getSession(connectionSession.getId()));

        sessionManager.addSessionValidator(connectionSession1 -> true);
        sessionManager.addSessionValidator(session -> !connectionSession.getId().equals(session.getId()));

        Assert.assertNull(sessionManager.getSession(connectionSession.getId()));
    }

    @Test
    public void getSession_sessionRemovedByAsyncValidator_getNull() throws Exception {
        Holder<ConnectionSession> holder = new Holder<>();
        CountDownLatch latch = new CountDownLatch(1);
        BaseValidatedConnectionSessionManager sessionManager =
                new DefaultConnectionSessionManager(getTaskManager(), new InMemorySessionRepository() {
                    @Override
                    public void delete(@NonNull ConnectionSession session) {
                        super.delete(session);
                        holder.setValue(session);
                        latch.countDown();
                    }
                });
        sessionManager.addSessionValidator(s -> false);
        sessionManager.setScanInterval(100, TimeUnit.MILLISECONDS);

        TestConnectionSessionFactory factory = getSessionConnectionFactory(ConnectType.OB_ORACLE, true);
        ConnectionSession connectionSession = sessionManager.start(factory);

        sessionManager.enableAsyncRefreshSessionManager();
        Assert.assertNull(sessionManager.getSession(connectionSession.getId()));
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(connectionSession.getId(), holder.getValue().getId());
        sessionManager.close();
    }

    @Test
    public void removeSession_removeAllSession_getEmpty() throws Exception {
        ValidatedConnectionSessionManager sessionManager =
                (ValidatedConnectionSessionManager) getConnectionSessionManager();
        Assert.assertTrue(sessionManager.retrieveAllSessions().isEmpty());

        ConnectionSession connectionSession =
                sessionManager.start(getSessionConnectionFactory(ConnectType.OB_ORACLE, true));
        Assert.assertFalse(sessionManager.retrieveAllSessions().isEmpty());
        sessionManager.removeSession(connectionSession);

        Assert.assertTrue(sessionManager.retrieveAllSessions().isEmpty());
    }

    private TestConnectionSessionFactory getSessionConnectionFactory(ConnectType connectType,
            Boolean autoCommit) {
        return new TestConnectionSessionFactory(connectType, autoCommit);
    }

    private ConnectionSessionManager getConnectionSessionManager() {
        return new DefaultConnectionSessionManager(getTaskManager(), new InMemorySessionRepository());
    }

    private TaskManager getTaskManager() {
        return new DefaultSqlExecuteTaskManager(3, "sessionmanager", 10, TimeUnit.SECONDS);
    }

}
