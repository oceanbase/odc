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
package com.oceanbase.odc.service.bastion;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.service.bastion.model.BastionAccount;
import com.oceanbase.odc.service.bastion.model.BastionProperties;
import com.oceanbase.odc.service.bastion.model.BastionProperties.AccountProperties;
import com.oceanbase.odc.service.bastion.model.BastionProperties.HttpProperties;
import com.oceanbase.odc.service.bastion.model.BastionProperties.QueryProperties;

@RunWith(MockitoJUnitRunner.class)
public class BastionAccountClientTest {
    private static final String TOKEN = "123456";

    @InjectMocks
    private BastionAccountClient accountClient;

    @Mock
    private BastionProperties bastionProperties;

    @Mock
    private BastionEncryptionService bastionEncryptionService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        QueryProperties queryProperties = new QueryProperties();
        queryProperties.setRequestMethod("POST");
        queryProperties.setRequestUrl("http://127.1/api/v1/accounts/me");
        queryProperties.setRequestHeaders(Collections.singletonList("H1=V1"));
        queryProperties.setRequestBody("{\"appCode\":\"app1\",\"token\":\"${account_verify_token}\"}");
        queryProperties.setResponseBodyValidExpression("['success'] == true");
        queryProperties.setResponseBodyUsernameExtractExpression("['data']['username']");
        queryProperties.setResponseBodyNickNameExtractExpression("['data']['nickName']");
        queryProperties.setRequestEncrypted(true);
        queryProperties.setResponseEncrypted(true);

        HttpProperties httpProperties = new HttpProperties();

        AccountProperties accountProperties = new AccountProperties();
        accountProperties.setQuery(queryProperties);
        accountProperties.setHttp(httpProperties);
        when(bastionProperties.getAccount()).thenReturn(accountProperties);

        when(bastionEncryptionService.encrypt(anyString()))
                .thenReturn("{\"appCode\":\"app1\",\"token\":\"${account_verify_token}\"}");
        when(bastionEncryptionService.decrypt(anyString()))
                .thenReturn("{\"success\":true,\"errorCode\":0,\"data\":{\"username\":\"user1\"}}");
    }

    @Test
    public void buildRequest_ReturnNotNull() {
        HttpUriRequest request = accountClient.buildRequest(TOKEN);
        Assert.assertNotNull(request);
    }

    @Test
    public void extractResponse_WithNickName_ReturnMatched() {
        String responseBody =
                "{\"success\":true,\"errorCode\":0,\"data\":{\"username\":\"user1\",\"nickName\":\"zhangsan\"}}";
        when(bastionEncryptionService.decrypt(anyString())).thenReturn(responseBody);

        BastionAccount expected = new BastionAccount();
        expected.setUsername("user1");
        expected.setNickName("zhangsan");

        BastionAccount bastionAccount = accountClient.extractResponse(responseBody);

        Assert.assertEquals(expected, bastionAccount);
    }

    @Test
    public void extractResponse_WithoutNickName_ReturnUsernameAsNickName() {
        BastionAccount expected = new BastionAccount();
        expected.setUsername("user1");
        expected.setNickName("user1");

        String responseBody =
                "{\"success\":true,\"errorCode\":0,\"data\":{\"username\":\"user1\"}}";
        BastionAccount bastionAccount = accountClient.extractResponse(responseBody);

        Assert.assertEquals(expected, bastionAccount);
    }
}
