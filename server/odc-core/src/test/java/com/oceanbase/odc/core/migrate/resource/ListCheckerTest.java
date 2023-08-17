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
package com.oceanbase.odc.core.migrate.resource;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.migrate.resource.checker.ListChecker;

/**
 * Test cases for {@link ListChecker}
 *
 * @author yh263208
 * @date 2022-06-23 21:23
 * @since ODC_release_3.3.2
 */
public class ListCheckerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void supports_illegalInput_returnFalse() {
        ListChecker checker = new ListChecker();
        Assert.assertFalse(checker.supports("asdasd"));
    }

    @Test
    public void supports_rightInput_returnTrue() {
        ListChecker checker = new ListChecker();
        Assert.assertTrue(checker.supports("[1,4]"));
        Assert.assertFalse(checker.supports("[1-4]"));
    }

    @Test
    public void contains_containsValue_returnTrue() {
        ListChecker checker = new ListChecker();
        Assert.assertTrue(checker.contains("[12,15]", 15));
    }

    @Test
    public void contains_notContainsValue_returnFalse() {
        ListChecker checker = new ListChecker();
        Assert.assertFalse(checker.contains("[12,15]", 16));
    }

    @Test
    public void contains_illegalInputString_expThrown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Input string is illegal, [12");
        ListChecker checker = new ListChecker();
        checker.contains("[12", "12");
    }

}
