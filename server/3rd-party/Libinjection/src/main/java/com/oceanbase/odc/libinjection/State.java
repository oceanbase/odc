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
 * @Date: 2021/7/5 下午2:45
 * @Description: []
 */
class State {

    String s; /* input string */
    int slen; /* length of input */
    int fplen; /* length of fingerprint */
    int flags; /* flag to indicate which mode we're running in: example.) flag_quote_none AND flag_sql_ansi */
    int pos; /* index in string during tokenization */
    int current; /* current position in tokenvec */
    int stats_comment_ddw;
    int stats_comment_ddx;
    int stats_comment_c; /* c-style comments found /x .. x/ */
    int stats_comment_hash; /* '#' operators or MySQL EOL comments found */
    int stats_folds;
    int stats_tokens;
    Token[] tokenvec = new Token[8];
    String fingerprint;


    State(String s, int len, int flags) {
        if (flags == 0) {
            flags = Libinjection.FLAG_QUOTE_NONE | Libinjection.FLAG_SQL_ANSI;
        }
        this.s = s;
        this.slen = len;
        this.flags = flags;
        this.current = 0;
    }
}
