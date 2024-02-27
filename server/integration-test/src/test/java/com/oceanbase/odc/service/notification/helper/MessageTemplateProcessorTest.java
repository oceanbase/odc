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
}
