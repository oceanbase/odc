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
package com.oceanbase.odc.common.util.tableformat;

public class Filler {

    /**
     * @param width
     * @return String
     */
    public static String getFiller(final int width) {
        return getFiller(" ", width);
    }

    /**
     * @param txt
     * @param width
     * @return String
     */
    public static String getFiller(final String txt, final int width) {
        StringBuffer sb = new StringBuffer(width * txt.length());
        for (int i = 0; i < width; i++) {
            sb.append(txt);
        }
        return sb.toString();
    }
}
