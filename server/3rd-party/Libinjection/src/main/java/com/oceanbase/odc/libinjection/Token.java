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

/**
 * @Author: Lebie
 * @Date: 2021/7/5 下午3:53
 * @Description: []
 */
class Token {
    char type;
    char str_open;
    char str_close;
    String val;
    int count;
    // position and length of input in original string
    int pos;
    int len;

    Token(int stype, int pos, int len, String val) {
        this.type = (char) stype;
        this.pos = pos;
        this.len = len;
        this.val = val;
    }
}
