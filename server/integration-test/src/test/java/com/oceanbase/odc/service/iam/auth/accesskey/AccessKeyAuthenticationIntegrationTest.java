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
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.iam.AccessKeyEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.iam.AccessKeyService;
import com.oceanbase.odc.service.iam.auth.accesskey.AccessKeyRequestBuilder.RequestConfig;
import com.oceanbase.odc.service.iam.model.User;

import jakarta.servlet.ServletException;

public class AccessKeyAuthenticationIntegrationTest extends ServiceTestEnv {

    private static final String ACCESS_KEY_ID = "AK016CED73EDDF4DF9AD82EB6C29A48D14";
    private static final String SECRET_ACCESS_KEY = "420f65d22340552ca308f5de0534b2c5c3d3ff227618f8a61360e051a47ae626";
    private static final String HOST = "127.0.0.1:8989";
    private static final String PATH = "/api/v1/test";
    private static final Long USER_ID = 1L;
    private static final Long ORGANIZATION_ID = 1L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @MockBean
    private AccessKeyService accessKeyService;

    @MockBean
    private UserRepository userRepository;

    private AccessKeyAuthenticationFilter filter;

    @Before
    public void setUp() {
        filter = new AccessKeyAuthenticationFilter(accessKeyService, userRepository);

        AccessKeyEntity accessKeyEntity = new AccessKeyEntity();
        accessKeyEntity.setAccessKeyId(ACCESS_KEY_ID);
        accessKeyEntity.setUserId(USER_ID);

        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID);
        userEntity.setOrganizationId(ORGANIZATION_ID);
        userEntity.setName("test-user");

        when(accessKeyService.getDecryptAccessKey(ACCESS_KEY_ID)).thenReturn(SECRET_ACCESS_KEY);
        when(accessKeyService.getByAccessKeyId(ACCESS_KEY_ID)).thenReturn(Optional.of(accessKeyEntity));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetRequestAuthentication() throws ServletException, IOException {
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET).buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);

        User user = (User) authentication.getPrincipal();
        assertEquals(USER_ID, user.getId());
        assertEquals(ORGANIZATION_ID, user.getOrganizationId());
        assertEquals("test-user", user.getName());
    }

    @Test
    public void testGetRequestWithQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of("page", "1", "size", "10");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString("page=1&size=10");
        request.addParameter("page", "1");
        request.addParameter("size", "10");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testPostRequestAuthentication() throws ServletException, IOException {
        String requestBody = "{\"name\":\"test\",\"value\":\"data\"}";
        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withBody(requestBody.getBytes(StandardCharsets.UTF_8))
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, requestBody);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testPostRequestWithQueryParamsAuthentication() throws ServletException, IOException {
        String requestBody = "{\"name\":\"test\",\"value\":\"data\"}";
        Map<String, String> queryParams = Map.of("action", "create", "type", "user");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withBody(requestBody.getBytes(StandardCharsets.UTF_8))
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, requestBody);
        request.setQueryString("action=create&type=user");
        request.addParameter("action", "create");
        request.addParameter("type", "user");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testFileUploadRequestAuthentication() throws ServletException, IOException {
        File tempFile = temporaryFolder.newFile("test.txt");
        Files.write(tempFile.toPath(), "test content".getBytes());

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Content-Type", "multipart/form-data");

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withCustomHeaders(customHeaders)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, null);
        request.setContentType("multipart/form-data");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testBinaryUploadRequestAuthentication() throws ServletException, IOException {
        byte[] binaryData = "binary content".getBytes();

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Content-Type", "application/octet-stream");

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withCustomHeaders(customHeaders)
                        .withBody(binaryData)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, null);
        request.setContentType("application/octet-stream");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testImageUploadRequestAuthentication() throws ServletException, IOException {
        byte[] imageData = "fake image data".getBytes();

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Content-Type", "image/jpeg");

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withCustomHeaders(customHeaders)
                        .withBody(imageData)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, null);
        request.setContentType("image/jpeg");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testJsonRequestAuthentication() throws ServletException, IOException {
        String jsonBody = "{\"name\":\"test\",\"value\":\"data\"}";

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withBody(jsonBody.getBytes(StandardCharsets.UTF_8))
                        .withCustomHeaders(customHeaders)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, jsonBody);
        request.setContentType("application/json");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testXmlRequestAuthentication() throws ServletException, IOException {
        String xmlBody = "<root><name>test</name><value>data</value></root>";

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Content-Type", MediaType.APPLICATION_XML_VALUE);

        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withBody(xmlBody.getBytes(StandardCharsets.UTF_8))
                        .withCustomHeaders(customHeaders)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, xmlBody);
        request.setContentType("application/xml");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testFormDataRequestAuthentication() throws ServletException, IOException {
        String formData = "name=test&value=data";

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withBody(formData.getBytes(StandardCharsets.UTF_8))
                        .withCustomHeaders(customHeaders)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, formData);
        request.setContentType("application/x-www-form-urlencoded");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testPutRequestAuthentication() throws ServletException, IOException {
        String requestBody = "{\"name\":\"test\",\"value\":\"updated\"}";
        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.PUT)
                        .withBody(requestBody.getBytes(StandardCharsets.UTF_8))
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.PUT.name(), PATH, requestBody);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testPutRequestWithQueryParamsAuthentication() throws ServletException, IOException {
        String requestBody = "{\"name\":\"test\",\"value\":\"updated\"}";
        Map<String, String> queryParams = Map.of("id", "123", "version", "2");
        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.PUT)
                        .withBody(requestBody.getBytes(StandardCharsets.UTF_8))
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.PUT.name(), PATH, requestBody);
        request.setQueryString("id=123&version=2");
        request.addParameter("id", "123");
        request.addParameter("version", "2");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testPatchRequestAuthentication() throws ServletException, IOException {
        String requestBody = "{\"value\":\"patched\"}";
        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.PATCH)
                        .withBody(requestBody.getBytes(StandardCharsets.UTF_8))
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.PATCH.name(), PATH, requestBody);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testDeleteRequestAuthentication() throws ServletException, IOException {
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.DELETE).buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.DELETE.name(), PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testDeleteRequestWithQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of("id", "123", "force", "true");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.DELETE)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.DELETE.name(), PATH, null);
        request.setQueryString("id=123&force=true");
        request.addParameter("id", "123");
        request.addParameter("force", "true");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testHeadRequestAuthentication() throws ServletException, IOException {
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.HEAD).buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.HEAD.name(), PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testOptionsRequestAuthentication() throws ServletException, IOException {
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.OPTIONS).buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.OPTIONS.name(), PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testDifferentAlgorithmAuthentication() throws ServletException, IOException {
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withAlgorithm("HMACSHA256")
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testCustomRequestAuthentication() throws ServletException, IOException {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Custom-Header", "custom-value");
        customHeaders.put("Content-Type", "application/json");

        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withBody("custom body".getBytes(StandardCharsets.UTF_8))
                        .withCustomHeaders(customHeaders)
                        .withAlgorithm("HMACSHA256")
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, "custom body");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testRequestConfigBuilderAuthentication() throws ServletException, IOException {
        AccessKeyRequestBuilder.RequestConfig config = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST);
        config.withBody("{\"test\":\"data\"}".getBytes(StandardCharsets.UTF_8));
        config.withQueryParams(Map.of("param", "value"));
        HttpHeaders headers = config.buildHeaders();

        MockHttpServletRequest request =
                createMockRequest(headers, HttpMethod.POST.name(), PATH, "{\"test\":\"data\"}");
        request.setQueryString("param=value");
        request.addParameter("param", "value");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testInvalidAccessKeyId() throws ServletException, IOException {
        when(accessKeyService.getDecryptAccessKey("INVALID_KEY")).thenReturn(null);

        thrown.expect(BadCredentialsException.class);
        thrown.expectMessage("Invalid AccessKey ID");

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                "INVALID_KEY", SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET).buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    public void testInvalidSignature() throws ServletException, IOException {
        thrown.expect(BadCredentialsException.class);
        thrown.expectMessage("Invalid signature");

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, "wrong-secret-key", HOST, PATH, HttpMethod.GET).buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    public void testMissingAuthorizationHeader() throws ServletException, IOException {
        thrown.expect(BadCredentialsException.class);
        thrown.expectMessage("Invalid Authorization header format");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(HttpMethod.GET.name());
        request.setRequestURI(PATH);
        request.setServerName(HOST);
        request.setServerPort(80);
        request.addHeader("Authorization", "ODC-ACCESS-KEY-HMACSHA256 ");
        request.addHeader("Date", "Wed, 21 Oct 2015 07:28:00 GMT");
        request.addHeader("x-odc-nonce", "test-nonce");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    public void testMissingDateHeader() throws ServletException, IOException {
        thrown.expect(BadCredentialsException.class);
        thrown.expectMessage("Missing Date or x-odc-date header");

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET).buildHeaders();
        headers.remove("Date");

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    public void testFormSubmissionWithQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> formData = Map.of("username", "testuser", "email", "test@example.com");
        String formDataString = formData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        Map<String, String> queryParams = Map.of("action", "create", "type", "user");

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withBody(formDataString.getBytes(StandardCharsets.UTF_8))
                        .withQueryParams(queryParams)
                        .withCustomHeaders(customHeaders)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, formDataString);
        request.setContentType("application/x-www-form-urlencoded");
        request.setQueryString("action=create&type=user");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testQueryParamsOnlyAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of("page", "1", "size", "10", "filter", "active");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString("page=1&size=10&filter=active");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testComplexFormSubmissionAuthentication() throws ServletException, IOException {
        String formData =
                "user[name]=John&user[email]=john@example.com&roles[]=admin&roles[]=user&settings[theme]=dark&settings[lang]=en&extra_field=should_affect_signature";
        Map<String, String> queryParams = Map.of("action", "update", "id", "123", "version", "2");

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        HttpHeaders headers = new RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.POST)
                        .withBody(formData.getBytes(StandardCharsets.UTF_8))
                        .withQueryParams(queryParams)
                        .withCustomHeaders(customHeaders)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.POST.name(), PATH, formData);
        request.setContentType("application/x-www-form-urlencoded");
        request.setQueryString("action=update&id=123&version=2");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testUrlEncodedQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "search", "test query",
                "filter", "status=active&type=user",
                "sort", "name asc");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString("search=test+query&filter=status%3Dactive%26type%3Duser&sort=name+asc");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testArrayQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "roles[]", "admin,user,guest",
                "tags[]", "web,development",
                "categories[]", "tech");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString(
                "roles[]=admin&roles[]=user&roles[]=guest&tags[]=web&tags[]=development&categories[]=tech");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testIndexedArrayQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "users[0]", "john",
                "users[1]", "jane",
                "users[2]", "bob",
                "scores[0]", "95",
                "scores[1]", "87");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString("users[0]=john&users[1]=jane&users[2]=bob&scores[0]=95&scores[1]=87");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testNestedObjectQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "user[name]", "John",
                "user[email]", "john@example.com",
                "user[age]", "30",
                "config[theme]", "dark",
                "config[lang]", "en");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString(
                "user[name]=John&user[email]=john@example.com&user[age]=30&config[theme]=dark&config[lang]=en");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testComplexNestedQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "data[user][name]", "John",
                "data[user][email]", "john@example.com",
                "data[settings][theme]", "dark",
                "data[settings][lang]", "en",
                "metadata[created_by]", "system",
                "metadata[version]", "1.0");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString(
                "data[user][name]=John&data[user][email]=john@example.com&data[settings][theme]=dark&data[settings][lang]=en&metadata[created_by]=system&metadata[version]=1.0");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testMixedArrayAndObjectQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "users[0][name]", "John",
                "users[0][email]", "john@example.com",
                "users[1][name]", "Jane",
                "users[1][email]", "jane@example.com",
                "roles[]", "user");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString(
                "users[0][name]=John&users[0][email]=john@example.com&users[1][name]=Jane&users[1][email]=jane@example.com&roles[]=user");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testChineseQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "name", "张三",
                "city", "北京",
                "role", "管理员");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString("name=%E5%BC%A0%E4%B8%89&city=%E5%8C%97%E4%BA%AC&role=%E7%AE%A1%E7%90%86%E5%91%98");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testChineseInComplexStructureAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user[name]", "张三");
        queryParams.put("user[city]", "北京");
        queryParams.put("roles[]", "管理员,用户"); // 多个值用逗号分隔

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString(
                "user[name]=%E5%BC%A0%E4%B8%89&user[city]=%E5%8C%97%E4%BA%AC&roles[]=%E7%AE%A1%E7%90%86%E5%91%98&roles[]=%E7%94%A8%E6%88%B7");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testSpecialCharactersQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "api_url", "https://api.example.com",
                "timeout", "30",
                "config[api_url]", "https://api.example.com");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString(
                "api_url=https%3A%2F%2Fapi.example.com&timeout=30&config[api_url]=https%3A%2F%2Fapi.example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testEmptyAndNullQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "empty", "",
                "null_value", "",
                "valid", "test");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString("empty=&null_value=&valid=test");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testMalformedQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = Map.of(
                "items[", "value1",
                "items]", "value2",
                "items[0]", "value3");
        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString("items[=value1&items]=value2&items[0]=value3");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testComplexRealWorldQueryParamsAuthentication() throws ServletException, IOException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("action", "create");
        queryParams.put("type", "user");
        queryParams.put("id", "123");
        queryParams.put("user[name]", "John Doe");
        queryParams.put("user[email]", "john@example.com");
        queryParams.put("roles[]", "admin,user");
        queryParams.put("settings[theme]", "dark");
        queryParams.put("settings[lang]", "en");
        queryParams.put("metadata[created_by]", "system");
        queryParams.put("metadata[version]", "1.0");
        queryParams.put("api_url", "https://api.example.com");
        queryParams.put("timeout", "30");

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString(
                "action=create&type=user&id=123&user[name]=John%20Doe&user[email]=john%40example.com&roles[]=admin&roles[]=user&settings[theme]=dark&settings[lang]=en&metadata[created_by]=system&metadata[version]=1.0&api_url=https%3A%2F%2Fapi.example.com&timeout=30");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof User);
    }

    @Test
    public void testArrayQueryParamsAuthenticationWithArrayParams() throws ServletException, IOException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("roles[]", "admin,user,guest"); // 多个值用逗号分隔
        queryParams.put("type", "user");

        HttpHeaders headers = new AccessKeyRequestBuilder.RequestConfig(
                ACCESS_KEY_ID, SECRET_ACCESS_KEY, HOST, PATH, HttpMethod.GET)
                        .withQueryParams(queryParams)
                        .buildHeaders();

        String queryString = "roles[]=admin&roles[]=user&roles[]=guest&type=user";
        MockHttpServletRequest request = createMockRequest(headers, HttpMethod.GET.name(), PATH, null);
        request.setQueryString(queryString);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            // 验签通过，返回200
            ((MockHttpServletResponse) res).setStatus(200);
        });
        assertEquals(200, response.getStatus());
    }


    private MockHttpServletRequest createMockRequest(HttpHeaders headers, String method, String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(path);
        request.setServerName(HOST);
        request.setServerPort(8989);

        headers.forEach((key, values) -> {
            for (String value : values) {
                request.addHeader(key, value);
            }
        });

        if (body != null && !body.isEmpty()) {
            request.setContent(body.getBytes());
            request.setContentType("application/json");
        }

        return request;
    }
}
