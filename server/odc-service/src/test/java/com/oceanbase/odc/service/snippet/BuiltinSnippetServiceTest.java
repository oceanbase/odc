/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.snippet;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.ConnectType;

public class BuiltinSnippetServiceTest {
    BuiltinSnippetService service = new BuiltinSnippetService();

    @Before
    public void setUp() throws Exception {
        service.init();
    }

    @Test
    public void listAll() {
        List<BuiltinSnippet> builtinSnippets = service.listAll();
        Assert.assertTrue(builtinSnippets.size() > 0);
    }

    @Test
    public void listByConnectType() {
        List<BuiltinSnippet> builtinSnippets = service.listByConnectType(ConnectType.OB_ORACLE);
        Assert.assertEquals(1, builtinSnippets.size());
    }
}
