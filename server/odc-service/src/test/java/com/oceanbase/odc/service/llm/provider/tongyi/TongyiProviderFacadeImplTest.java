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
package com.oceanbase.odc.service.llm.provider.tongyi;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.llm.model.LlmModel;
import com.oceanbase.odc.service.llm.model.LlmProvider;
import com.oceanbase.odc.service.llm.model.ModelType;
import com.oceanbase.odc.service.llm.provider.template.ProviderTemplate;

/**
 * Test cases for {@link TongyiProviderFacadeImpl}
 *
 * @author ODC_release_4.3.2
 */
public class TongyiProviderFacadeImplTest {

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_MASKED_API_KEY = "test-****key";
    private static final String TEST_SALT = "test-salt";
    private static final String TEST_ENCRYPTED_JSON = "encrypted-json";
    private static final String TEST_DECRYPTED_JSON =
            "{\"dashscope_api_key\":\"test-api-key\",\"max_tokens\":\"2048\",\"context_size\":\"8192\",\"function_calling_type\":\"auto\"}";
    private static final Long TEST_ORGANIZATION_ID = 1L;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_MODEL_NAME = "qwen-max";
    private static final Float TEST_TEMPERATURE = 0.7f;
    private static final Float TEST_TOP_P = 0.9f;
    private static final Integer TEST_SEED = 1234;
    private static final Float TEST_REPETITION_PENALTY = 1.1f;
    private static final Integer TEST_MAX_TOKENS = 2048;
    @Mock
    private EncryptionFacade encryptionFacade;
    @Mock
    private AuthenticationFacade authenticationFacade;
    @Mock
    private TextEncryptor textEncryptor;
    @InjectMocks
    private TongyiProviderFacadeImpl tongyiProviderFacade;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Mock common behaviors
        when(encryptionFacade.generateSalt()).thenReturn(TEST_SALT);
        when(encryptionFacade.organizationEncryptor(TEST_ORGANIZATION_ID, TEST_SALT)).thenReturn(textEncryptor);
        when(textEncryptor.encrypt(any())).thenReturn(TEST_ENCRYPTED_JSON);
        when(textEncryptor.decrypt(TEST_ENCRYPTED_JSON)).thenReturn(TEST_DECRYPTED_JSON);

        when(authenticationFacade.currentOrganizationId()).thenReturn(TEST_ORGANIZATION_ID);
        when(authenticationFacade.currentUserId()).thenReturn(TEST_USER_ID);
    }

    @Test
    public void getProviderTemplate_Always_ReturnsTemplate() {
        // when
        ProviderTemplate template = tongyiProviderFacade.getProviderTemplate();

        // then
        Assert.assertNotNull(template);
    }

    @Test
    public void generateModelsByProviderCredential_ValidCredential_ReturnsModels() {
        // given
        LlmProvider provider = createTestProvider();
        TongyiProviderCredential credential = new TongyiProviderCredential(TEST_API_KEY);

        // when
        List<TongyiModelCredential> models = tongyiProviderFacade.generateModelsByProviderCredential(credential);

        // then
        Assert.assertNotNull(models);
    }

    private LlmProvider createTestProvider() {
        LlmProvider provider = new LlmProvider();
        provider.setId(1L);
        provider.setName("TONGYI");
        provider.setOrganizationId(TEST_ORGANIZATION_ID);
        provider.setPropertiesJson(TEST_ENCRYPTED_JSON);
        provider.setSalt(TEST_SALT);
        return provider;
    }

    private LlmModel createTestModel() {
        LlmModel model = new LlmModel();
        model.setId(1L);
        model.setProviderName("TONGYI");
        model.setModelName("qwen-max");
        model.setModelType(ModelType.CHAT);
        model.setOrganizationId(TEST_ORGANIZATION_ID);
        model.setPropertiesJson(TEST_ENCRYPTED_JSON);
        model.setSalt(TEST_SALT);
        return model;
    }

    private TongyiModelCredential createTestModelCredential() {
        return TongyiModelCredential.builder()
                .modelName(TEST_MODEL_NAME)
                .modelType(ModelType.CHAT)
                .apiKey(TEST_API_KEY)
                .maxToken(TEST_MAX_TOKENS)
                .temperature(TEST_TEMPERATURE)
                .topP(TEST_TOP_P)
                .seed(TEST_SEED)
                .repetitionPenalty(TEST_REPETITION_PENALTY)
                .deprecated(false)
                .build();
    }
}
