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
package com.oceanbase.odc.core.datamasking.algorithm.pseudonymization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.core.datamasking.exception.CharNotInAlphabetException;
import com.oceanbase.odc.core.datamasking.exception.InvalidAlphabetException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/8/27
 */

@Slf4j
public class Alphabet {
    @Getter
    private final List<Character> symbols;
    private final Map<Character, Integer> mapForFasterAccess = new HashMap<>();

    public Alphabet(String charString) throws InvalidAlphabetException {
        String[] chars = charString.split(",");
        for (int i = 0; i < chars.length; i++) {
            chars[i] = chars[i].trim();
        }
        symbols = new ArrayList<>();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i].length() != 1) {
                log.warn("Invalid char: {} from charString: {}", chars[i], charString);
                throw new InvalidAlphabetException("Invalid char: " + chars[i]);
            }
            for (int j = i + 1; j < chars.length; j++) {
                if (chars[i].equals(chars[j])) {
                    log.warn("Duplicate char: {} from charString: {}", chars[i], charString);
                    throw new InvalidAlphabetException("Duplicate char: " + chars[i]);
                }
            }
            symbols.add(chars[i].charAt(0));
        }
        for (int i = 0; i < symbols.size(); i++) {
            mapForFasterAccess.put(symbols.get(i), i);
        }
    }

    public int length() {
        return symbols.size();
    }

    public char getSymbol(int pos) throws IndexOutOfBoundsException {
        if (pos >= symbols.size() || pos < 0) {
            log.warn("Requested Pos: {} has exceed max pos: {}", pos, symbols.size() - 1);
            throw new IndexOutOfBoundsException("Requested pos: " + pos + " max pos: " + (symbols.size() - 1));
        }
        return symbols.get(pos);
    }

    public int getPosForSymbol(char symbol) throws CharNotInAlphabetException {
        Integer result = mapForFasterAccess.get(symbol);
        if (result == null) {
            log.warn("Char: {} is not an element of this alphabet: {}", symbol, symbols);
            throw new CharNotInAlphabetException("Char " + symbol + " is not an element of this alphabet");
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < symbols.size(); i++) {
            if (i + 1 == symbols.size()) {
                result.append(symbols.get(i));
            } else {
                result.append(symbols.get(i) + " ");
            }
        }
        return result.toString();
    }
}
