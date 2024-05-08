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
package com.oceanbase.tools.sqlparser.statement;

import lombok.Getter;

/**
 * {@link Operator}
 *
 * @author yh263208
 * @date 2022-11-24 21:04
 * @since ODC_release_4.1.0
 */
@Getter
public enum Operator {
    // +
    ADD("+"),
    // -
    SUB("-"),
    // *
    MUL("*"),
    // /
    DIV("/"),
    // =
    EQ("="),
    // >=
    GE(">="),
    // >
    GT(">"),
    // <=
    LE("<="),
    // <
    LT("<"),
    // ~=
    NE_PL("~="),
    // not equals
    NE("!=", "<>", "^="),
    CNNOP("||"),
    TO("TO"),
    // in (a,b,c)
    IN("IN"),
    // not in (a,b,c)
    NOT_IN("NOT IN"),
    // between a and b
    BETWEEN("BETWEEN"),
    // not between a and b
    NOT_BETWEEN("NOT BETWEEN"),
    // like ''
    LIKE("LIKE"),
    // not like ''
    NOT_LIKE("NOT LIKE"),
    // a and b
    AND("AND"),
    OR("OR"),
    // escape xxx
    ESCAPE("ESCAPE"),
    MEMBER_OF("MEMBER OF", "MEMBER"),
    NOT_MEMBER_OF("NOT MEMBER OF", "NOT MEMBER"),
    SUBMULTISET_OF("SUBMULTISET OF", "SUBMULTISET"),
    NOT_SUBMULTISET_OF("NOT SUBMULTISET OF", "NOT SUBMULTISET"),
    IS_A_SET("IS A SET"),
    IS_NOT_A_SET("IS NOT A SET"),
    IS_EMPTY("IS EMPTY"),
    IS_NOT_EMPTY("IS NOT EMPTY"),
    NOT("NOT"),
    CONNECT_BY_ROOT("CONNECT_BY_ROOT"),
    PRIOR("PRIOR"),
    AT_TIME_ZONE("AT TIME ZONE"),
    AT_LOCAL("AT LOCAL"),
    MULTISET_OP("MULTISET UNION", "MULTISET INTERSECT", "MULTISET EXCEPT"),
    MULTISET_OP_ALL("MULTISET UNION ALL", "MULTISET INTERSECT ALL", "MULTISET EXCEPT ALL"),
    MULTISET_OP_DISTINCT("MULTISET UNION DISTINCT", "MULTISET INTERSECT DISTINCT", "MULTISET EXCEPT DISTINCT"),
    POW_PL("**"),
    MOD("MOD"),
    SET_VAR(":="),
    NSEQ("<=>"),
    // for mysql, regexp 'xxx'
    REGEXP("regexp"),
    // for mysql, not regexp 'xxx'
    NOT_REGEXP("not regexp"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),
    BINARY("binary"),
    TILDE("~"),
    EXISTS("exists"),
    JSON_EXTRACT("->"),
    JSON_EXTRACT_UNQUOTED("->>"),
    XOR("XOR"),
    // other, see syntax
    OTHER;

    private final String[] text;

    Operator(String... text) {
        this.text = text;
    }

}
