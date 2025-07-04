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
package com.oceanbase.odc.service.iam.auth.accesskey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.iam.AccessKeyService;

public class AccessKeyAuthenticationFilterTest {

    private AccessKeyAuthenticationFilter filter;
    private AccessKeyService accessKeyService;
    private UserRepository userRepository;

    @Before
    public void setUp() {
        accessKeyService = mock(AccessKeyService.class);
        userRepository = mock(UserRepository.class);
        filter = new AccessKeyAuthenticationFilter(accessKeyService, userRepository);
    }

    @Test
    public void testParseQueryParams_EmptyQueryString() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString(null);

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testParseQueryParams_SingleParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=value");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("value", params.get("name"));
    }

    @Test
    public void testParseQueryParams_MultipleParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=value&age=25&city=beijing");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("value", params.get("name"));
        assertEquals("25", params.get("age"));
        assertEquals("beijing", params.get("city"));
    }

    @Test
    public void testParseQueryParams_EmptyValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=&age=25");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals("", params.get("name"));
        assertEquals("25", params.get("age"));
    }

    @Test
    public void testParseQueryParams_NoValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name&age=25");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals("25", params.get("age"));
        assertEquals("", params.get("name"));
    }

    @Test
    public void testParseQueryParams_UrlEncoded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=John%20Doe&city=New%20York");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals("John Doe", params.get("name"));
        assertEquals("New York", params.get("city"));
    }

    @Test
    public void testParseQueryParams_SpecialCharacters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=test%40example.com&path=%2Fapi%2Fv1");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals("test@example.com", params.get("name"));
        assertEquals("/api/v1", params.get("path"));
    }

    @Test
    public void testParseQueryParams_ChineseCharacters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=%E5%BC%A0%E4%B8%89&city=%E5%8C%97%E4%BA%AC");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals("张三", params.get("name"));
        assertEquals("北京", params.get("city"));
    }

    @Test
    public void testParseQueryParams_DuplicateKeys() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=John&name=Jane&name=Bob");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());

        assertEquals("John,Jane,Bob", params.get("name"));
    }

    @Test
    public void testParseQueryParams_ComplexQuery() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("action=create&type=user&id=123&name=John%20Doe&email=john%40example.com&active=true");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(6, params.size());
        assertEquals("create", params.get("action"));
        assertEquals("user", params.get("type"));
        assertEquals("123", params.get("id"));
        assertEquals("John Doe", params.get("name"));
        assertEquals("john@example.com", params.get("email"));
        assertEquals("true", params.get("active"));
    }

    @Test
    public void testParseQueryParams_EmptyQueryStringWithQuestionMark() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testParseQueryParams_MalformedQuery() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=value&malformed&age=25");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("value", params.get("name"));
        assertEquals("25", params.get("age"));
    }

    @Test
    public void testParseQueryParams_OnlyEquals() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("=");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testParseQueryParams_MultipleEquals() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=value=extra");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("value=extra", params.get("name"));
    }

    @Test
    public void testParseQueryParams_Whitespace() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=value%20with%20spaces");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("value with spaces", params.get("name"));
    }

    @Test
    public void testParseQueryParams_PlusSign() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("name=value+with+plus");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("value with plus", params.get("name"));
    }

    @Test
    public void testParseQueryParams_ArrayParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("roles[]=admin&roles[]=user&roles[]=guest");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());

        assertEquals("admin,user,guest", params.get("roles[]"));
    }

    @Test
    public void testParseQueryParams_ArrayParametersWithIndex() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("roles[0]=admin&roles[1]=user&roles[2]=guest");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("admin", params.get("roles[0]"));
        assertEquals("user", params.get("roles[1]"));
        assertEquals("guest", params.get("roles[2]"));
    }

    @Test
    public void testParseQueryParams_NestedObjectParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("user[name]=John&user[email]=john@example.com&user[age]=30");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("John", params.get("user[name]"));
        assertEquals("john@example.com", params.get("user[email]"));
        assertEquals("30", params.get("user[age]"));
    }

    @Test
    public void testParseQueryParams_ComplexNestedStructure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString(
                "data[user][name]=John&data[user][email]=john@example.com&data[settings][theme]=dark&data[settings][lang]=en");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(4, params.size());
        assertEquals("John", params.get("data[user][name]"));
        assertEquals("john@example.com", params.get("data[user][email]"));
        assertEquals("dark", params.get("data[settings][theme]"));
        assertEquals("en", params.get("data[settings][lang]"));
    }

    @Test
    public void testParseQueryParams_MixedArrayAndObject() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString(
                "users[0][name]=John&users[0][email]=john@example.com&users[1][name]=Jane&users[1][email]=jane@example.com&roles[]=admin&roles[]=user");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(5, params.size());
        assertEquals("John", params.get("users[0][name]"));
        assertEquals("john@example.com", params.get("users[0][email]"));
        assertEquals("Jane", params.get("users[1][name]"));
        assertEquals("jane@example.com", params.get("users[1][email]"));
        assertEquals("admin,user", params.get("roles[]"));
    }

    @Test
    public void testParseQueryParams_ArrayWithSpecialCharacters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("tags[]=web%20development&tags[]=javascript&tags[]=node.js");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());

        assertEquals("web development,javascript,node.js", params.get("tags[]"));
    }

    @Test
    public void testParseQueryParams_ObjectWithSpecialCharacters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("config[api_url]=https%3A%2F%2Fapi.example.com&config[timeout]=30");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals("https://api.example.com", params.get("config[api_url]"));
        assertEquals("30", params.get("config[timeout]"));
    }

    @Test
    public void testParseQueryParams_EmptyArrayElements() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("items[]=item1&items[]=&items[]=item3");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("item1,,item3", params.get("items[]"));
    }

    @Test
    public void testParseQueryParams_EmptyObjectProperties() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("user[name]=John&user[email]=&user[age]=30");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("John", params.get("user[name]"));
        assertEquals("", params.get("user[email]"));
        assertEquals("30", params.get("user[age]"));
    }

    @Test
    public void testParseQueryParams_MalformedArray() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("items[=value1&items]=value2&items[0]=value3");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("value1", params.get("items["));
        assertEquals("value2", params.get("items]"));
        assertEquals("value3", params.get("items[0]"));
    }

    @Test
    public void testParseQueryParams_MalformedObject() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("user[name=John&user]email=jane@example.com&user[age]=30");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("John", params.get("user[name"));
        assertEquals("jane@example.com", params.get("user]email"));
        assertEquals("30", params.get("user[age]"));
    }

    @Test
    public void testParseQueryParams_UnclosedBrackets() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("items[0=value1&items[1=value2");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals("value1", params.get("items[0"));
        assertEquals("value2", params.get("items[1"));
    }

    @Test
    public void testParseQueryParams_DeepNesting() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("data[level1][level2][level3][value]=deep");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("deep", params.get("data[level1][level2][level3][value]"));
    }

    @Test
    public void testParseQueryParams_ComplexRealWorldExample() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString(
                "action=create&type=user&id=123&user[name]=John%20Doe&user[email]=john%40example.com&roles[]=admin&roles[]=user&settings[theme]=dark&settings[lang]=en&metadata[created_by]=system&metadata[version]=1.0");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(10, params.size());
        assertEquals("create", params.get("action"));
        assertEquals("user", params.get("type"));
        assertEquals("123", params.get("id"));
        assertEquals("John Doe", params.get("user[name]"));
        assertEquals("john@example.com", params.get("user[email]"));
        // 数组参数现在应该包含所有值，用逗号分隔
        assertEquals("admin,user", params.get("roles[]"));
        assertEquals("dark", params.get("settings[theme]"));
        assertEquals("en", params.get("settings[lang]"));
        assertEquals("system", params.get("metadata[created_by]"));
        assertEquals("1.0", params.get("metadata[version]"));
    }

    @Test
    public void testParseQueryParams_ChineseInComplexStructure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString(
                "user[name]=%E5%BC%A0%E4%B8%89&user[city]=%E5%8C%97%E4%BA%AC&roles[]=%E7%AE%A1%E7%90%86%E5%91%98&roles[]=%E7%94%A8%E6%88%B7");

        Map<String, String> params = filter.parseQueryParams(request);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("张三", params.get("user[name]"));
        assertEquals("北京", params.get("user[city]"));
        assertEquals("管理员,用户", params.get("roles[]"));
    }
}
