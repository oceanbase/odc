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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PasswordChecker {

    /**
     * The special characters that are supported in the password.
     */
    private static final Set<Character> SUPPORTED_SPECIAL_CHARACTERS = new HashSet<>(
            Arrays.asList('~', '!', '@', '#', '%', '^', '&', '*', '_', '-', '+', '=', '|', '(', ')', '{', '}', '[', ']',
                    ':', ';', ',', '.', '?', '/', '$', '`', '\'', '"', '<', '>', '\\'));

    /**
     * Check if the password is legal.
     *
     * @param password the password to check
     * @return true if the password is legal, false otherwise
     */
    public static boolean checkPassword(String password) {
        if (password == null || password.length() > 32 || password.length() < 8) {
            return false;
        }
        char[] arr = password.toCharArray();
        int digitCount = 0, lowerCount = 0, upperCount = 0, specialCount = 0;
        boolean allCharLegal = true;
        for (char c : arr) {
            if (Character.isDigit(c)) {
                digitCount = 1;
            } else if (Character.isLowerCase(c)) {
                lowerCount = 1;
            } else if (Character.isUpperCase(c)) {
                upperCount = 1;
            } else if (SUPPORTED_SPECIAL_CHARACTERS.contains(c)) {
                specialCount = 1;
            } else {
                allCharLegal = false;
                break;
            }
        }
        return allCharLegal && (digitCount + lowerCount + upperCount + specialCount) >= 3;
    }

}
