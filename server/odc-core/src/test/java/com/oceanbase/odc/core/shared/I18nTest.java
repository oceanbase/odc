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

import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.context.NoSuchMessageException;

import com.oceanbase.odc.common.i18n.I18n;

public class I18nTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private Object[] args;

    @Test
    public void translate_HasMessage_Match() {
        String message =
                I18n.translate("com.oceanbase.odc.ErrorCodes.Success", (Object[]) null, Locale.SIMPLIFIED_CHINESE);
        Assert.assertEquals("成功", message);
    }

    @Test
    public void translate_LocaleMissMessage_UseDefault() {
        String message = I18n.translate("com.oceanbase.odc.ErrorCodes.Success", args, Locale.US);
        Assert.assertEquals("Success", message);
    }

    @Test
    public void translate_NoMessage_Exception() {
        thrown.expect(NoSuchMessageException.class);
        thrown.expectMessage("No message found");
        I18n.translate("Key.Not.Exists", (Object[]) null, Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    public void translate_NoMessageWithDefault_Default() {
        String message = I18n.translate("Key.Not.Exists", null, "DefaultMessage", Locale.SIMPLIFIED_CHINESE);
        Assert.assertEquals("DefaultMessage", message);
    }

    @Test
    public void testGetAllMessages() {
        Map<String, String> allMessages = I18n.getAllMessages(Locale.SIMPLIFIED_CHINESE);
        Assert.assertEquals("成功", allMessages.get("com.oceanbase.odc.ErrorCodes.Success"));
    }

    @Test
    public void translate_WithArgs() {
        String message = I18n.translate("com.oceanbase.odc.ErrorCodes.NotFound", new Object[] {"R1", "p1", "v1"},
                "DefaultMessage", Locale.US);
        Assert.assertEquals("R1 with identifier p1=v1 not found", message);
    }
}
