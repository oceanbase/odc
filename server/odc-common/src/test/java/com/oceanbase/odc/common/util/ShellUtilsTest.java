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
package com.oceanbase.odc.common.util;

import static com.oceanbase.odc.common.util.ShellUtils.escapeOnLinux;
import static com.oceanbase.odc.common.util.ShellUtils.escapeOnWindows;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ShellUtilsTest {
    @Parameter(0)
    public String input;
    @Parameter(1)
    public String expectedForLinux;
    @Parameter(2)
    public String expectedForWindows;

    @Parameters(name = "{index}: var[{0}]={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // test for special characters
                {"hello", "hello", "hello"},
                {"passw$rd", "passw\\$rd", "passw$rd"},
                {"pass\\word", "pass\\\\word", "pass\\word"},
                {"pass\"word\"", "pass\\\"word\\\"", "pass\\\"word\\\""},
                {"pass'word'", "pass\\'word\\'", "pass'word'"},
                {"pass<word>", "pass\\<word\\>", "pass\"<word>"},
                // test for double-quote
                {"BGP<>|^", "BGP\\<\\>\\|^", "BGP\"<>|^"},
                {"B\"GP<>|^", "B\\\"GP\\<\\>\\|^", "B\\\"GP<>|^"},
                {"B\"G\"P<>|^", "B\\\"G\\\"P\\<\\>\\|^", "B\\\"G\\\"P\"<>|^"},
                {"B\"G\"P\"<>|^", "B\\\"G\\\"P\\\"\\<\\>\\|^", "B\\\"G\\\"P\\\"<>|^"},
                {"B\"G\"P\"\"<>|^", "B\\\"G\\\"P\\\"\\\"\\<\\>\\|^", "B\\\"G\\\"P\\\"\\\"\"<>|^"},
                // test for blank-space
                {"hello world", "hello\\ world", "hello\" world"},
                {"hello world!", "hello\\ world\\!", "hello\" world!"},
                {"\"test \"&test >test", "\\\"test\\ \\\"\\&test\\ \\>test", "\\\"test\" \\\"&test >test"}
        });
    }

    @Test
    public void test_EscapeOnLinux() {
        assertEquals(expectedForLinux, escapeOnLinux(input));
    }

    @Test
    public void test_EscapeOnWindows() {
        assertEquals(expectedForWindows, escapeOnWindows(input));
    }

}
