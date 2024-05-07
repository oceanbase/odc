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
package com.oceanbase.odc.plugin.connect.obmysql.diagnose;

import java.util.Map;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/12
 */
public class PlanParameterSubstitutor {

    public static String replace(String charSequence, Map<String, String> variables) {
        StringBuilder builder = new StringBuilder();
        StringBuilder label = new StringBuilder(":");

        char[] cs = charSequence.toCharArray();
        for (int pos = 0; pos < cs.length; pos++) {
            if (cs[pos] == ':') {
                while (++pos < cs.length && Character.isDigit(cs[pos])) {
                    label.append(cs[pos]);
                }
                String s = label.toString();
                label = new StringBuilder(":");
                builder.append(variables.getOrDefault(s, s));
                if (pos >= cs.length) {
                    break;
                }
            }
            builder.append(cs[pos]);
        }
        return builder.toString();
    }

}
