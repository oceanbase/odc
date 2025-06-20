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
package com.oceanbase.odc.service.intergation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.integration.ExternalSqlInterceptor;
import com.oceanbase.odc.service.integration.HttpOperationService;
import com.oceanbase.odc.service.integration.HttpOperationService.IntegrationConfigProperties;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.client.SqlInterceptorClient;
import com.oceanbase.odc.service.integration.model.Encryption;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.SqlConsoleRuleService;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.task.net.HttpServerContainer;
import com.oceanbase.odc.service.task.net.RequestHandler;
import com.oceanbase.odc.service.task.supervisor.PortDetector;

import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2025/6/19 17:25
 */
@Slf4j
public class ExternalSqlInterceptorTest {

    private static final String CONFIG_TEMPLATE = "http:\n"
            + "  connectTimeoutSeconds: 5\n"
            + "  socketTimeoutSeconds: 30\n"
            + "api:\n"
            + "  check:\n"
            + "    method: POST\n"
            + "    url: http://127.0.0.1:${port}/c/platformApi/checkControlSensitiveOdc\n"
            + "    headers:\n"
            + "      Content-Type: application/json;charset=UTF-8\n"
            + "      Accept: application/json\n"
            + "    body:\n"
            + "      type: RAW\n"
            + "      content: |-\n"
            + "        {\n"
            + "            \"sqlStatement\": \"${sql.content}\", \"accountName\":\"${connection.properties.accountName}\", \"collectionIds\":\"${connection.properties.collectionIds}\",\"dbInstance\":\"${connection.properties.dbInstance}\",\"instanceType\":\"${connection.properties.instanceType}\",\"ipAddress\":\"${connection.properties.ipAddress}\",\"operateSessionId\":\"${connection.properties.operateSessionId}\",\"reservedFields1\":\"${connection.properties.reservedFields1}\", \"reservedFields2\":\"${connection.properties.reservedFields2}\", \"reservedFields3\":\"${connection.properties.reservedFields3}\", \"serviceName\":\"${connection.properties.serviceName}\", \"ssoType\":\"${connection.properties.ssoType}\", \"userCode\":\"${connection.properties.userCode}\"\n"
            + "        }\n"
            + "    callback:\n"
            + "      onNeedReview:\n"
            + "        method: POST\n"
            + "        url: http://127.0.0.1:${port}/c/platformApi/cancel\n"
            + "        headers:\n"
            + "          Content-Type: application/json;charset=UTF-8\n"
            + "          Accept: application/json\n"
            + "        body:\n"
            + "          type: RAW\n"
            + "          content: |-\n"
            + "            {\n"
            + "              \"uuid\": \"${external.response.key1}\"\n"
            + "            }\n"
            + "        requestEncrypted: false\n"
            + "        requestSuccessExpression: '[checkResult] == \"2\"'\n"
            + "        responseEncrypted: false\n"
            + "      onInWhiteList:\n"
            + "        method: POST\n"
            + "        url: http://127.0.0.1:${port}/c/platformApi/cancel\n"
            + "        headers:\n"
            + "          Content-Type: application/json;charset=UTF-8\n"
            + "          Accept: application/json\n"
            + "        body:\n"
            + "          type: RAW\n"
            + "          content: |-\n"
            + "            {\n"
            + "               \"uuid\": \"${key1}\"\n"
            + "            }\n"
            + "      responseExtractExpressions:\n"
            + "        key1: '[uuid]'\n"
            + "        key2: '[vala]'\n"
            + "    requestEncrypted: false\n"
            + "    requestSuccessExpression: '[resultCode] == \"0\"'\n"
            + "    responseEncrypted: false\n"
            + "    inWhiteListExpression: '[checkResult] == \"1\"'\n"
            + "    inBlackListExpression: '[checkResult] == \"4\"'\n"
            + "    needReviewExpression: '[checkResult] == \"2\"'\n";

    private int port = PortDetector.getInstance().getPort();
    private MockHttpServer mockHttpServer;
    private String config;
    private SqlInterceptorProperties sqlInterceptorProperties;
    private ExternalSqlInterceptor externalSqlInterceptor;
    private SqlInterceptorClient sqlInterceptorClient;
    private HttpOperationService httpOperationService;
    private ConnectionSession connectionSession;

    @Before
    public void before() {
        mockHttpServer = new MockHttpServer(port);;
        mockHttpServer.start();
        config = CONFIG_TEMPLATE.replace("${port}", String.valueOf(port));
        sqlInterceptorProperties = YamlUtils.from(config, SqlInterceptorProperties.class);
        sqlInterceptorProperties.setEncryption(Encryption.empty());
        externalSqlInterceptor = new ExternalSqlInterceptor();
        // mock httpOperationService
        httpOperationService = new HttpOperationService();
        IntegrationConfigProperties integrationConfigProperties = new IntegrationConfigProperties();
        setField(httpOperationService, "configProperties", integrationConfigProperties);
        // mock sql interceptor client
        sqlInterceptorClient = new SqlInterceptorClient();
        setField(sqlInterceptorClient, "httpService", httpOperationService);
        sqlInterceptorClient.init();
        setField(externalSqlInterceptor, "sqlInterceptorClient", sqlInterceptorClient);
        // mock integrationService
        IntegrationService integrationService = Mockito.mock(IntegrationService.class);
        IntegrationEntity integrationEntity = new IntegrationEntity();
        integrationEntity.setId(11L);
        Mockito.when(integrationService.findIntegrationById(Mockito.anyLong()))
                .thenReturn(Optional.of(integrationEntity));
        Mockito.when(integrationService.getIntegrationProperties(Mockito.anyLong()))
                .thenReturn(sqlInterceptorProperties);
        setField(externalSqlInterceptor, "integrationService", integrationService);
        // mock authenticationFacade
        AuthenticationFacade authenticationFacade = Mockito.mock(AuthenticationFacade.class);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(1L);
        User user = new User();
        user.setId(1024L);
        user.setOrganizationType(OrganizationType.TEAM);
        Mockito.when(authenticationFacade.currentUser()).thenReturn(user);
        Mockito.when(authenticationFacade.currentUsername()).thenReturn("test");
        Mockito.when(authenticationFacade.currentUserAccountName()).thenReturn("testAccount");
        setField(externalSqlInterceptor, "authenticationFacade", authenticationFacade);
        // mock systemConfigService
        SystemConfigService systemConfigService = Mockito.mock(SystemConfigService.class);
        Configuration configuration = new Configuration("odc_url", "127.0.0.1:8888");
        Mockito.when(systemConfigService.queryByKeyPrefix(Mockito.anyString()))
                .thenReturn(Arrays.asList(configuration));
        setField(externalSqlInterceptor, "systemConfigService", systemConfigService);
        // mock ruleService
        RuleService ruleService = Mockito.mock(RuleService.class);
        Mockito.when(ruleService.getByRulesetIdAndName(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(Optional.empty());
        setField(externalSqlInterceptor, "ruleService", ruleService);
        // mock sqlConsoleRuleService
        SqlConsoleRuleService sqlConsoleRuleService = Mockito.mock(SqlConsoleRuleService.class);
        Mockito.when(
                sqlConsoleRuleService.getProperties(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(1));
        setField(externalSqlInterceptor, "sqlConsoleRuleService", sqlConsoleRuleService);
        // set connectionSession
        connectionSession = Mockito.mock(ConnectionSession.class);
        Mockito.when(connectionSession.getAttribute(ConnectionSessionConstants.RULE_SET_ID_NAME)).thenReturn(1L);
        SqlCommentProcessor sqlCommentProcessor = new SqlCommentProcessor(DialectType.OB_MYSQL, ";");
        Mockito.when(connectionSession.getAttribute(ConnectionSessionConstants.SQL_COMMENT_PROCESSOR_KEY))
                .thenReturn(sqlCommentProcessor);
        Mockito.when(connectionSession.getDialectType()).thenReturn(DialectType.OB_MYSQL);

    }

    @Test
    public void testSQLInterceptor() {
        SqlAsyncExecuteReq request = new SqlAsyncExecuteReq();
        request.setSql("select 1");
        SqlAsyncExecuteResp resp = new SqlAsyncExecuteResp(true);
        externalSqlInterceptor.doPreHandle(request, resp, connectionSession, Mockito.mock(AsyncExecuteContext.class));
        List<String> requestResult = mockHttpServer.accessInfo;
        Assert.assertEquals(requestResult.size(), 2);
        Assert.assertTrue(StringUtils.containsIgnoreCase(requestResult.get(1), "\"uuid\": \"myuid\""));
    }

    @After
    public void clear() throws Exception {
        if (null != mockHttpServer) {
            mockHttpServer.stop();
        }
    }

    private static void setField(Object object, String fieldName, Object value) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
            Assert.assertEquals(field.get(object), value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private static final class MockHttpServer extends HttpServerContainer<Map<String, String>> {
        private final int port;
        private List<String> accessInfo = new ArrayList<>();

        public MockHttpServer(int port) {
            this.port = port;
        }

        @Override
        protected int getPort() {
            return port;
        }

        @Override
        protected RequestHandler<Map<String, String>> getRequestHandler() {
            return new RequestHandler<Map<String, String>>() {
                @Override
                public Map<String, String> process(HttpMethod httpMethod, String uri, String requestData) {
                    log.info("receive request, uri: {}, requestData: {}", uri, requestData);
                    accessInfo.add(uri + "/" + requestData);
                    Map<String, String> ret = new HashMap<>();
                    ret.put("uuid", "myuid");
                    ret.put("resultCode", "0");

                    if (StringUtils.containsIgnoreCase(uri, "check")) {
                        ret.put("checkResult", "2");
                        ret.put("key1", "sssss");
                        ret.put("vala", "vvvvvvv");
                        ret.put("key3", "ggg");
                    }
                    return ret;
                }

                @Override
                public Map<String, String> processException(Throwable e) {
                    Map<String, String> ret = new HashMap<>();
                    ret.put("uuid", "myuid");
                    ret.put("resultCode", "0");
                    ret.put("checkResult", "4");
                    return ret;
                }
            };
        }

        @Override
        protected String getModuleName() {
            return "mock http server";
        }

        @Override
        protected Thread createThread(Runnable r) {
            return new Thread(r);
        }

        @Override
        protected Consumer<Integer> portConsumer() {
            return (port) -> {
            };
        }
    }
}
