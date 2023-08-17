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
package com.oceanbase.odc.libinjection;

import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: Lebie
 * @Date: 2021/7/5 下午2:44
 * @Description: []
 */
class Keyword {
    HashMap<String, Character> keywordMap = new HashMap<String, Character>();
    private static Pattern wordpattern = Pattern.compile("\\{\"(.*)\"");
    private static Pattern typepattern = Pattern.compile("\'(.*)\'");

    Keyword() {
        String word;
        char type;
        Matcher matchedword, matchedtype;
        Scanner in = new Scanner(this.getClass().getResourceAsStream("/Keywords.txt"));
        String line;

        while (in.hasNextLine()) {
            line = in.nextLine();
            matchedword = wordpattern.matcher(line);
            matchedtype = typepattern.matcher(line);

            while (matchedword.find() && matchedtype.find()) {
                word = matchedword.group(1);
                type = matchedtype.group(1).charAt(0);
                keywordMap.put(word, type);
            }
        }
        in.close();
    }
}
