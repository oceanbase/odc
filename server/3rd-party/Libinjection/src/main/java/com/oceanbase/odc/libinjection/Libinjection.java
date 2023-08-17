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
 * @Date: 2021/7/5 下午2:43
 * @Description: [Main class of the git 3rd-party library for sql injection detecting, see here:
 *               https://github.com/jeonglee/Libinjection]
 */
public class Libinjection {

    public static final int LIBINJECTION_SQLI_MAX_TOKENS = 5;

    public static final int FLAG_QUOTE_NONE = 1; /* 1 << 0 */
    public static final int FLAG_QUOTE_SINGLE = 2; /* 1 << 1 */
    public static final int FLAG_QUOTE_DOUBLE = 4; /* 1 << 2 */
    public static final int FLAG_SQL_ANSI = 8; /* 1 << 3 */
    public static final int FLAG_SQL_MYSQL = 16; /* 1 << 4 */

    public static final int TYPE_NONE = 0;
    public static final int TYPE_KEYWORD = (int) 'k';
    public static final int TYPE_UNION = (int) 'U';
    public static final int TYPE_GROUP = (int) 'B';
    public static final int TYPE_EXPRESSION = (int) 'E';
    public static final int TYPE_SQLTYPE = (int) 't';
    public static final int TYPE_FUNCTION = (int) 'f';
    public static final int TYPE_BAREWORD = (int) 'n';
    public static final int TYPE_NUMBER = (int) '1';
    public static final int TYPE_VARIABLE = (int) 'v';
    public static final int TYPE_STRING = (int) 's';
    public static final int TYPE_OPERATOR = (int) 'o';
    public static final int TYPE_LOGIC_OPERATOR = (int) '&';
    public static final int TYPE_COMMENT = (int) 'c';
    public static final int TYPE_COLLATE = (int) 'A';
    public static final int TYPE_LEFTPARENS = (int) '(';
    public static final int TYPE_RIGHTPARENS = (int) ')';
    public static final int TYPE_LEFTBRACE = (int) '{';
    public static final int TYPE_RIGHTBRACE = (int) '}';
    public static final int TYPE_DOT = (int) '.';
    public static final int TYPE_COMMA = (int) ',';
    public static final int TYPE_COLON = (int) ':';
    public static final int TYPE_SEMICOLON = (int) ';';
    public static final int TYPE_TSQL = (int) 'T';
    public static final int TYPE_UNKNOWN = (int) '?';
    public static final int TYPE_EVIL = (int) 'X';
    public static final int TYPE_FINGERPRINT = (int) 'F';
    public static final int TYPE_BACKSLASH = (int) '\\';

    public static final char CHAR_NULL = '\0';
    public static final char CHAR_SINGLE = '\'';
    public static final char CHAR_DOUBLE = '"';
    public static final char CHAR_TICK = '`';

    private static Keyword keywords = new Keyword(); /* keyword hashmap */
    private State state;
    private String output;


    public State getState() {
        return state;
    }

    public String getOutput() {
        return output;
    }

    /**
     * Main API
     */
    public boolean libinjection_sqli(String input) {
        this.state = new State(input, input.length(), 0);
        boolean issqli = libinjection_is_sqli();

        output = issqli + " : " + state.fingerprint;
        // System.out.println(output);
        return issqli;
    }

    public boolean libinjection_is_sqli() {
        String s = state.s;
        int slen = state.slen;
        boolean sqlifingerprint;

        if (slen == 0) {
            state.fingerprint = "";
            return false;
        }

        /* test input as-is */
        libinjection_sqli_fingerprint(FLAG_QUOTE_NONE | FLAG_SQL_ANSI);
        sqlifingerprint = libinjection_sqli_check_fingerprint();
        if (sqlifingerprint) {
            return true;
        } else if (reparse_as_mysql()) {
            libinjection_sqli_fingerprint(FLAG_QUOTE_NONE | FLAG_SQL_MYSQL);
            sqlifingerprint = libinjection_sqli_check_fingerprint();
            if (sqlifingerprint) {
                return true;
            }
        }


        /*
         * if input contains single quote, pretend it starts with single quote example: admin' OR 1=1-- is
         * tested as 'admin' OR 1=1--
         */
        if (s.contains("'")) {
            libinjection_sqli_fingerprint(FLAG_QUOTE_SINGLE | FLAG_SQL_ANSI);
            sqlifingerprint = libinjection_sqli_check_fingerprint();
            if (sqlifingerprint) {
                return true;
            } else if (reparse_as_mysql()) {
                libinjection_sqli_fingerprint(FLAG_QUOTE_SINGLE | FLAG_SQL_MYSQL);
                sqlifingerprint = libinjection_sqli_check_fingerprint();
                if (sqlifingerprint) {
                    return true;
                }
            }
        }

        /*
         * same as above but with a double-quote "
         */
        if (s.contains("\"")) {
            libinjection_sqli_fingerprint(FLAG_QUOTE_DOUBLE | FLAG_SQL_MYSQL);
            sqlifingerprint = libinjection_sqli_check_fingerprint();
            if (sqlifingerprint) {
                return true;
            }
        }

        /* Not SQLi! */
        return false;
    }

    public boolean reparse_as_mysql() {
        return (state.stats_comment_ddx + state.stats_comment_hash) > 0;
    }

    /**
     * Secondary API: Detect SQLi GIVEN a context.
     */
    public String libinjection_sqli_fingerprint(int flags) {
        int fplen = 0;
        StringBuilder fp = new StringBuilder();

        /*
         * reset state: needed since we may test single input multiples times: - as is - single quote mode -
         * double quote mode
         */
        state = new State(state.s, state.slen, flags);

        /* get fingerprint */
        fplen = libinjection_sqli_fold();

        /*
         * Check for magic PHP backquote comment If: * last token is of type "bareword" * And is quoted in a
         * backtick * And isn't closed * And it's empty? Then convert it to comment
         */
        if (fplen > 2 && state.tokenvec[fplen - 1].type == TYPE_BAREWORD
                && state.tokenvec[fplen - 1].str_open == CHAR_TICK
                && state.tokenvec[fplen - 1].len == 0
                && state.tokenvec[fplen - 1].str_close == CHAR_NULL) {
            state.tokenvec[fplen - 1].type = TYPE_COMMENT;
        }

        /* copy fingerprint to String */
        for (int i = 0; i < fplen; i++) {
            fp.append(state.tokenvec[i].type);
        }
        state.fingerprint = fp.toString();

        /*
         * check for 'X' in pattern, and then clear out all tokens
         *
         * this means parsing could not be done accurately due to pgsql's double comments or other syntax
         * that isn't consistent. Should be very rare false positive
         */
        if (state.fingerprint.indexOf(TYPE_EVIL) != -1) {
            state.fingerprint = "X";
            Token token = new Token(TYPE_EVIL, 0, 0, String.valueOf(TYPE_EVIL));
            Token[] replace = {token, null, null, null, null, null, null, null};
            state.tokenvec = replace;
        }

        return state.fingerprint;
    }

    public Character libinjection_sqli_lookup_word(String str) {
        return keywords.keywordMap.get(str.toUpperCase());
    }

    public boolean is_keyword(String str) {
        Character value = keywords.keywordMap.get(str.toUpperCase());

        if (value == null || value != TYPE_FINGERPRINT) {
            return false;
        }
        return true;
    }

    public boolean libinjection_sqli_check_fingerprint() {
        return libinjection_sqli_blacklist() && libinjection_sqli_not_whitelist();
    }

    public boolean libinjection_sqli_blacklist() {
        int len = state.fingerprint.length();

        if (len > 0 && is_keyword(state.fingerprint)) {
            return true;
        }
        return false;
    }

    /*
     * return true if SQLi, false if benign
     */
    public boolean libinjection_sqli_not_whitelist() {
        /*
         * We assume we got a SQLi match This next part just helps reduce false positives.
         *
         */
        char ch;
        String fingerprint = state.fingerprint;
        int tlen = fingerprint.length();

        if (tlen > 1 && fingerprint.charAt(tlen - 1) == TYPE_COMMENT) {
            /*
             * if ending comment is 'sp_password' then it's SQLi! MS Audit log apparently ignores anything with
             * 'sp_password' in it. Unable to find primary reference to this "feature" of SQL Server but seems
             * to be known SQLi technique
             */
            if (state.s.contains("sp_password")) {
                return true;
            }
        }

        switch (tlen) {
            case 2: {
                /*
                 * case 2 are "very small SQLi" which make them hard to tell from normal input...
                 */

                if (fingerprint.charAt(1) == TYPE_UNION) {
                    if (state.stats_tokens == 2) {
                        /*
                         * not sure why but 1U comes up in SQLi attack likely part of parameter splitting/etc. lots of
                         * reasons why "1 union" might be normal input, so beep only if other SQLi things are present
                         */
                        /*
                         * it really is a number and 'union' other wise it has folding or comments
                         */
                        return false;
                    } else {
                        return true;
                    }
                }
                /*
                 * if 'comment' is '#' ignore.. too many FP
                 */
                if (state.tokenvec[1].val.charAt(0) == '#') {
                    return false;
                }

                /*
                 * for fingerprint like 'nc', only comments of /x are treated as SQL... ending comments of "--" and
                 * "#" are not SQLi
                 */
                if (state.tokenvec[0].type == TYPE_BAREWORD &&
                        state.tokenvec[1].type == TYPE_COMMENT &&
                        state.tokenvec[1].val.charAt(0) != '/') {
                    return false;
                }

                /*
                 * if '1c' ends with '/x' then it's SQLi
                 */
                if (state.tokenvec[0].type == TYPE_NUMBER &&
                        state.tokenvec[1].type == TYPE_COMMENT &&
                        state.tokenvec[1].val.charAt(0) == '/') {
                    return true;
                }

                /*
                 * there are some odd base64-looking query string values 1234-ABCDEFEhfhihwuefi-- which evaluate to
                 * "1c"... these are not SQLi but 1234-- probably is. Make sure the "1" in "1c" is actually a true
                 * decimal number
                 *
                 * Need to check -original- string since the folding step may have merged tokens, e.g. "1+FOO" is
                 * folded into "1"
                 *
                 * Note: evasion: 1*1--
                 */
                if (state.tokenvec[0].type == TYPE_NUMBER &&
                        state.tokenvec[1].type == TYPE_COMMENT) {
                    if (state.stats_tokens > 2) {
                        /* we have some folding going on, highly likely SQLi */
                        return true;
                    }
                    /*
                     * we check that next character after the number is either whitespace, or '/' or a '-' ==> SQLi.
                     */
                    ch = state.s.charAt(state.tokenvec[0].len);
                    if (ch <= 32) {
                        /*
                         * next char was whitespace,e.g. "1234 --" this isn't exactly correct.. ideally we should skip
                         * over all whitespace but this seems to be ok for now
                         */
                        return true;
                    }
                    if (ch == '/' && state.s.charAt(state.tokenvec[0].len + 1) == '*') {
                        return true;
                    }
                    if (ch == '-' && state.s.charAt(state.tokenvec[0].len + 1) == '-') {
                        return true;
                    }

                    return false;
                }

                /*
                 * detect obvious SQLi scans.. many people put '--' in plain text so only detect if input ends with
                 * '--', e.g. 1-- but not 1-- foo
                 */
                if ((state.tokenvec[1].len > 2)
                        && state.tokenvec[1].val.charAt(0) == '-') {
                    return false;
                }

                break;
            } /* case 2 */
            case 3: {
                /*
                 * ...foo' + 'bar... no opening quote, no closing quote and each string has data
                 */

                if (fingerprint.equals("sos")
                        || fingerprint.equals("s&s")) {

                    if ((state.tokenvec[0].str_open == CHAR_NULL)
                            && (state.tokenvec[2].str_close == CHAR_NULL)
                            && (state.tokenvec[0].str_close == state.tokenvec[2].str_open)) {
                        /*
                         * if ....foo" + "bar....
                         */
                        return true;
                    }
                    if (state.stats_tokens == 3) {
                        return false;
                    }

                    /*
                     * not SQLi
                     */
                    return false;
                } else if (state.fingerprint.equals("s&n") ||
                        state.fingerprint.equals("n&1") ||
                        state.fingerprint.equals("1&1") ||
                        state.fingerprint.equals("1&v") ||
                        state.fingerprint.equals("1&s")) {
                    /*
                     * 'sexy and 17' not SQLi 'sexy and 17<18' SQLi
                     */
                    if (state.stats_tokens == 3) {
                        return false;
                    }
                } else if (state.tokenvec[1].type == TYPE_KEYWORD) {
                    String keyword = state.tokenvec[1].val.toUpperCase();
                    if ((state.tokenvec[1].len < 5) ||
                            !(keyword.equals("INTO OUTFILE") || keyword.equals("INTO DUMPFILE"))) {
                        /*
                         * if it's not "INTO OUTFILE", or "INTO DUMPFILE" (MySQL) then treat as safe
                         */
                        return false;
                    }
                }
                break;
            } /* case 3 */
            case 4: {
                /* NOVC, 1OVC */
                if (state.fingerprint.equals("novc") || state.fingerprint.equals("1ovc")) {
                    if (state.tokenvec[1].val.equals("!")
                            && state.tokenvec[2].len == 0
                            && state.tokenvec[3].val.charAt(0) == '#') {
                        /*
                         * case where user enters !@# in password
                         */
                        return false;
                    }
                }
                break;
            } /* case 4 */
            case 5: {
                /* nothing right now */
                break;
            } /* case 5 */
        } /* end switch */

        return true;
    }

    public int libinjection_sqli_fold() {
        int pos = 0; /* position where NEXT token goes */
        int left = 0; /* # of tokens so far that will be part of the final fingerprint */
        boolean more = true; /* more characters in input to check? */
        int current = state.current;
        Token last_comment = new Token(CHAR_NULL, 0, 0, null); /* A comment token to add additional info */

        /* skip stuff we don't need to look at */
        while (more) {
            more = libinjection_sqli_tokenize();
            if (!(state.tokenvec[current].type == TYPE_COMMENT
                    || state.tokenvec[current].type == TYPE_LEFTPARENS
                    || state.tokenvec[current].type == TYPE_SQLTYPE
                    || token_is_unary_op(state.tokenvec[current]))) {
                break;
            }
        }

        if (!more) {
            return 0;
        } else {
            pos += 1;
        }

        /* the actual tokenizing and folding */
        while (true) {
            /*
             * do we have all the max number of tokens? if so do some special cases for 5 tokens
             */
            if (pos >= LIBINJECTION_SQLI_MAX_TOKENS) {
                if ((state.tokenvec[0].type == TYPE_NUMBER
                        && (state.tokenvec[1].type == TYPE_OPERATOR || state.tokenvec[1].type == TYPE_COMMA)
                        && state.tokenvec[2].type == TYPE_LEFTPARENS
                        && state.tokenvec[3].type == TYPE_NUMBER
                        && state.tokenvec[4].type == TYPE_RIGHTPARENS)
                        || (state.tokenvec[0].type == TYPE_BAREWORD
                                && state.tokenvec[1].type == TYPE_OPERATOR
                                && state.tokenvec[2].type == TYPE_LEFTPARENS
                                && (state.tokenvec[3].type == TYPE_BAREWORD || state.tokenvec[3].type == TYPE_NUMBER)
                                && state.tokenvec[4].type == TYPE_RIGHTPARENS)
                        || (state.tokenvec[0].type == TYPE_NUMBER
                                && state.tokenvec[1].type == TYPE_RIGHTPARENS
                                && state.tokenvec[2].type == TYPE_COMMA
                                && state.tokenvec[3].type == TYPE_LEFTPARENS
                                && state.tokenvec[4].type == TYPE_NUMBER)
                        || (state.tokenvec[0].type == TYPE_BAREWORD
                                && state.tokenvec[1].type == TYPE_RIGHTPARENS
                                && state.tokenvec[2].type == TYPE_OPERATOR
                                && state.tokenvec[3].type == TYPE_LEFTPARENS
                                && state.tokenvec[4].type == TYPE_BAREWORD)) {
                    if (pos > LIBINJECTION_SQLI_MAX_TOKENS) {
                        state.tokenvec[1] = state.tokenvec[LIBINJECTION_SQLI_MAX_TOKENS];
                        pos = 2;
                        left = 0;
                    } else {
                        pos = 1;
                        left = 0;
                    }
                }
            }

            /* if checked all of input or # of tokens in fingerprint exceeds 5, stop. */
            if (!more || left >= LIBINJECTION_SQLI_MAX_TOKENS) {
                left = pos;
                break;
            }

            /* get up to two tokens */
            while (more && pos <= LIBINJECTION_SQLI_MAX_TOKENS && (pos - left) < 2) {
                state.current = pos;
                current = state.current;

                more = libinjection_sqli_tokenize();
                if (more) {
                    if (state.tokenvec[current].type == TYPE_COMMENT) {
                        last_comment = state.tokenvec[current];
                    } else {
                        last_comment.type = CHAR_NULL;
                        pos += 1;
                    }
                }
            }

            /*
             * if we didn't get at least two tokens, it means we exited above while loop because we: 1.)
             * processed all of the input OR 2.) added the 5th (and last) token In this case start over
             */
            if (pos - left < 2) {
                left = pos;
                continue;
            }

            /*
             * two token folding
             */
            if (state.tokenvec[left].type == TYPE_STRING
                    && state.tokenvec[left + 1].type == TYPE_STRING) {
                pos -= 1;
                state.stats_folds += 1;
                continue;
            } else if (state.tokenvec[left].type == TYPE_SEMICOLON
                    && state.tokenvec[left + 1].type == TYPE_SEMICOLON) {
                /* fold away repeated semicolons. i.e. ;; to ; */
                pos -= 1;
                state.stats_folds += 1;
                continue;
            } else if (state.tokenvec[left].type == TYPE_SEMICOLON
                    && state.tokenvec[left + 1].type == TYPE_FUNCTION
                    && state.tokenvec[left + 1].val.toUpperCase().equals("IF")) {
                state.tokenvec[left + 1].type = TYPE_TSQL;
                left += 2;
                continue; /* reparse everything. but we probably can advance left, and pos */
            } else if ((state.tokenvec[left].type == TYPE_OPERATOR || state.tokenvec[left].type == TYPE_LOGIC_OPERATOR)
                    && (token_is_unary_op(state.tokenvec[left + 1]) || state.tokenvec[left + 1].type == TYPE_SQLTYPE)) {
                pos -= 1;
                state.stats_folds += 1;
                left = 0;
                continue;
            } else if (state.tokenvec[left].type == TYPE_LEFTPARENS
                    && token_is_unary_op(state.tokenvec[left + 1])) {
                pos -= 1;
                state.stats_folds += 1;
                if (left > 0) {
                    left -= 1;
                }
                continue;
            } else if (syntax_merge_words(state.tokenvec[left], left, state.tokenvec[left + 1], left + 1)) {
                pos -= 1;
                state.stats_folds += 1;
                if (left > 0) {
                    left -= 1;
                }
                continue;
            }

            /*
             * two token handling.
             */
            else if ((state.tokenvec[left].type == TYPE_BAREWORD || state.tokenvec[left].type == TYPE_VARIABLE)
                    && state.tokenvec[left + 1].type == TYPE_LEFTPARENS
                    && (
                    /* TSQL functions but common enough to be column names */
                    state.tokenvec[left].val.toUpperCase().equals("USER_ID")
                            || state.tokenvec[left].val.toUpperCase().equals("USER_NAME") ||

                            /* Function in MYSQL */
                            state.tokenvec[left].val.toUpperCase().equals("DATABASE")
                            || state.tokenvec[left].val.toUpperCase().equals("PASSWORD")
                            || state.tokenvec[left].val.toUpperCase().equals("USER") ||

                            /*
                             * Mysql words that act as a variable and are a function
                             */

                            /* TSQL current_users is fake-variable */
                            /*
                             * http://msdn.microsoft.com/en-us/library/ms176050. aspx
                             */
                            state.tokenvec[left].val.toUpperCase().equals("CURRENT_USER")
                            || state.tokenvec[left].val.toUpperCase().equals("CURRENT_DATE")
                            || state.tokenvec[left].val.toUpperCase().equals("CURRENT_TIME")
                            || state.tokenvec[left].val.toUpperCase().equals("CURRENT_TIMESTAMP")
                            || state.tokenvec[left].val.toUpperCase().equals("LOCALTIME")
                            || state.tokenvec[left].val.toUpperCase().equals("LOCALTIMESTAMP"))) {
                /*
                 * pos is the same other conversions need to go here... for instance password CAN be a function,
                 * coalesce CAN be a function
                 */
                state.tokenvec[left].type = TYPE_FUNCTION;
                continue;
            } else if (state.tokenvec[left].type == TYPE_KEYWORD
                    && (state.tokenvec[left].val.toUpperCase().equals("IN")
                            || state.tokenvec[left].val.toUpperCase().equals("NOT IN"))) {

                if (state.tokenvec[left + 1].type == TYPE_LEFTPARENS) {
                    /* got .... IN ( ... (or 'NOT IN') it's an operator */
                    state.tokenvec[left].type = TYPE_OPERATOR;
                } else {
                    /* it's a nothing */
                    state.tokenvec[left].type = TYPE_BAREWORD;
                }

                /*
                 * "IN" can be used as "IN BOOLEAN MODE" for mysql in which case merging of words can be done later
                 * other wise it acts as an equality operator __ IN (values..)
                 *
                 * here we got "IN" "(" so it's an operator. also back track to handle "NOT IN" might need to do the
                 * same with like two use cases "foo" LIKE "BAR" (normal operator) "foo" = LIKE(1,2)
                 */
                continue;
            } else if ((state.tokenvec[left].type == TYPE_OPERATOR)
                    && (state.tokenvec[left].val.toUpperCase().equals("LIKE")
                            || state.tokenvec[left].val.toUpperCase().equals("NOT LIKE"))) {
                if (state.tokenvec[left + 1].type == TYPE_LEFTPARENS) {
                    /* SELECT LIKE(... it's a function */
                    state.tokenvec[left].type = TYPE_FUNCTION;
                }
            } else if (state.tokenvec[left].type == TYPE_SQLTYPE && (state.tokenvec[left + 1].type == TYPE_BAREWORD
                    || state.tokenvec[left + 1].type == TYPE_NUMBER
                    || state.tokenvec[left + 1].type == TYPE_SQLTYPE
                    || state.tokenvec[left + 1].type == TYPE_LEFTPARENS
                    || state.tokenvec[left + 1].type == TYPE_FUNCTION
                    || state.tokenvec[left + 1].type == TYPE_VARIABLE
                    || state.tokenvec[left + 1].type == TYPE_STRING)) {
                state.tokenvec[left] = state.tokenvec[left + 1];
                pos -= 1;
                state.stats_folds += 1;
                left = 0;
                continue;
            } else if (state.tokenvec[left].type == TYPE_COLLATE
                    && state.tokenvec[left + 1].type == TYPE_BAREWORD) {
                /*
                 * there are too many collation types.. so if the bareword has a "_" then it's TYPE_SQLTYPE
                 */
                if (state.tokenvec[left + 1].val.indexOf('_') != -1) {
                    state.tokenvec[left + 1].type = TYPE_SQLTYPE;
                    left = 0;
                }
            } else if (state.tokenvec[left].type == TYPE_BACKSLASH) {
                if (token_is_arithmetic_op(state.tokenvec[left + 1])) {
                    /* very weird case in TSQL where '\%1' is parsed as '0 % 1',etc */
                    state.tokenvec[left].type = TYPE_NUMBER;
                } else {
                    /* just ignore it.. Again T-SQL seems to parse \1 as "1" */
                    state.tokenvec[left] = state.tokenvec[left + 1];
                    pos -= 1;
                    state.stats_folds += 1;
                }
                left = 0;
                continue;
            } else if (state.tokenvec[left].type == TYPE_LEFTPARENS
                    && state.tokenvec[left + 1].type == TYPE_LEFTPARENS) {
                pos -= 1;
                left = 0;
                state.stats_folds += 1;
                continue;
            } else if (state.tokenvec[left].type == TYPE_RIGHTPARENS
                    && state.tokenvec[left + 1].type == TYPE_RIGHTPARENS) {
                pos -= 1;
                left = 0;
                state.stats_folds += 1;
                continue;
            } else if (state.tokenvec[left].type == TYPE_LEFTBRACE
                    && state.tokenvec[left + 1].type == TYPE_BAREWORD) {

                /*
                 * MySQL Degenerate case --
                 *
                 * select { ``.``.id }; -- valid !!! select { ``.``.``.id }; -- invalid select ``.``.id; -- invalid
                 * select { ``.id }; -- invalid
                 *
                 * so it appears {``.``.id} is a magic case I suspect this is
                 * "current database, current table, field id"
                 *
                 * The folding code can't look at more than 3 tokens, and I don't want to make two passes.
                 *
                 * Since "{ ``" so rare, we are just going to blacklist it.
                 *
                 * Highly likely this will need revisiting!
                 *
                 * CREDIT @rsalgado 2013-11-25
                 */
                if (state.tokenvec[left + 1].len == 0) {
                    state.tokenvec[left + 1].type = TYPE_EVIL;
                    return (int) (left + 2);
                }
                /*
                 * weird ODBC / MYSQL {foo expr} --> expr but for this rule we just strip away the "{ foo" part
                 */
                left = 0;
                pos -= 2;
                state.stats_folds += 2;
                continue;
            } else if (state.tokenvec[left + 1].type == TYPE_RIGHTBRACE) {
                pos -= 1;
                left = 0;
                state.stats_folds += 1;
                continue;
            }


            /*
             * all cases of handling 2 tokens is done and nothing matched. Get one more token
             */
            while (more && pos <= LIBINJECTION_SQLI_MAX_TOKENS && (pos - left) < 3) {
                state.current = pos;
                current = state.current;

                more = libinjection_sqli_tokenize();
                if (more) {
                    if (state.tokenvec[current].type == TYPE_COMMENT) {
                        last_comment = state.tokenvec[current];
                    } else {
                        last_comment.type = CHAR_NULL;
                        pos += 1;
                    }
                }
            }

            /*
             * if we didn't get at least three tokens, it means we exited above while loop because we: 1.)
             * processed all of the input OR 2.) added the 5th (and last) token In this case start over.
             */
            if (pos - left < 3) {
                left = pos;
                continue;
            }

            /*
             * Three token folding
             */

            if (state.tokenvec[left].type == TYPE_NUMBER
                    && state.tokenvec[left + 1].type == TYPE_OPERATOR
                    && state.tokenvec[left + 2].type == TYPE_NUMBER) {
                pos -= 2;
                left = 0;
                continue;
            } else if (state.tokenvec[left].type == TYPE_OPERATOR
                    && state.tokenvec[left + 1].type != TYPE_LEFTPARENS
                    && state.tokenvec[left + 2].type == TYPE_OPERATOR) {
                left = 0;
                pos -= 2;
                continue;
            } else if (state.tokenvec[left].type == TYPE_LOGIC_OPERATOR
                    && state.tokenvec[left + 2].type == TYPE_LOGIC_OPERATOR) {
                pos -= 2;
                left = 0;
                continue;
            } else if (state.tokenvec[left].type == TYPE_VARIABLE
                    && state.tokenvec[left + 1].type == TYPE_OPERATOR
                    && (state.tokenvec[left + 2].type == TYPE_VARIABLE
                            || state.tokenvec[left + 2].type == TYPE_NUMBER
                            || state.tokenvec[left + 2].type == TYPE_BAREWORD)) {
                pos -= 2;
                left = 0;
                continue;
            } else if ((state.tokenvec[left].type == TYPE_BAREWORD || state.tokenvec[left].type == TYPE_NUMBER)
                    && state.tokenvec[left + 1].type == TYPE_OPERATOR
                    && (state.tokenvec[left + 2].type == TYPE_NUMBER
                            || state.tokenvec[left + 2].type == TYPE_BAREWORD)) {
                pos -= 2;
                left = 0;
                continue;
            } else if ((state.tokenvec[left].type == TYPE_BAREWORD
                    || state.tokenvec[left].type == TYPE_NUMBER
                    || state.tokenvec[left].type == TYPE_VARIABLE
                    || state.tokenvec[left].type == TYPE_STRING)
                    && state.tokenvec[left + 1].type == TYPE_OPERATOR
                    && state.tokenvec[left + 1].val.equals("::")
                    && state.tokenvec[left + 2].type == TYPE_SQLTYPE) {
                pos -= 2;
                left = 0;
                state.stats_folds += 2;
                continue;
            } else if ((state.tokenvec[left].type == TYPE_BAREWORD
                    || state.tokenvec[left].type == TYPE_NUMBER
                    || state.tokenvec[left].type == TYPE_STRING
                    || state.tokenvec[left].type == TYPE_VARIABLE)
                    && state.tokenvec[left + 1].type == TYPE_COMMA
                    && (state.tokenvec[left + 2].type == TYPE_NUMBER
                            || state.tokenvec[left + 2].type == TYPE_BAREWORD
                            || state.tokenvec[left + 2].type == TYPE_STRING
                            || state.tokenvec[left + 2].type == TYPE_VARIABLE)) {
                pos -= 2;
                left = 0;
                continue;
            } else if ((state.tokenvec[left].type == TYPE_EXPRESSION
                    || state.tokenvec[left].type == TYPE_GROUP
                    || state.tokenvec[left].type == TYPE_COMMA)
                    && token_is_unary_op(state.tokenvec[left + 1])
                    && state.tokenvec[left + 2].type == TYPE_LEFTPARENS) {
                /*
                 * got something like SELECT + (, LIMIT + ( remove unary operator
                 */
                state.tokenvec[left + 1] = state.tokenvec[left + 2];
                pos -= 1;
                left = 0;
                continue;
            } else if ((state.tokenvec[left].type == TYPE_KEYWORD
                    || state.tokenvec[left].type == TYPE_EXPRESSION
                    || state.tokenvec[left].type == TYPE_GROUP)
                    && token_is_unary_op(state.tokenvec[left + 1])
                    && (state.tokenvec[left + 2].type == TYPE_NUMBER
                            || state.tokenvec[left + 2].type == TYPE_BAREWORD
                            || state.tokenvec[left + 2].type == TYPE_VARIABLE
                            || state.tokenvec[left + 2].type == TYPE_STRING
                            || state.tokenvec[left + 2].type == TYPE_FUNCTION)) {
                /*
                 * remove unary operators select - 1
                 */
                state.tokenvec[left + 1] = state.tokenvec[left + 2];
                pos -= 1;
                left = 0;
                continue;
            } else if (state.tokenvec[left].type == TYPE_COMMA && token_is_unary_op(state.tokenvec[left + 1])
                    && (state.tokenvec[left + 2].type == TYPE_NUMBER
                            || state.tokenvec[left + 2].type == TYPE_BAREWORD
                            || state.tokenvec[left + 2].type == TYPE_VARIABLE
                            || state.tokenvec[left + 2].type == TYPE_STRING)) {
                /*
                 * interesting case turn ", -1" ->> ",1" PLUS we need to back up one token if possible to see if
                 * more folding can be done "1,-1" --> "1"
                 */
                state.tokenvec[left + 1] = state.tokenvec[left + 2];
                left = 0;
                /* pos is >= 3 so this is safe */
                assert (pos >= 3);
                pos -= 3;
                continue;
            } else if (state.tokenvec[left].type == TYPE_COMMA
                    && token_is_unary_op(state.tokenvec[left + 1])
                    && state.tokenvec[left + 2].type == TYPE_FUNCTION) {

                /*
                 * Separate case from above since you end up with 1,-sin(1) --> 1 (1) Here, just do 1,-sin(1) -->
                 * 1,sin(1) just remove unary operator
                 */
                state.tokenvec[left + 1] = state.tokenvec[left + 2];
                pos -= 1;
                left = 0;
                continue;
            } else if ((state.tokenvec[left].type == TYPE_BAREWORD)
                    && (state.tokenvec[left + 1].type == TYPE_DOT)
                    && (state.tokenvec[left + 2].type == TYPE_BAREWORD)) {
                /*
                 * ignore the '.n' typically is this databasename.table
                 */
                assert (pos >= 3);
                pos -= 2;
                left = 0;
                continue;
            } else if ((state.tokenvec[left].type == TYPE_EXPRESSION)
                    && (state.tokenvec[left + 1].type == TYPE_DOT)
                    && (state.tokenvec[left + 2].type == TYPE_BAREWORD)) {
                /*
                 * select . `foo` --> select `foo`
                 */
                state.tokenvec[left + 1] = state.tokenvec[left + 2];
                pos -= 1;
                left = 0;
                continue;
            } else if ((state.tokenvec[left].type == TYPE_FUNCTION)
                    && (state.tokenvec[left + 1].type == TYPE_LEFTPARENS)
                    && (state.tokenvec[left + 2].type != TYPE_RIGHTPARENS)) {
                /*
                 * whats going on here Some SQL functions like USER() have 0 args if we get User(foo), then User is
                 * not a function This should be expanded since it eliminated a lot of false positives.
                 */
                if (state.tokenvec[left].val.toUpperCase().equals("USER")) {
                    state.tokenvec[left].type = TYPE_BAREWORD;
                }
            }

            /*
             * assume left-most token is good, now use the existing 2 tokens, do not get another
             */
            left += 1;

        } /* while(1) */

        /*
         * if we have 4 or less tokens, and we had a comment token at the end, add it back
         */
        if (left < LIBINJECTION_SQLI_MAX_TOKENS && last_comment.type == TYPE_COMMENT) {
            state.tokenvec[left] = last_comment;
            left += 1;
        }

        /*
         * sometimes we grab a 6th token to help determine the type of token 5. --> what does this mean?
         */
        if (left > LIBINJECTION_SQLI_MAX_TOKENS) {
            left = LIBINJECTION_SQLI_MAX_TOKENS;
        }

        return left;
    }

    /*
     * Tokenize, return whether there are more characters to tokenize
     */
    public boolean libinjection_sqli_tokenize() {
        int pos = state.pos;
        int slen = state.slen;
        int current = state.current;
        String s = state.s;

        if (slen == 0) {
            return false;
        }

        /* clear token in current position (also to initialize) */
        state.tokenvec[current] = new Token(TYPE_NONE, 0, 0, "");

        /*
         * if we are at beginning of string and in single-quote or double quote mode then pretend the input
         * starts with a quote
         */
        if (pos == 0 && (state.flags & (FLAG_QUOTE_SINGLE | FLAG_QUOTE_DOUBLE)) != 0) {
            state.pos = parse_string_core(flag2delim(state.flags), 0);
            state.stats_tokens += 1;
            return true;
        }

        while (pos < slen) {
            char ch = s.charAt(pos); /* current character */
            switch (ch) {
                case 0:
                    pos = parse_white();
                    break; /* 0 */
                case 1:
                    pos = parse_white();
                    break; /* 1 */
                case 2:
                    pos = parse_white();
                    break; /* 2 */
                case 3:
                    pos = parse_white();
                    break; /* 3 */
                case 4:
                    pos = parse_white();
                    break; /* 4 */
                case 5:
                    pos = parse_white();
                    break; /* 5 */
                case 6:
                    pos = parse_white();
                    break; /* 6 */
                case 7:
                    pos = parse_white();
                    break; /* 7 */
                case 8:
                    pos = parse_white();
                    break; /* 8 */
                case 9:
                    pos = parse_white();
                    break; /* 9 */
                case 10:
                    pos = parse_white();
                    break; /* 10 */
                case 11:
                    pos = parse_white();
                    break; /* 11 */
                case 12:
                    pos = parse_white();
                    break; /* 12 */
                case 13:
                    pos = parse_white();
                    break; /* 13 */
                case 14:
                    pos = parse_white();
                    break; /* 14 */
                case 15:
                    pos = parse_white();
                    break; /* 15 */
                case 16:
                    pos = parse_white();
                    break; /* 16 */
                case 17:
                    pos = parse_white();
                    break; /* 17 */
                case 18:
                    pos = parse_white();
                    break; /* 18 */
                case 19:
                    pos = parse_white();
                    break; /* 19 */
                case 20:
                    pos = parse_white();
                    break; /* 20 */
                case 21:
                    pos = parse_white();
                    break; /* 21 */
                case 22:
                    pos = parse_white();
                    break; /* 22 */
                case 23:
                    pos = parse_white();
                    break; /* 23 */
                case 24:
                    pos = parse_white();
                    break; /* 24 */
                case 25:
                    pos = parse_white();
                    break; /* 25 */
                case 26:
                    pos = parse_white();
                    break; /* 26 */
                case 27:
                    pos = parse_white();
                    break; /* 27 */
                case 28:
                    pos = parse_white();
                    break; /* 28 */
                case 29:
                    pos = parse_white();
                    break; /* 29 */
                case 30:
                    pos = parse_white();
                    break; /* 30 */
                case 31:
                    pos = parse_white();
                    break; /* 31 */
                case 32:
                    pos = parse_white();
                    break; /* 32 */
                case 33:
                    pos = parse_operator2();
                    break; /* 33 */
                case 34:
                    pos = parse_string();
                    break; /* 34 */
                case 35:
                    pos = parse_hash();
                    break; /* 35 */
                case 36:
                    pos = parse_money();
                    break; /* 36 */
                case 37:
                    pos = parse_operator1();
                    break; /* 37 */
                case 38:
                    pos = parse_operator2();
                    break; /* 38 */
                case 39:
                    pos = parse_string();
                    break; /* 39 */
                case 40:
                    pos = parse_char();
                    break; /* 40 */
                case 41:
                    pos = parse_char();
                    break; /* 41 */
                case 42:
                    pos = parse_operator2();
                    break; /* 42 */
                case 43:
                    pos = parse_operator1();
                    break; /* 43 */
                case 44:
                    pos = parse_char();
                    break; /* 44 */
                case 45:
                    pos = parse_dash();
                    break; /* 45 */
                case 46:
                    pos = parse_number();
                    break; /* 46 */
                case 47:
                    pos = parse_slash();
                    break; /* 47 */
                case 48:
                    pos = parse_number();
                    break; /* 48 */
                case 49:
                    pos = parse_number();
                    break; /* 49 */
                case 50:
                    pos = parse_number();
                    break; /* 50 */
                case 51:
                    pos = parse_number();
                    break; /* 51 */
                case 52:
                    pos = parse_number();
                    break; /* 52 */
                case 53:
                    pos = parse_number();
                    break; /* 53 */
                case 54:
                    pos = parse_number();
                    break; /* 54 */
                case 55:
                    pos = parse_number();
                    break; /* 55 */
                case 56:
                    pos = parse_number();
                    break; /* 56 */
                case 57:
                    pos = parse_number();
                    break; /* 57 */
                case 58:
                    pos = parse_operator2();
                    break; /* 58 */
                case 59:
                    pos = parse_char();
                    break; /* 59 */
                case 60:
                    pos = parse_operator2();
                    break; /* 60 */
                case 61:
                    pos = parse_operator2();
                    break; /* 61 */
                case 62:
                    pos = parse_operator2();
                    break; /* 62 */
                case 63:
                    pos = parse_other();
                    break; /* 63 */
                case 64:
                    pos = parse_var();
                    break; /* 64 */
                case 65:
                    pos = parse_word();
                    break; /* 65 */
                case 66:
                    pos = parse_bstring();
                    break; /* 66 */
                case 67:
                    pos = parse_word();
                    break; /* 67 */
                case 68:
                    pos = parse_word();
                    break; /* 68 */
                case 69:
                    pos = parse_estring();
                    break; /* 69 */
                case 70:
                    pos = parse_word();
                    break; /* 70 */
                case 71:
                    pos = parse_word();
                    break; /* 71 */
                case 72:
                    pos = parse_word();
                    break; /* 72 */
                case 73:
                    pos = parse_word();
                    break; /* 73 */
                case 74:
                    pos = parse_word();
                    break; /* 74 */
                case 75:
                    pos = parse_word();
                    break; /* 75 */
                case 76:
                    pos = parse_word();
                    break; /* 76 */
                case 77:
                    pos = parse_word();
                    break; /* 77 */
                case 78:
                    pos = parse_nqstring();
                    break; /* 78 */
                case 79:
                    pos = parse_word();
                    break; /* 79 */
                case 80:
                    pos = parse_word();
                    break; /* 80 */
                case 81:
                    pos = parse_qstring();
                    break; /* 81 */
                case 82:
                    pos = parse_word();
                    break; /* 82 */
                case 83:
                    pos = parse_word();
                    break; /* 83 */
                case 84:
                    pos = parse_word();
                    break; /* 84 */
                case 85:
                    pos = parse_ustring();
                    break; /* 85 */
                case 86:
                    pos = parse_word();
                    break; /* 86 */
                case 87:
                    pos = parse_word();
                    break; /* 87 */
                case 88:
                    pos = parse_xstring();
                    break; /* 88 */
                case 89:
                    pos = parse_word();
                    break; /* 89 */
                case 90:
                    pos = parse_word();
                    break; /* 90 */
                case 91:
                    pos = parse_bword();
                    break; /* 91 */
                case 92:
                    pos = parse_backslash();
                    break; /* 92 */
                case 93:
                    pos = parse_other();
                    break; /* 93 */
                case 94:
                    pos = parse_operator1();
                    break; /* 94 */
                case 95:
                    pos = parse_word();
                    break; /* 95 */
                case 96:
                    pos = parse_tick();
                    break; /* 96 */
                case 97:
                    pos = parse_word();
                    break; /* 97 */
                case 98:
                    pos = parse_bstring();
                    break; /* 98 */
                case 99:
                    pos = parse_word();
                    break; /* 99 */
                case 100:
                    pos = parse_word();
                    break; /* 100 */
                case 101:
                    pos = parse_estring();
                    break; /* 101 */
                case 102:
                    pos = parse_word();
                    break; /* 102 */
                case 103:
                    pos = parse_word();
                    break; /* 103 */
                case 104:
                    pos = parse_word();
                    break; /* 104 */
                case 105:
                    pos = parse_word();
                    break; /* 105 */
                case 106:
                    pos = parse_word();
                    break; /* 106 */
                case 107:
                    pos = parse_word();
                    break; /* 107 */
                case 108:
                    pos = parse_word();
                    break; /* 108 */
                case 109:
                    pos = parse_word();
                    break; /* 109 */
                case 110:
                    pos = parse_nqstring();
                    break; /* 110 */
                case 111:
                    pos = parse_word();
                    break; /* 111 */
                case 112:
                    pos = parse_word();
                    break; /* 112 */
                case 113:
                    pos = parse_qstring();
                    break; /* 113 */
                case 114:
                    pos = parse_word();
                    break; /* 114 */
                case 115:
                    pos = parse_word();
                    break; /* 115 */
                case 116:
                    pos = parse_word();
                    break; /* 116 */
                case 117:
                    pos = parse_ustring();
                    break; /* 117 */
                case 118:
                    pos = parse_word();
                    break; /* 118 */
                case 119:
                    pos = parse_word();
                    break; /* 119 */
                case 120:
                    pos = parse_xstring();
                    break; /* 120 */
                case 121:
                    pos = parse_word();
                    break; /* 121 */
                case 122:
                    pos = parse_word();
                    break; /* 122 */
                case 123:
                    pos = parse_char();
                    break; /* 123 */
                case 124:
                    pos = parse_operator2();
                    break; /* 124 */
                case 125:
                    pos = parse_char();
                    break; /* 125 */
                case 126:
                    pos = parse_operator1();
                    break; /* 126 */
                case 127:
                    pos = parse_white();
                    break; /* 127 */
                case 128:
                    pos = parse_word();
                    break; /* 128 */
                case 129:
                    pos = parse_word();
                    break; /* 129 */
                case 130:
                    pos = parse_word();
                    break; /* 130 */
                case 131:
                    pos = parse_word();
                    break; /* 131 */
                case 132:
                    pos = parse_word();
                    break; /* 132 */
                case 133:
                    pos = parse_word();
                    break; /* 133 */
                case 134:
                    pos = parse_word();
                    break; /* 134 */
                case 135:
                    pos = parse_word();
                    break; /* 135 */
                case 136:
                    pos = parse_word();
                    break; /* 136 */
                case 137:
                    pos = parse_word();
                    break; /* 137 */
                case 138:
                    pos = parse_word();
                    break; /* 138 */
                case 139:
                    pos = parse_word();
                    break; /* 139 */
                case 140:
                    pos = parse_word();
                    break; /* 140 */
                case 141:
                    pos = parse_word();
                    break; /* 141 */
                case 142:
                    pos = parse_word();
                    break; /* 142 */
                case 143:
                    pos = parse_word();
                    break; /* 143 */
                case 144:
                    pos = parse_word();
                    break; /* 144 */
                case 145:
                    pos = parse_word();
                    break; /* 145 */
                case 146:
                    pos = parse_word();
                    break; /* 146 */
                case 147:
                    pos = parse_word();
                    break; /* 147 */
                case 148:
                    pos = parse_word();
                    break; /* 148 */
                case 149:
                    pos = parse_word();
                    break; /* 149 */
                case 150:
                    pos = parse_word();
                    break; /* 150 */
                case 151:
                    pos = parse_word();
                    break; /* 151 */
                case 152:
                    pos = parse_word();
                    break; /* 152 */
                case 153:
                    pos = parse_word();
                    break; /* 153 */
                case 154:
                    pos = parse_word();
                    break; /* 154 */
                case 155:
                    pos = parse_word();
                    break; /* 155 */
                case 156:
                    pos = parse_word();
                    break; /* 156 */
                case 157:
                    pos = parse_word();
                    break; /* 157 */
                case 158:
                    pos = parse_word();
                    break; /* 158 */
                case 159:
                    pos = parse_word();
                    break; /* 159 */
                case 160:
                    pos = parse_white();
                    break; /* 160 */
                case 161:
                    pos = parse_word();
                    break; /* 161 */
                case 162:
                    pos = parse_word();
                    break; /* 162 */
                case 163:
                    pos = parse_word();
                    break; /* 163 */
                case 164:
                    pos = parse_word();
                    break; /* 164 */
                case 165:
                    pos = parse_word();
                    break; /* 165 */
                case 166:
                    pos = parse_word();
                    break; /* 166 */
                case 167:
                    pos = parse_word();
                    break; /* 167 */
                case 168:
                    pos = parse_word();
                    break; /* 168 */
                case 169:
                    pos = parse_word();
                    break; /* 169 */
                case 170:
                    pos = parse_word();
                    break; /* 170 */
                case 171:
                    pos = parse_word();
                    break; /* 171 */
                case 172:
                    pos = parse_word();
                    break; /* 172 */
                case 173:
                    pos = parse_word();
                    break; /* 173 */
                case 174:
                    pos = parse_word();
                    break; /* 174 */
                case 175:
                    pos = parse_word();
                    break; /* 175 */
                case 176:
                    pos = parse_word();
                    break; /* 176 */
                case 177:
                    pos = parse_word();
                    break; /* 177 */
                case 178:
                    pos = parse_word();
                    break; /* 178 */
                case 179:
                    pos = parse_word();
                    break; /* 179 */
                case 180:
                    pos = parse_word();
                    break; /* 180 */
                case 181:
                    pos = parse_word();
                    break; /* 181 */
                case 182:
                    pos = parse_word();
                    break; /* 182 */
                case 183:
                    pos = parse_word();
                    break; /* 183 */
                case 184:
                    pos = parse_word();
                    break; /* 184 */
                case 185:
                    pos = parse_word();
                    break; /* 185 */
                case 186:
                    pos = parse_word();
                    break; /* 186 */
                case 187:
                    pos = parse_word();
                    break; /* 187 */
                case 188:
                    pos = parse_word();
                    break; /* 188 */
                case 189:
                    pos = parse_word();
                    break; /* 189 */
                case 190:
                    pos = parse_word();
                    break; /* 190 */
                case 191:
                    pos = parse_word();
                    break; /* 191 */
                case 192:
                    pos = parse_word();
                    break; /* 192 */
                case 193:
                    pos = parse_word();
                    break; /* 193 */
                case 194:
                    pos = parse_word();
                    break; /* 194 */
                case 195:
                    pos = parse_word();
                    break; /* 195 */
                case 196:
                    pos = parse_word();
                    break; /* 196 */
                case 197:
                    pos = parse_word();
                    break; /* 197 */
                case 198:
                    pos = parse_word();
                    break; /* 198 */
                case 199:
                    pos = parse_word();
                    break; /* 199 */
                case 200:
                    pos = parse_word();
                    break; /* 200 */
                case 201:
                    pos = parse_word();
                    break; /* 201 */
                case 202:
                    pos = parse_word();
                    break; /* 202 */
                case 203:
                    pos = parse_word();
                    break; /* 203 */
                case 204:
                    pos = parse_word();
                    break; /* 204 */
                case 205:
                    pos = parse_word();
                    break; /* 205 */
                case 206:
                    pos = parse_word();
                    break; /* 206 */
                case 207:
                    pos = parse_word();
                    break; /* 207 */
                case 208:
                    pos = parse_word();
                    break; /* 208 */
                case 209:
                    pos = parse_word();
                    break; /* 209 */
                case 210:
                    pos = parse_word();
                    break; /* 210 */
                case 211:
                    pos = parse_word();
                    break; /* 211 */
                case 212:
                    pos = parse_word();
                    break; /* 212 */
                case 213:
                    pos = parse_word();
                    break; /* 213 */
                case 214:
                    pos = parse_word();
                    break; /* 214 */
                case 215:
                    pos = parse_word();
                    break; /* 215 */
                case 216:
                    pos = parse_word();
                    break; /* 216 */
                case 217:
                    pos = parse_word();
                    break; /* 217 */
                case 218:
                    pos = parse_word();
                    break; /* 218 */
                case 219:
                    pos = parse_word();
                    break; /* 219 */
                case 220:
                    pos = parse_word();
                    break; /* 220 */
                case 221:
                    pos = parse_word();
                    break; /* 221 */
                case 222:
                    pos = parse_word();
                    break; /* 222 */
                case 223:
                    pos = parse_word();
                    break; /* 223 */
                case 224:
                    pos = parse_word();
                    break; /* 224 */
                case 225:
                    pos = parse_word();
                    break; /* 225 */
                case 226:
                    pos = parse_word();
                    break; /* 226 */
                case 227:
                    pos = parse_word();
                    break; /* 227 */
                case 228:
                    pos = parse_word();
                    break; /* 228 */
                case 229:
                    pos = parse_word();
                    break; /* 229 */
                case 230:
                    pos = parse_word();
                    break; /* 230 */
                case 231:
                    pos = parse_word();
                    break; /* 231 */
                case 232:
                    pos = parse_word();
                    break; /* 232 */
                case 233:
                    pos = parse_word();
                    break; /* 233 */
                case 234:
                    pos = parse_word();
                    break; /* 234 */
                case 235:
                    pos = parse_word();
                    break; /* 235 */
                case 236:
                    pos = parse_word();
                    break; /* 236 */
                case 237:
                    pos = parse_word();
                    break; /* 237 */
                case 238:
                    pos = parse_word();
                    break; /* 238 */
                case 239:
                    pos = parse_word();
                    break; /* 239 */
                case 240:
                    pos = parse_word();
                    break; /* 240 */
                case 241:
                    pos = parse_word();
                    break; /* 241 */
                case 242:
                    pos = parse_word();
                    break; /* 242 */
                case 243:
                    pos = parse_word();
                    break; /* 243 */
                case 244:
                    pos = parse_word();
                    break; /* 244 */
                case 245:
                    pos = parse_word();
                    break; /* 245 */
                case 246:
                    pos = parse_word();
                    break; /* 246 */
                case 247:
                    pos = parse_word();
                    break; /* 247 */
                case 248:
                    pos = parse_word();
                    break; /* 248 */
                case 249:
                    pos = parse_word();
                    break; /* 249 */
                case 250:
                    pos = parse_word();
                    break; /* 250 */
                case 251:
                    pos = parse_word();
                    break; /* 251 */
                case 252:
                    pos = parse_word();
                    break; /* 252 */
                case 253:
                    pos = parse_word();
                    break; /* 253 */
                case 254:
                    pos = parse_word();
                    break; /* 254 */
                case 255:
                    pos = parse_word();
                    break; /* 255 */
                default: /* move on if not in standard ascii set */
                    pos = pos + 1;
                    break;
            }
            state.pos = pos;
            if (state.tokenvec[current].type != CHAR_NULL) {
                state.stats_tokens += 1;
                return true;
            }
        }
        return false;
    }

    /**
     * Parsers: Looks at current character in input String, makes sense of it and turns it into a token.
     */
    public int parse_white() {
        return state.pos + 1;
    }

    public int parse_operator1() {
        String s = state.s;
        int pos = state.pos;
        Token token = new Token(TYPE_OPERATOR, pos, 1, String.valueOf(s.charAt(pos)));
        state.tokenvec[state.current] = token;
        return pos + 1;
    }

    public int parse_other() {
        String s = state.s;
        int pos = state.pos;
        Token token = new Token(TYPE_UNKNOWN, pos, 1, String.valueOf(s.charAt(pos)));
        state.tokenvec[state.current] = token;
        return pos + 1;
    }

    public int parse_char() {
        String s = state.s;
        int pos = state.pos;
        Token token = new Token(s.charAt(pos), pos, 1, String.valueOf(s.charAt(pos)));
        state.tokenvec[state.current] = token;
        return pos + 1;
    }

    public int parse_eol_comment() {
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;

        /* first occurrence of '\n' starting from pos */
        int endpos = s.indexOf('\n', pos);
        if (endpos == -1) {
            Token token = new Token(TYPE_COMMENT, pos, slen - pos, s.substring(pos));
            state.tokenvec[state.current] = token;
            return slen;
        } else {
            /*
             * tokenize from pos to endpos - 1. example: if "abc--\n" then tokenize "--"
             */
            Token token = new Token(TYPE_COMMENT, pos, endpos - pos, s.substring(pos, endpos));
            state.tokenvec[state.current] = token;
            return endpos + 1;
        }
    }

    /*
     * In ANSI mode, hash is an operator In MYSQL mode, it's a EOL comment like '--'
     */
    public int parse_hash() {
        state.stats_comment_hash += 1;
        if ((state.flags & FLAG_SQL_MYSQL) != 0) {
            state.stats_comment_hash += 1;
            return parse_eol_comment();
        } else {
            Token token = new Token(TYPE_OPERATOR, state.pos, 1, "#");
            state.tokenvec[state.current] = token;
            return state.pos + 1;
        }
    }

    public int parse_dash() {
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;
        /*
         * five cases: 1) --[white] this is always a SQL comment 2) --[EOF] this is a comment 3)
         * --[notwhite] in MySQL this is NOT a comment but two unary operators 4) --[notwhite] everyone else
         * thinks this is a comment 5) -[not dash] '-' is a unary operator
         */
        if (pos + 2 < slen && s.charAt(pos + 1) == '-' && char_is_white(s.charAt(pos + 2))) {
            return parse_eol_comment();
        } else if (pos + 2 == slen && s.charAt(pos + 1) == '-') {
            return parse_eol_comment();
        } else if (pos + 1 < slen && s.charAt(pos + 1) == '-' && (state.flags & FLAG_SQL_ANSI) != 0) {
            state.stats_comment_ddx += 1;
            return parse_eol_comment();
        } else {
            Token token = new Token(TYPE_OPERATOR, pos, 1, String.valueOf('-'));
            state.tokenvec[state.current] = token;
            return pos + 1;
        }
    }

    public int parse_slash() {
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;

        /* not a comment */
        if (pos + 1 == slen || s.charAt(pos + 1) != '*') {
            return parse_operator1();
        }

        /* is a comment */
        int clen;
        int ctype = TYPE_COMMENT;
        int cend = s.indexOf("*/", pos + 2); // index of * in */ (we do pos + 2 to skip over /*)
        boolean closed = cend != -1;

        if (!closed) {
            clen = slen - pos;
            cend = slen - 2;
        } else {
            clen = (cend + 2) - pos;
        }

        /*
         * postgresql allows nested comments which makes this is incompatible with parsing so if we find a
         * '/x' inside the comment, then make a new token.
         *
         * Also, Mysql's "conditional" comments for version are an automatic black ban!
         */
        if (closed && s.substring(pos + 2, cend + 2).contains("/*")) {
            ctype = TYPE_EVIL;
        } else if (is_mysql_comment(s, slen, pos)) {
            ctype = TYPE_EVIL;
        }

        Token token = new Token(ctype, pos, clen, s.substring(pos, cend + 2));
        state.tokenvec[state.current] = token;
        return pos + clen;
    }

    public int parse_backslash() {
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;

        /*
         * Weird MySQL alias for NULL, "\N" (capital N only)
         */
        if (pos + 1 < slen && s.charAt(pos + 1) == 'N') {
            Token token = new Token(TYPE_NUMBER, pos, 2, s.substring(pos, pos + 2));
            state.tokenvec[state.current] = token;
            return pos + 2;
        } else {
            Token token = new Token(TYPE_BACKSLASH, pos, 1, String.valueOf(s.charAt(pos)));
            state.tokenvec[state.current] = token;
            return pos + 1;
        }
    }

    public int parse_operator2() {
        Character ch;
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;

        /* single operator at end of line */
        if (pos + 1 >= slen) {
            return parse_operator1();
        }

        /* "<=>" */
        if (pos + 2 < slen && s.charAt(pos) == '<'
                && s.charAt(pos + 1) == '='
                && s.charAt(pos + 2) == '>') {
            /*
             * special 3-char operator
             */
            Token token = new Token(TYPE_OPERATOR, pos, 3, "<=>");
            state.tokenvec[state.current] = token;
            return pos + 3;
        }

        /* 2-char operators: "-=", "+=", "!!", ":=", etc... */
        String operator = s.substring(pos, pos + 2);
        ch = libinjection_sqli_lookup_word(operator);
        if (ch != null) {
            state.tokenvec[state.current] = new Token(ch, pos, 2, operator);
            return pos + 2;
        }

        if (s.charAt(pos) == ':') {
            /* ':' alone is not an operator */
            state.tokenvec[state.current] = new Token(TYPE_COLON, pos, 1, ":");
            return pos + 1;
        } else {
            /* must be a 1-char operator */
            return parse_operator1();
        }
    }

    /*
     * Look forward for doubling of delimiter
     *
     * case 'foo''bar' --> foo''bar
     *
     * ending quote isn't duplicated (i.e. escaped) since it's the wrong char or EOL
     *
     */
    public int parse_string_core(char delim, int offset) {
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;
        int qpos = s.indexOf(delim, pos + offset); /* offset to skip first quote */
        /* real quote if offset > 0, simulated quote if not */
        char str_open = (offset > 0) ? delim : CHAR_NULL;

        while (true) {
            if (qpos == -1) {
                /* string ended with no trailing quote. add token */
                Token token = new Token(TYPE_STRING, pos + offset, slen - pos - offset, s.substring(pos + offset));
                token.str_open = str_open;
                token.str_close = CHAR_NULL;
                state.tokenvec[state.current] = token;
                return slen;
            } else if (is_backslash_escaped(qpos - 1, pos + offset, s)) {
                qpos = s.indexOf(delim, qpos + 1);
                continue;
            } else if (is_double_delim_escaped(qpos, slen, s)) {
                qpos = s.indexOf(delim, qpos + 2);
                continue;
            } else {
                /* quote is closed: it's a normal string */
                Token token = new Token(TYPE_STRING, pos + offset, qpos - (pos + offset),
                        s.substring(pos + offset, qpos));
                token.str_open = str_open;
                token.str_close = delim;
                state.tokenvec[state.current] = token;
                return qpos + 1;
            }
        }
    }

    /* Used when first char is ' or " */
    public int parse_string() {
        return parse_string_core(state.s.charAt(state.pos), 1);
    }

    /*
     * Used when first char is E : psql "Escaped String"
     */
    public int parse_estring() {
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;

        if (pos + 2 >= slen || s.charAt(pos + 1) != CHAR_SINGLE) {
            return parse_word();
        }
        return parse_string_core(CHAR_SINGLE, 2);
    }

    /*
     * Used when first char is N or n: mysql "National Character set"
     */
    public int parse_ustring() {
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;

        if (pos + 2 < slen && s.charAt(pos + 1) == '&' && s.charAt(pos + 2) == '\'') {
            state.pos = state.pos + 2;
            pos = parse_string();
            state.tokenvec[state.current].str_open = 'u';
            if (state.tokenvec[state.current].str_close == '\'') {
                state.tokenvec[state.current].str_close = 'u';
            }
            return pos;
        } else {
            return parse_word();
        }

    }

    public int parse_qstring_core(int offset) {
        char ch;
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos + offset;

        /*
         * if we are already at end of string.. if current char is not q or Q if we don't have 2 more chars
         * if char2 != a single quote then, just treat as word
         */
        if (pos >= slen
                || (s.charAt(pos) != 'q' && s.charAt(pos) != 'Q')
                || pos + 2 >= slen
                || s.charAt(pos + 1) != '\'') {
            return parse_word();
        }
        ch = s.charAt(pos + 2);

        /*
         * the ch > 127 is un-needed since we assume char is signed
         */
        if (ch < 33 /* || ch > 127 */) {
            return parse_word();
        }
        switch (ch) {
            case '(':
                ch = ')';
                break;
            case '[':
                ch = ']';
                break;
            case '{':
                ch = '}';
                break;
            case '<':
                ch = '>';
                break;
        }

        /* find )' or ]' or }' or >' */
        String find = String.valueOf(ch) + String.valueOf('\'');

        int found = s.indexOf(find, pos + 3);
        if (found == -1) {
            Token token = new Token(TYPE_STRING, pos + 3, slen - pos - 3, s.substring(pos + 3));
            token.str_open = 'q';
            token.str_close = CHAR_NULL;
            state.tokenvec[state.current] = token;
            return slen;
        } else {
            Token token = new Token(TYPE_STRING, pos + 3, found - pos - 3, s.substring(pos + 3, found));
            token.str_open = 'q';
            token.str_close = 'q';
            state.tokenvec[state.current] = token;
            return found + 2; /* +2 to skip over )' or ]' or }' or >' */
        }

    }

    /*
     * Oracle's q string
     */
    public int parse_qstring() {
        return parse_qstring_core(0);
    }

    /*
     * mysql's N'STRING' or ... Oracle's nq string
     */
    public int parse_nqstring() {
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;
        if (pos + 2 < slen && s.charAt(pos + 1) == CHAR_SINGLE) {
            return parse_estring();
        }
        return parse_qstring_core(1);
    }

    /*
     * binary literal string re: [bB]'[01]*'
     */
    public int parse_bstring() {
        int wlen;
        String s = state.s;
        int pos = state.pos;
        int slen = state.slen;

        /*
         * need at least 2 more characters if next char isn't a single quote, then continue as normal word
         */
        if (pos + 2 >= slen || s.charAt(pos + 1) != '\'') {
            return parse_word();
        }
        wlen = strlenspn(s.substring(pos + 2), "01");

        /*
         * if [01]* pattern not found, or the pattern did not close with a single quote
         */
        if (pos + 2 + wlen >= slen || s.charAt(pos + 2 + wlen) != '\'') {
            return parse_word();
        }

        /* +3 for [bB], starting quote, ending quote */
        Token token = new Token(TYPE_NUMBER, pos, wlen + 3, s.substring(pos, pos + wlen + 3));
        state.tokenvec[state.current] = token;
        return pos + 2 + wlen + 1;
    }

    /*
     * hex literal string re: [xX]'[0123456789abcdefABCDEF]*' mysql has requirement of having EVEN
     * number of chars, but pgsql does not
     */
    public int parse_xstring() {
        int wlen;
        String s = state.s;
        int pos = state.pos;
        int slen = state.slen;

        /*
         * need at least 2 more characters if next char isn't a single quote, then continue as normal word
         */
        if (pos + 2 >= slen || s.charAt(pos + 1) != '\'') {
            return parse_word();
        }
        wlen = strlenspn(s.substring(pos + 2), "0123456789abcdefABCDEF");

        /*
         * if [0123456789abcdefABCDEF]* pattern not found, or the pattern did not close with a single quote
         */
        if (pos + 2 + wlen >= slen || s.charAt(pos + 2 + wlen) != '\'') {
            return parse_word();
        }

        /* +3 for [xX], starting quote, ending quote */
        Token token = new Token(TYPE_NUMBER, pos, wlen + 3, s.substring(pos, pos + wlen + 3));
        state.tokenvec[state.current] = token;
        return pos + 2 + wlen + 1;
    }

    /*
     * This handles MS SQLSERVER bracket words
     * http://stackoverflow.com/questions/3551284/sql-serverwhat-do-brackets- mean-around-column-name
     */
    public int parse_bword() {
        String s = state.s;
        int pos = state.pos;
        int slen = state.slen;
        int endptr = s.indexOf(']', pos);
        if (endptr == -1) {
            Token token = new Token(TYPE_BAREWORD, pos, slen - pos, s.substring(pos));
            state.tokenvec[state.current] = token;
            return state.slen;
        } else {
            Token token = new Token(TYPE_BAREWORD, pos, endptr + 1 - pos, s.substring(pos, endptr + 1));
            state.tokenvec[state.current] = token;
            return endptr + 1;
        }
    }

    public int parse_word() {
        Character wordtype;
        char delim;
        String s = state.s;
        int pos = state.pos;

        String unaccepted = " []{}<>:\\?=@!#~+-*/&|^%(),';\t\n\f\r\"\240\000\u000b"; // \u000b is vertical tab
        String str = s.substring(pos);
        int wlen = strlencspn(str, unaccepted);
        String word = s.substring(pos, pos + wlen);

        Token token = new Token(TYPE_BAREWORD, pos, wlen, word);
        state.tokenvec[state.current] = token;

        /*
         * look for characters before "." and "`" and see if they're keywords
         */
        for (int i = 0; i < token.len; i++) {
            delim = token.val.charAt(i);
            if (delim == '.' || delim == '`') {
                wordtype = libinjection_sqli_lookup_word(word.substring(0, i));
                if (wordtype != null && wordtype != TYPE_NONE && wordtype != TYPE_BAREWORD) {
                    /*
                     * we got something like "SELECT.1" or SELECT`column`
                     */
                    state.tokenvec[state.current] = new Token(wordtype, pos, i, word.substring(0, i));
                    return pos + i;
                }
            }
        }

        /*
         * do normal lookup with word including '.'
         */
        wordtype = libinjection_sqli_lookup_word(token.val);
        /*
         * before, we differentiated fingerprint lookups from word lookups by adding a 0 to the front for
         * fingerprint lookups. now, just check if word we found was a fingerprint
         */
        if (wordtype == null || wordtype == 'F') {
            wordtype = TYPE_BAREWORD;
        }
        state.tokenvec[state.current].type = (char) wordtype;

        return pos + wlen;
    }

    public int parse_tick() {
        int pos = parse_string_core(CHAR_TICK, 1);

        /*
         * we could check to see if start and end of of string are both "`", i.e. make sure we have matching
         * set. `foo` vs. `foo but I don't think it matters much
         */

        /*
         * check value of string to see if it's a keyword, function, operator, etc
         */
        Character wordtype = libinjection_sqli_lookup_word(state.tokenvec[state.current].val);
        if (wordtype != null && wordtype == TYPE_FUNCTION) {
            /* if it's a function, then convert to token */
            state.tokenvec[state.current].type = TYPE_FUNCTION;
        } else {
            /*
             * otherwise it's a 'n' type -- mysql treats everything as a bare word
             */
            state.tokenvec[state.current].type = TYPE_BAREWORD;
        }
        return pos;
    }

    public int parse_var() {
        int xlen;
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos + 1;

        /*
         * var_count is only used to reconstruct the input. It counts the number of '@' seen 0 in the case
         * of NULL, 1 or 2
         */

        /*
         * move past optional other '@'
         */
        if (pos < slen && s.charAt(pos) == '@') {
            pos += 1;
            state.tokenvec[state.current].count = 2;
        } else {
            state.tokenvec[state.current].count = 1;
        }

        /*
         * MySQL allows @@`version`
         */
        if (pos < slen) {
            if (s.charAt(pos) == '`') {
                state.pos = pos;
                pos = parse_tick();
                state.tokenvec[state.current].type = TYPE_VARIABLE;
                return pos;
            } else if (s.charAt(pos) == CHAR_SINGLE || s.charAt(pos) == CHAR_DOUBLE) {
                state.pos = pos;
                pos = parse_string();
                state.tokenvec[state.current].type = TYPE_VARIABLE;
                return pos;
            }
        }

        xlen = strlencspn(s.substring(pos), " <>:\\?=@!#~+-*/&|^%(),';\t\n\u000b\f\r'`\"");
        if (xlen == 0) {
            Token token = new Token(TYPE_VARIABLE, pos, 0, "");
            state.tokenvec[state.current] = token;
            return pos;
        } else {
            Token token = new Token(TYPE_VARIABLE, pos, xlen, s.substring(pos, pos + xlen));
            state.tokenvec[state.current] = token;
            return pos + xlen;
        }
    }

    public int parse_money() {
        int xlen;
        int strend;
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;

        if (pos + 1 == slen) {
            /* end of line */
            state.tokenvec[state.current] = new Token(TYPE_BAREWORD, pos, 1, "$");
            return slen;
        }

        /*
         * $1,000.00 or $1.000,00 ok! This also parses $....,,,111 but that's ok
         */
        xlen = strlenspn(s.substring(pos + 1), "0123456789.,");
        if (xlen == 0) {
            if (s.charAt(pos + 1) == '$') {
                /* we have $$ .. find ending $$ and make string */
                strend = s.indexOf("$$", pos + 2);
                if (strend == -1) {
                    /* fell off edge: $$ not found */
                    Token token = new Token(TYPE_STRING, pos + 2, slen - (pos + 2), s.substring(pos + 2));
                    token.str_open = '$';
                    token.str_close = CHAR_NULL;
                    state.tokenvec[state.current] = token;
                    return slen;
                } else {
                    Token token = new Token(TYPE_STRING, pos + 2, strend - (pos + 2), s.substring(pos + 2, strend));
                    token.str_open = '$';
                    token.str_close = '$';
                    state.tokenvec[state.current] = token;
                    return strend + 2;
                }
            } else {
                /* it's not '$$', but maybe it's pgsql "$ quoted strings" */
                xlen = strlenspn(s.substring(pos + 1), "abcdefghjiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
                if (xlen == 0) {
                    /* hmm it's "$" _something_ .. just add $ and keep going */
                    Token token = new Token(TYPE_BAREWORD, pos, 1, "$");
                    state.tokenvec[state.current] = token;
                    return pos + 1;
                } else {
                    /* we have $foobar????? */
                    /* is it $foobar$ */
                    if (pos + xlen + 1 == slen || s.charAt(pos + xlen + 1) != '$') {
                        /* not $foobar$, or fell off edge */
                        Token token = new Token(TYPE_BAREWORD, pos, 1, "$");
                        state.tokenvec[state.current] = token;
                        return pos + 1;
                    }

                    /* we have $foobar$... find it again */
                    strend = s.indexOf(s.substring(pos, pos + xlen + 2), pos + xlen + 2);

                    if (strend == -1 || strend < pos + xlen + 2) {
                        /* fell off edge */
                        Token token = new Token(TYPE_STRING, pos + xlen + 2, slen - pos - xlen - 2,
                                s.substring(pos + xlen + 2));
                        token.str_open = '$';
                        token.str_close = CHAR_NULL;
                        state.tokenvec[state.current] = token;
                        return slen;
                    } else {
                        /*
                         * got one. we're looking in between $foobar$__________$foobar$
                         */
                        Token token = new Token(TYPE_STRING, pos + xlen + 2, strend - pos - xlen - 2,
                                s.substring(pos + xlen + 2, strend));
                        token.str_open = '$';
                        token.str_close = '$';
                        state.tokenvec[state.current] = token;
                        return strend + xlen + 2;
                    }
                }
            }
        } else if (xlen == 1 && s.charAt(pos + 1) == '.') {
            /* $. should be parsed as a word */
            return parse_word();
        } else {
            Token token = new Token(TYPE_NUMBER, pos, 1 + xlen, s.substring(pos, pos + xlen + 1));
            state.tokenvec[state.current] = token;
            return pos + xlen + 1;
        }
    }

    public int parse_number() {
        int xlen;
        int start;
        String digits = null;
        String s = state.s;
        int slen = state.slen;
        int pos = state.pos;
        boolean have_e = false;
        boolean have_exp = false;

        /*
         * s.charAt(pos) == '0' has 1/10 chance of being true, while pos+1< slen is almost always true
         */
        if (s.charAt(pos) == '0' && pos + 1 < slen) {
            if (s.charAt(pos + 1) == 'X' || s.charAt(pos + 1) == 'x') {
                digits = "0123456789ABCDEFabcdef";
            } else if (s.charAt(pos + 1) == 'B' || s.charAt(pos + 1) == 'b') {
                digits = "01";
            }

            if (digits != null) {
                xlen = strlenspn(s.substring(pos + 2), digits);
                if (xlen == 0) {
                    Token token = new Token(TYPE_BAREWORD, pos, 2, "0" + s.charAt(pos + 1));
                    state.tokenvec[state.current] = token;
                    return pos + 2;
                } else {
                    Token token = new Token(TYPE_NUMBER, pos, 2 + xlen, s.substring(pos, pos + 1 + xlen + 1));
                    state.tokenvec[state.current] = token;
                    return pos + 1 + xlen + 1;
                }
            }
        }

        start = pos;
        while (pos < slen && Character.isDigit(s.charAt(pos))) {
            pos += 1;
        }

        /* number sequence reached a '.' */
        if (pos < slen && s.charAt(pos) == '.') {
            pos += 1;
            /* keep going since it might be decimal */
            while (pos < slen && Character.isDigit(s.charAt(pos))) {
                pos += 1;
            }
            if (pos - start == 1) {
                /* only one character '.' read so far */
                state.tokenvec[state.current] = new Token(TYPE_DOT, start, 1, "");
                return pos;
            }
        }

        if (pos < slen) {
            if (s.charAt(pos) == 'E' || s.charAt(pos) == 'e') {
                have_e = true;
                pos += 1;
                if (pos < slen && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                    pos += 1;
                }
                while (pos < slen && Character.isDigit(s.charAt(pos))) {
                    have_exp = true;
                    pos += 1;
                }
            }
        }

        /*
         * oracle's ending float or double suffix
         * http://docs.oracle.com/cd/B19306_01/server.102/b14200/sql_elements003 .htm#i139891
         */
        if (pos < slen
                && (s.charAt(pos) == 'd' || s.charAt(pos) == 'D' || s.charAt(pos) == 'f' || s.charAt(pos) == 'F')) {
            if (pos + 1 == slen) {
                /* line ends evaluate "... 1.2f$" as '1.2f' */
                pos += 1;
            } else if ((char_is_white(s.charAt(pos + 1)) || s.charAt(pos + 1) == ';')) {
                /*
                 * easy case, evaluate "... 1.2f ... as '1.2f'
                 */
                pos += 1;
            } else if (s.charAt(pos + 1) == 'u' || s.charAt(pos + 1) == 'U') {
                /*
                 * a bit of a hack but makes '1fUNION' parse as '1f UNION'
                 */
                pos += 1;
            }
        }

        if (have_e && !have_exp) {
            /*
             * very special form of "1234.e" "10.10E" ".E" this is a WORD not a number!!
             */
            state.tokenvec[state.current] = new Token(TYPE_BAREWORD, start, pos - start, s.substring(start, pos));
        } else {
            state.tokenvec[state.current] = new Token(TYPE_NUMBER, start, pos - start, s.substring(start, pos));
        }
        return pos;
    }


    /**
     * Helper Functions
     */

    /*
     * See if two tokens can be merged since they are compound SQL phrases.
     *
     * This takes two tokens, and, if they are the right type, merges their values together. Then checks
     * to see if the new value is special using the PHRASES mapping.
     *
     * Example: "UNION" + "ALL" ==> "UNION ALL"
     *
     * C Security Notes: this is safe to use C-strings (null-terminated) since the types involved by
     * definition do not have embedded nulls (e.g. there is no keyword with embedded null)
     *
     * Porting Notes: since this is C, it's oddly complicated. This is just: multikeywords[token.value +
     * ' ' + token2.value]
     *
     */
    public boolean syntax_merge_words(Token a, int apos, Token b, int bpos) {
        String merged;
        Character wordtype;

        /* first token must not represent any of these types */
        if (!(a.type == TYPE_KEYWORD
                || a.type == TYPE_BAREWORD
                || a.type == TYPE_OPERATOR
                || a.type == TYPE_UNION
                || a.type == TYPE_FUNCTION
                || a.type == TYPE_EXPRESSION
                || a.type == TYPE_SQLTYPE)) {
            return false;
        }

        /* second token must not represent any of these types */
        if (b.type != TYPE_KEYWORD && b.type != TYPE_BAREWORD
                && b.type != TYPE_OPERATOR
                && b.type != TYPE_SQLTYPE
                && b.type != TYPE_LOGIC_OPERATOR
                && b.type != TYPE_FUNCTION
                && b.type != TYPE_UNION
                && b.type != TYPE_EXPRESSION) {
            return false;
        }

        merged = a.val + " " + b.val;
        wordtype = libinjection_sqli_lookup_word(merged);

        if (wordtype != null) {
            Token token = new Token(wordtype, a.pos, merged.length(), merged);
            state.tokenvec[apos] = token;
            /* shift down all tokens after b by one index */
            for (int i = bpos; i < state.tokenvec.length - 1; i++) {
                if (state.tokenvec[i] != null) {
                    state.tokenvec[i] = state.tokenvec[i + 1];
                } else {
                    break;
                }
            }
            state.tokenvec[7] = null;
            return true;
        } else {
            return false;
        }

    }

    public boolean token_is_unary_op(Token token) {
        String str = token.val;
        int len = token.len;

        if (token.type != TYPE_OPERATOR) {
            return false;
        }

        switch (len) {
            case 1:
                return str.charAt(0) == '+' || str.charAt(0) == '-' || str.charAt(0) == '!' || str.charAt(0) == '~';
            case 2:
                return str.charAt(0) == '!' && str.charAt(1) == '!';
            case 3:
                return str.toUpperCase().equals("NOT");
            default:
                return false;
        }
    }

    public boolean token_is_arithmetic_op(Token token) {
        if (token.len == 1 && token.type == TYPE_OPERATOR) {
            char ch = token.val.charAt(0);
            return (ch == '*' || ch == '/' || ch == '-' || ch == '+' || ch == '%');
        }
        return false;
    }

    public boolean char_is_white(char ch) {
        /*
         * ' ' space is 0x20 '\t 0x09 \011 horizontal tab '\n' 0x0a \012 new line '\v' 0x0b \013 vertical
         * tab '\f' 0x0c \014 new page '\r' 0x0d \015 carriage return 0x00 \000 null (oracle) 0xa0 \240 is
         * Latin-1
         */

        switch ((int) ch) {
            case 0x20:
                return true;
            case 0x09:
                return true;
            case 0x0a:
                return true;
            case 0x0b:
                return true;
            case 0x0c:
                return true;
            case 0x0d:
                return true;
            case 0x00:
                return true;
            case 0xa0:
                return true;
            default:
                return false;
        }
    }

    /*
     * This detects MySQL comments, comments that start with /x! We just ban these now but previously we
     * attempted to parse the inside
     *
     * For reference: the form of /x![anything]x/ or /x!12345[anything] x/
     *
     * Mysql 3 (maybe 4), allowed this: /x!0selectx/ 1; where 0 could be any number.
     *
     * The last version of MySQL 3 was in 2003. It is unclear if the MySQL 3 syntax was allowed in MySQL
     * 4. The last version of MySQL 4 was in 2008
     *
     */
    public boolean is_mysql_comment(String s, int len, int pos) {
        /*
         * so far... s.charAt(pos) == '/' && s.charAt(pos+1) == '*'
         */

        if (pos + 2 >= len) {
            /* not a mysql comment */
            return false;
        }

        if (s.charAt(pos + 2) != '!') {
            /* not a mysql comment */
            return false;
        }

        /*
         * this is a mysql comment got "/x!"
         */
        return true;
    }

    /*
     * "  \"   " one backslash = escaped! " \\"   " two backslash = not escaped! "\\\"   " three
     * backslash = escaped!
     */
    public boolean is_backslash_escaped(int end, int start, String s) {
        int i = end;

        while (i >= start) {
            if (s.charAt(i) != '\\') {
                break;
            }
            i--;
        }

        return ((end - i) & 1) == 1;
    }

    public boolean is_double_delim_escaped(int cur, int end, String s) {
        return ((cur + 1) < end) && (s.charAt(cur + 1) == s.charAt(cur));
    }

    public char flag2delim(int flag) {
        if ((flag & FLAG_QUOTE_SINGLE) != 0) {
            return CHAR_SINGLE;
        } else if ((flag & FLAG_QUOTE_DOUBLE) != 0) {
            return CHAR_DOUBLE;
        } else {
            return CHAR_NULL;
        }
    }

    public int strlenspn(String s, String accept) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (accept.indexOf(s.charAt(i)) == -1) {
                return i;
            }
        }
        return len;
    }

    public int strlencspn(String s, String unaccepted) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (unaccepted.indexOf(s.charAt(i)) != -1) {
                return i;
            }
        }
        return s.length();
    }
}

