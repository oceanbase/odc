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
package com.oceanbase.odc.service.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.script.model.ScriptMeta;

public class ScriptServiceWithLocalTest extends ServiceTestEnv {
    private static final String file1Name = "test.txt";
    private static final String file1Content = "hello world!";
    private static final String file2Name = "test2.txt";
    private static final String file2Content = "你好，世界！";
    @Autowired
    private ScriptService scriptService;
    @MockBean
    AuthenticationFacade authenticationFacade;

    @Before
    public void setUp() {
        when(authenticationFacade.currentUserIdStr()).thenReturn("1");
        when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
    }

    @After
    public void tearDown() {
        scriptService.tempPathsInBatchDownloadCache.invalidateAll();
    }

    @Test
    public void testBatchDownload() throws IOException {
        List<ScriptMeta> scriptMetas = scriptService.batchPutScript(
                Arrays.asList(createMultipartFile(file1Name, file1Content),
                        createMultipartFile(file2Name, file2Content)));
        assertEquals(2, scriptMetas.size());
        List<Long> scriptIds = scriptMetas.stream().map(ScriptMeta::getId).collect(Collectors.toList());
        ResponseEntity<InputStreamResource> entity = scriptService.batchDownload(scriptIds);
        assertNotNull(entity);
        String filename = Objects.requireNonNull(entity.getHeaders().getContentDisposition().getFilename());
        assertTrue(URLDecoder.decode(filename, StandardCharsets.UTF_8.toString()).startsWith(
                AuditEventAction.DOWNLOAD_SCRIPT.getLocalizedMessage()));
        assertTrue(filename.endsWith(".zip"));

        InputStreamResource body = entity.getBody();
        assertNotNull(body);
        Set<String> fileNamesInZip = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(body.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals(file1Name)) {
                    fileNamesInZip.add(name);
                    assertEquals(file1Content, IOUtils.toString(zipInputStream, StandardCharsets.UTF_8));
                } else if (name.equals(file2Name)) {
                    fileNamesInZip.add(name);
                    assertEquals(file2Content, IOUtils.toString(zipInputStream, StandardCharsets.UTF_8));
                }
            }
        }
        assertEquals(new HashSet<>(Arrays.asList(file1Name, file2Name)), fileNamesInZip);
    }

    private MultipartFile createMultipartFile(String fileName, String content) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        return new MockMultipartFile(fileName, fileName, "text/plain", inputStream);
    }

    private String readFullyAsString(InputStream inputStream, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);

            }
        }
        return sb.toString();
    }
}
