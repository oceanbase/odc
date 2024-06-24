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
package com.oceanbase.odc.service.notification.helper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class MessageTemplateProcessorTest {

    @Test
    public void testReplaceVariables_EmptyVariables_ReturnTemplate() {
        String template = "fake template";
        String actual = MessageTemplateProcessor.replaceVariables(template, Locale.getDefault(), new HashMap<>());
        Assert.assertEquals(template, actual);
    }

    @Test
    public void testReplaceVariables_EmptyTemplate_ReturnEmptyString() {
        String actual = MessageTemplateProcessor.replaceVariables("", Locale.getDefault(), new HashMap<>());
        Assert.assertEquals("", actual);
    }

    @Test
    public void testReplaceVariables_Success() {
        String template = "this is a test template, name=${name}";
        Map<String, String> variables = new HashMap<>();
        variables.put("name", "fake name");

        String expected = "this is a test template, name=fake name";
        String actual = MessageTemplateProcessor.replaceVariables(template, Locale.getDefault(), variables);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReplaceVariables_VariableNotExists_ReplaceEmptyString() {
        String template = "name=${name}, project=${project}";

        Map<String, String> variables = new HashMap<>();
        variables.put("name", "fake name");

        String expected = "name=fake name, project=";
        String actual = MessageTemplateProcessor.replaceVariables(template, Locale.getDefault(), variables);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetLocalMessage_NullLocale_ReturnTemplate() {
        String template = "fake template";
        String message = MessageTemplateProcessor.getLocalMessage(null, template);
        Assert.assertEquals(template, message);
    }

    @Test
    public void testGetLocalMessage_VariousLocale_Success() {
        Locale locale1 = new Locale("en", "US");
        String template1 = "${com.oceanbase.odc.builtin-resource.collaboration.environment.dev.name}";
        String message1 = MessageTemplateProcessor.getLocalMessage(locale1, template1);
        Assert.assertEquals("dev", message1);
        Locale locale2 = new Locale("zh", "TW");
        String template2 = "${com.oceanbase.odc.builtin-resource.collaboration.environment.dev.name}";
        String message2 = MessageTemplateProcessor.getLocalMessage(locale2, template2);
        Assert.assertEquals("開發", message2);
        Locale locale3 = new Locale("zh", "CN");
        String template3 = "${com.oceanbase.odc.builtin-resource.collaboration.environment.dev.name}";
        String message3 = MessageTemplateProcessor.getLocalMessage(locale3, template3);
        Assert.assertEquals("开发", message3);
    }

    @Test
    public void testGetLocalMessage_NullTemplate_ReturnNull() {
        Locale locale = new Locale("zh", "CN");
        String template = null;
        String message = MessageTemplateProcessor.getLocalMessage(locale, template);
        Assert.assertEquals(null, message);
    }

}
