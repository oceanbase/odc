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
package com.oceanbase.odc.service.integration.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.integration.IntegrationTestUtil;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.Body;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.BodyType;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.HttpProperties;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.RequestMethod;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties.Api;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties.CheckProperties;

/**
 * @author gaoda.xy
 * @date 2023/3/30 19:55
 */
public class SqlExecuteInterceptorPropertiesTest {

    @Test
    public void test_getApprovalProperties() {
        SqlInterceptorProperties properties =
                SqlInterceptorProperties.from(IntegrationTestUtil.createSqlInterceptorConfig());
        Assert.assertEquals(getExceptSqlInterceptorProperties(), properties);
    }

    private SqlInterceptorProperties getExceptSqlInterceptorProperties() {
        CheckProperties check = new CheckProperties();
        check.setMethod(RequestMethod.POST);
        check.setUrl("http://localhost:18989/check");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json");
        check.setHeaders(headers);
        Body body = new Body();
        body.setType(BodyType.RAW);
        body.setContent("{\"sql\":\"${sql_content}\"}");
        check.setBody(body);
        check.setRequestSuccessExpression("[resultCode]==0");
        check.setInWhiteListExpression("[checkResult]>=3");
        check.setInBlackListExpression("[checkResult]==1");
        check.setNeedReviewExpression("[checkResult]==2");
        Api api = new Api();
        api.setCheck(check);
        HttpProperties http = new HttpProperties();
        http.setConnectTimeoutSeconds(5);
        http.setSocketTimeoutSeconds(30);
        SqlInterceptorProperties properties = new SqlInterceptorProperties();
        properties.setApi(api);
        properties.setEncryption(Encryption.empty());
        properties.setHttp(http);
        return properties;
    }

}
