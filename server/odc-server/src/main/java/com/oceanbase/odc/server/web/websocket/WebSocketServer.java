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
package com.oceanbase.odc.server.web.websocket;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.ShellUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.config.WebSocketEndpointConfigure;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.script.model.ScriptConstants;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.websocket.ClientProxy;
import com.oceanbase.odc.service.websocket.ConnectionConfigProvider;
import com.oceanbase.odc.service.websocket.OBClientProxy;
import com.oceanbase.odc.service.websocket.WebSocketBody;
import com.oceanbase.odc.service.websocket.WebSocketCustomEncoding;
import com.oceanbase.odc.service.websocket.WebSocketParams;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2020/12/16
 */

@Slf4j
@ServerEndpoint(value = "/api/v1/webSocket/obclient/{resourceId}", configurator = WebSocketEndpointConfigure.class,
        encoders = WebSocketCustomEncoding.class)
@Service
public class WebSocketServer {
    private static final String PING = "ping";
    private static final String STD_IN = "stdin";
    private static final String STD_OUT = "stdout";
    private static final String SET_GBK_COMMAND = "--init-command=\"set names gbk;\"";
    // default ping time out for a single websocket connection
    private static final int DEFAULT_PING_TIMEOUT_MILLIS = 5 * 60 * 1000;// 5min
    // record number of websocket connections
    private static final AtomicInteger onlineNum = new AtomicInteger();
    private static final ConcurrentHashMap<Session, OBClientProxy> connectionPool = new ConcurrentHashMap<>();

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ConnectSessionService sessionService;

    @Autowired
    private ConnectionConfigProvider connectionConfigProvider;

    @Value("${odc.server.obclient.command-black-list:}")
    private List<String> obclientCommandBlackList;
    /**
     * obclient可执行文件路径
     */
    private final String obclientFilePath;
    /**
     * 脚本存储空间
     */
    private final String baseScriptFilePath;
    /**
     * 线程池的最大大小
     */
    private final int maxPoolSize;
    private final ScheduledExecutorService scheduleExecutor;
    private final ThreadPoolExecutor proxyExecutor;

    @Autowired
    public WebSocketServer(@Value("${obclient.file.path:/opt/odc/obclient/bin/obclient}") String obclientFilePath,
            @Value("${odc.objectstorage.local.dir:#{systemProperties['user.home'].concat(T(java.io.File).separator).concat('data').concat"
                    + "(T(java.io.File).separator).concat('files')}}") String baseObjectStorageDir) {
        this.obclientFilePath = obclientFilePath;
        this.baseScriptFilePath =
                baseObjectStorageDir.concat(File.separator).concat(ScriptConstants.SCRIPT_BASE_BUCKET);
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("odc-web-socket-schedule-%d")
                .build();
        scheduleExecutor = new ScheduledThreadPoolExecutor(1, threadFactory);

        scheduleExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Session, OBClientProxy> entry : connectionPool.entrySet()) {
                    log.debug("last access time: {}", entry.getValue().getLastAccessTime());
                    if (!entry.getValue().isAlive()) {
                        // backend thread is dead
                        log.info("Close session for backend thread has been killed, session id: {}",
                                entry.getKey().getId());
                        closeSession(entry.getKey(), null);
                        continue;
                    }
                    if (System.currentTimeMillis()
                            - entry.getValue().getLastAccessTime() > DEFAULT_PING_TIMEOUT_MILLIS) {
                        // ping time out from frontend
                        log.info(
                                "close session for frontend ping has exceeded ping timeout, session id: {}, now {}, last time {}",
                                entry.getKey().getId(), System.currentTimeMillis(),
                                entry.getValue().getLastAccessTime());
                        closeSession(entry.getKey(), null);
                    }
                }
            }
        }, 60, 10, TimeUnit.SECONDS);// lower disconnect delay

        this.maxPoolSize = 4 * SystemUtils.availableProcessors();
        // thread control in threadpool
        // core pool size equals to max is for submiting new thread without blocking
        this.proxyExecutor = new ThreadPoolExecutor(maxPoolSize, maxPoolSize, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1),
                r -> new Thread(r, "Obclient_Read_Thread_" + r.hashCode()),
                new ThreadPoolExecutor.CallerRunsPolicy());
        log.info("odc web socket server initialized");
    }

    @PreDestroy
    public void destroy() {
        log.info("web socket server destroy...");
        closeAllProxy();
        ExecutorUtils.gracefulShutdown(proxyExecutor, "webSocketProxyExecutor", 5);
        ExecutorUtils.gracefulShutdown(scheduleExecutor, "webSocketScheduleExecutor", 5);
        log.info("web socket server destroyed");
    }

    private void closeAllProxy() {
        for (ClientProxy proxy : connectionPool.values()) {
            proxy.close();
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "resourceId") String resourceId) {
        addOnlineCount();
        try {
            log.info("obclient session initializing, resourceId={}, sessionId={}", resourceId, session.getId());
            OBClientProxy proxy = connectObClient(resourceId, session);
            connectionPool.put(session, proxy);
            log.info("obclient session initialized, resourceId={}, sessionId={}", resourceId, session.getId());
        } catch (Exception e) {
            log.error("Error occurs when connecting obclient, resourceId={}, ", resourceId, e);
            closeSession(session, new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, e.getMessage()));
        }
    }

    private List<String> getRunAsOtherUserCommand(String currentUserIdStr, String obclientCmd) {
        List<String> cmds = new ArrayList<>();
        cmds.add("su");
        cmds.add("-c");
        cmds.add(obclientCmd);
        cmds.add("-s");
        cmds.add("/bin/sh");
        cmds.add(getOsUserName(currentUserIdStr));
        return cmds;
    }

    private OBClientProxy connectObClient(String resourceId, Session session) {
        // GBK is not supported before OceanBase 1.4.79
        ConnectionConfig connectionConfig = connectionConfigProvider.getConnectionSession(resourceId, session);
        ConnectionSession connectionSession = sessionService.nullSafeGet(SidUtils.getSessionId(resourceId));
        boolean supportSetGBK = isSupportSetGBK(connectionSession);
        String schema = MoreObjects.firstNonNull(ConnectionSessionUtil.getCurrentSchema(connectionSession),
                connectionConfig.getDefaultSchema());

        String[] cmds = generateCmd(connectionConfig, supportSetGBK, schema);

        String userWorkDirectory = generateUserFolderPath(authenticationFacade.currentUserIdStr());
        log.debug("user work directory: {}", userWorkDirectory);

        OBClientProxy proxy = new OBClientProxy(this.proxyExecutor, t -> {
            WebSocketBody body = new WebSocketBody();
            body.setMethod(STD_OUT);
            body.setParams(new WebSocketParams(t));
            sendMessage(session, body);
        }, userWorkDirectory);
        // do not print password
        String[] commandToPrint = StringUtils.isBlank(connectionConfig.getPassword()) ? cmds
                : ArrayUtils.subarray(cmds, 0, cmds.length - 1);
        log.info("command to be execute: {}", String.join(" ", commandToPrint));
        proxy.connect(cmds);
        log.info("{} has established connection with obclient, current connection number is {}, session id: {}",
                getDbUser(connectionConfig), onlineNum, session.getId());
        return proxy;
    }

    private String[] generateCmd(ConnectionConfig connectionConfig, boolean supportSetGBK, String schema) {
        List<String> cmd;
        if (!SystemUtils.isOnLinux()) {
            cmd = getRunObClientCmd(connectionConfig, supportSetGBK, schema);
            return cmd.toArray(new String[cmd.size()]);
        }
        try {
            addOsUser(authenticationFacade.currentUserIdStr());
        } catch (IOException | InterruptedException ex) {
            log.warn("create os user failed, userId={}", authenticationFacade.currentUserIdStr());
            throw new RuntimeException("create os user failed, odcUserId=" + authenticationFacade.currentUserIdStr(),
                    ex);
        }
        List<String> obclientCmd = getRunObClientCmd(connectionConfig, false, schema);
        cmd = getRunAsOtherUserCommand(authenticationFacade.currentUserIdStr(), String.join(" ", obclientCmd));
        return cmd.toArray(new String[cmd.size()]);
    }


    private List<String> getRunObClientCmd(ConnectionConfig connectionConfig, boolean supportSetGBK, String schema) {
        List<String> obclientCmd = new ArrayList<>();
        obclientCmd.add(obclientFilePath);
        obclientCmd.add(String.format("-h%s", connectionConfig.getHost()));
        obclientCmd.add(String.format("-P%d", connectionConfig.getPort()));
        // 用户@租户#集群
        StringBuilder uOptionBuilder = new StringBuilder(getDbUser(connectionConfig));
        if (StringUtils.isNotBlank(connectionConfig.getTenantName())) {
            uOptionBuilder.append("@").append(connectionConfig.getTenantName());
        }
        if (StringUtils.isNotBlank(connectionConfig.getClusterName())) {
            uOptionBuilder.append("#").append(connectionConfig.getClusterName());
        }
        obclientCmd.add(String.format("-u%s", uOptionBuilder.toString()));
        obclientCmd.add(String.format("-D%s", quoteValue(schema, connectionConfig.getDialectType())));
        if (SystemUtils.getEnvOrProperty("sun.jnu.encoding").equalsIgnoreCase("gbk") && supportSetGBK) {
            obclientCmd.add("--init-command");
            obclientCmd.add("set names gbk");
        }
        if (CollectionUtils.isNotEmpty(obclientCommandBlackList)) {
            obclientCmd.add("--ob-disable_commands");
            obclientCmd.add(String.join(",", obclientCommandBlackList));
        }
        String password = connectionConfig.getPassword();
        if (StringUtils.isNotBlank(password)) {
            obclientCmd.add(String.format("-p%s", ShellUtils.escape(password)));
        }
        return obclientCmd;
    }

    private synchronized void addOsUser(String userId) throws IOException, InterruptedException {
        String osUserName = getOsUserName(userId);
        Process userExists = new ProcessBuilder()
                .command("grep", "-c", "^" + osUserName + ":", "/etc/passwd").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(userExists.getInputStream()))) {
            String line = reader.readLine();
            if (StringUtils.equals(line, "1")) {
                log.info("user {} already exists", osUserName);
                return;
            }
        }
        Process userAdd = new ProcessBuilder()
                .redirectErrorStream(true).command("bash", "-c", "useradd ".concat(osUserName)).start();
        if (userAdd.waitFor(10, TimeUnit.SECONDS)) {
            // exitValue() == 0 means add user successfully, exitValue() == 9 means user already exists, we
            // would return silently
            if (userAdd.exitValue() == 0 || userAdd.exitValue() == 9) {
                log.info("create os user successfully, name={}", osUserName);
                return;
            }
        }

        String output = IOUtils.toString(userAdd.getInputStream(), StandardCharsets.UTF_8);
        log.warn("create os user failed, exitValue={}, output={}", userAdd.exitValue(), output);
        throw new RuntimeException("create os user failed, exitVal=" + userAdd.exitValue() + ", output=" + output);
    }

    private String getDbUser(ConnectionConfig connectionConfig) {
        String dbUser = ConnectionSessionUtil.getUserOrSchemaString(connectionConfig.getUsername(),
                connectionConfig.getDialectType());
        return quoteValue(dbUser, connectionConfig.getDialectType());
    }

    private String quoteValue(String value, DialectType dialectType) {
        if (DialectType.OB_ORACLE.equals(dialectType)) {
            if (!SystemUtils.isOnLinux()) {
                // refer:
                // https://stackoverflow.com/questions/12124935/processbuilder-adds-extra-quotes-to-command-line
                // use \"schema\" but not "schema" to keep the original while using process builder
                value = "\\\"" + value + "\\\"";
            } else {
                value = "\"" + value + "\"";
            }
        }
        return value;
    }

    private boolean isSupportSetGBK(ConnectionSession connectionSession) {
        String version = ConnectionSessionUtil.getVersion(connectionSession);
        return VersionUtils.isGreaterThan(version, "1.4.79");
    }

    @OnClose
    public void onClose(Session session) {
        OBClientProxy proxy = connectionPool.remove(session);
        if (null != proxy) {
            proxy.close();
        }
        subOnlineCount();
        log.info("Terminate connection. Current connnection number is {}", onlineNum);
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        OBClientProxy proxy = connectionPool.get(session);
        if (Objects.isNull(proxy)) {
            log.warn("proxy not found by session, sessionId={}", session.getId());
            return;
        }
        WebSocketBody request = JsonUtils.fromJson(message, WebSocketBody.class);
        if (Objects.isNull(request)) {
            log.warn("Invalid web socket message, message={}", message);
            return;
        }
        if (STD_IN.equals(request.getMethod())) {
            try {
                proxy.write(request.getParams().getData());
            } catch (Exception e) {
                log.warn("Error occurs when write new command to obclient ", e);
            }
        } else {
            WebSocketBody body = new WebSocketBody();
            body.setId(request.getId());
            if (PING.equals(request.getMethod())) {
                body.setMethod(PING);
                proxy.setLastAccessTime(System.currentTimeMillis());
            }
            sendMessage(session, body);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.info("WebSocket sessionId:[{}] onError when keeping connection with obclient", session.getId(), throwable);
    }

    private void sendMessage(Session session, Object message) {
        if (session != null) {
            synchronized (session) {
                try {
                    session.getBasicRemote().sendObject(message);
                } catch (Exception e) {
                    log.error("Error occurs when send message back", e);
                }
            }
        }
    }

    private void closeSession(Session session, @Nullable CloseReason reason) {
        try {
            if (null != reason) {
                session.close(reason);
            } else {
                session.close();
            }
        } catch (Exception e) {
            log.error("Error occurs when closing session, session id: {}", session.getId(), e);
        }
    }

    private void addOnlineCount() {
        onlineNum.incrementAndGet();
    }

    private void subOnlineCount() {
        onlineNum.decrementAndGet();
    }

    public String generateUserFolderPath(String userId) {
        return baseScriptFilePath.concat(File.separator).concat(userId).concat(File.separator);
    }

    private String getOsUserName(String userId) {
        return "odcuser_".concat(userId);
    }
}
