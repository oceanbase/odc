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
package com.oceanbase.odc.core.shared;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.i18n.I18nOutputSerializer;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.json.JacksonFactory;
import com.oceanbase.odc.common.json.JacksonModules;
import com.oceanbase.odc.common.json.JacksonModules.CustomOutputSerializer;
import com.oceanbase.odc.common.json.JacksonModules.SensitiveOutputSerializer;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.common.json.SensitiveOutput;

import lombok.Data;

public class JacksonModulesTest {
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        CustomOutputSerializer customOutputSerializer = new CustomOutputSerializer()
                .addSerializer(SensitiveOutput.class, new SensitiveOutputSerializer(t -> "******"))
                .addSerializer(Internationalizable.class, new I18nOutputSerializer());
        objectMapper = JacksonFactory.unsafeJsonMapper()
                .registerModule(JacksonModules.sensitiveInputHandling(t -> "somevalue"))
                .registerModule(JacksonModules.customOutputHandling(customOutputSerializer));
    }

    @Test
    public void sensitiveInputHandling_NoAnnotation_MatchOrigin() throws JsonProcessingException {
        String json = "{\"password\":\"123456\"}";

        T1 t = objectMapper.readValue(json, T1.class);

        Assert.assertEquals("123456", t.getPassword());
    }

    @Test
    public void sensitiveInputHandling_HasAnnotation_MatchHandler() throws JsonProcessingException {
        String json = "{\"password\":\"123456\"}";

        T2 t = objectMapper.readValue(json, T2.class);

        Assert.assertEquals("somevalue", t.getPassword());
    }

    @Test
    public void customOutputHandling_NoAnnotation_MatchOrigin() throws JsonProcessingException {
        T3 t = new T3();
        t.setPassword("123456");
        t.setMaskRule("${com.oceanbase.odc.ResourceType.ODC_FLOW_TASK_INSTANCE}");

        String content = objectMapper.writeValueAsString(t);

        Assert.assertEquals(
                "{\"maskRule\":\"${com.oceanbase.odc.ResourceType.ODC_FLOW_TASK_INSTANCE}\",\"password\":\"123456\"}",
                content);
    }

    @Test
    public void customOutputHandling_HasAnnotation_MatchHandler() throws JsonProcessingException {
        T4 t = new T4();
        t.setPassword("123456");
        t.setMaskRule("resource list: "
                + "${com.oceanbase.odc.ResourceType.ODC_FLOW_TASK_INSTANCE}, "
                + "${com.oceanbase.odc.ResourceType.ODC_AUDIT_EVENT}");
        t.setMaskRuleList(Collections.singletonList("${com.oceanbase.odc.ResourceType.ODC_AUDIT_EVENT}"));
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        String content = objectMapper.writeValueAsString(t);
        Assert.assertEquals(
                "{\"maskRule\":\"resource list: 任务节点, 审计事件\",\"maskRuleList\":[\"审计事件\"],\"password\":\"******\"}",
                content);
    }

    @Data
    private static class T1 {
        private String password;
    }

    @Data
    private static class T2 {
        @SensitiveInput
        private String password;
    }

    @Data
    private static class T3 {
        private String password;

        private String maskRule;
    }

    @Data
    private static class T4 {
        @SensitiveOutput
        private String password;

        @Internationalizable
        private String maskRule;

        @Internationalizable
        private List<String> maskRuleList;
    }

}
