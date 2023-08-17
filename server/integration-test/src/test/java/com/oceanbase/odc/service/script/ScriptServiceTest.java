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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.script.ScriptMetaEntity;
import com.oceanbase.odc.metadb.script.ScriptMetaRepository;
import com.oceanbase.odc.service.common.FileChecker;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.operator.ObjectMetaOperator;
import com.oceanbase.odc.service.script.model.ScriptMeta;
import com.oceanbase.odc.service.script.model.ScriptProperties;

public class ScriptServiceTest extends ServiceTestEnv {
    private static final String BUCKET_NAME = "test-bucket";
    private static final String OBJECT_NAME = "some-file.tmp";
    private static final String OBJECT_ID = "object-id.tmp";
    private static final long OBJECT_LENGTH = 1000L;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Autowired
    private ScriptService scriptService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private ObjectStorageFacade objectStorageFacade;

    @MockBean
    private ScriptMetaRepository scriptMetaRepository;

    @Mock
    private ScriptProperties scriptProperties;

    @Mock
    private FileChecker fileChecker;

    @Mock
    private ObjectMetaOperator metaOperator;

    private final MultipartFile mockFile =
            new MockMultipartFile(OBJECT_NAME, OBJECT_NAME, null, new byte[(int) OBJECT_LENGTH]);

    @Before
    public void setUp() throws Exception {
        folder.newFolder(BUCKET_NAME);
        when(authenticationFacade.currentUserIdStr()).thenReturn("1");
        ObjectMetadata mockObjectMetadata = ObjectMetadata.builder()
                .bucketName(BUCKET_NAME.concat("/1"))
                .objectId(OBJECT_ID)
                .objectName(mockFile.getOriginalFilename())
                .build();
        ScriptMetaEntity mockScriptMeta = ScriptMetaEntity.builder()
                .id(1L)
                .objectName(OBJECT_NAME)
                .objectId(OBJECT_ID)
                .build();
        when(objectStorageFacade.putObject(anyString(), anyString(), anyLong(), any(InputStream.class)))
                .thenReturn(mockObjectMetadata);
        when(scriptMetaRepository.saveAndFlush(any(ScriptMetaEntity.class))).thenReturn(mockScriptMeta);
        when(scriptProperties.getMaxEditLength()).thenReturn(20 * 1024 * 1024L);
        when(scriptProperties.getMaxUploadLength()).thenReturn(250 * 1024 * 1024L);
        when(scriptMetaRepository.findByIdAndBucketName(anyLong(), anyString()))
                .thenReturn(Optional.of(mockScriptMeta));
        when(metaOperator.getObjectMeta(anyString(), anyString())).thenReturn(mockObjectMetadata);
    }

    @Test
    public void testBatchPutScript_Success() {
        scriptService.batchPutScript(Arrays.asList(mockFile));
        Mockito.verify(scriptMetaRepository, times(1)).saveAndFlush(any(ScriptMetaEntity.class));
    }

    @Test
    public void testSynchronizeScript() throws IOException {
        List<ScriptMeta> scriptMetaList = scriptService.batchPutScript(Arrays.asList(mockFile));
        ScriptMeta scriptMeta = scriptService.synchronizeScript(scriptMetaList.get(0).getId());
        Assert.assertEquals(OBJECT_NAME, scriptMeta.getObjectName());
    }
}
