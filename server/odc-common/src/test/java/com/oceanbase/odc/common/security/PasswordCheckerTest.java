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
package com.oceanbase.odc.common.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordCheckerTest {

    @Test
    public void checkPassword() {
        // valid password
        assertTrue(PasswordChecker.checkPassword("aA123456"));
        assertTrue(PasswordChecker.checkPassword("aA-23456"));
        assertTrue(PasswordChecker.checkPassword("1A-24567"));
        assertTrue(PasswordChecker.checkPassword("aA-bcdef"));
        assertTrue(PasswordChecker.checkPassword("1A~!@#%^&*_-+=|(){}[]:;,.?/"));
        assertTrue(PasswordChecker.checkPassword("1A@#%^&*_-+=|(){}[]:;,.?/$`'\"<>\\"));
        assertTrue(PasswordChecker.checkPassword("1A~!@^&*_-+=|(){}[]:;,.?/$`'\"<>\\"));

        // length < 8 or > 32
        assertFalse(PasswordChecker.checkPassword("abc"));
        assertFalse(PasswordChecker.checkPassword("aA11"));
        assertTrue(PasswordChecker.checkPassword("aA012345678901234567890123456789"));
        assertFalse(PasswordChecker.checkPassword("aA0123456789012345678901234567890"));

        // character type count < 3
        assertFalse(PasswordChecker.checkPassword("1234567-"));

        // invalid character
        assertFalse(PasswordChecker.checkPassword("aaAA__11 "));
    }
}
