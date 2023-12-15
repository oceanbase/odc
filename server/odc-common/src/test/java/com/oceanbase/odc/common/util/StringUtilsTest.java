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
package com.oceanbase.odc.common.util;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.test.tool.TestCollections;

public class StringUtilsTest {
    @Test
    public void equals_NullToNull_ReturnTrue() {
        boolean result = StringUtils.equals(null, null);
        Assert.assertTrue(result);
    }

    @Test
    public void equals_EmptyToNull_ReturnFalse() {
        boolean result = StringUtils.equals("", null);
        Assert.assertFalse(result);
    }

    @Test
    public void equals_SameString_ReturnTrue() {
        boolean result = StringUtils.equals("s1", "s1");
        Assert.assertTrue(result);
    }

    @Test
    public void equals_DifferentString_ReturnFalse() {
        boolean result = StringUtils.equals("s1", "s2");
        Assert.assertFalse(result);
    }

    @Test
    public void quoteSqlIdentifier_Null_Null() {
        String result = StringUtils.quoteSqlIdentifier(null, '`');
        Assert.assertNull(result);;
    }

    @Test
    public void quoteSqlIdentifier_Empty_Wrapped() {
        String result = StringUtils.quoteSqlIdentifier("", '`');
        Assert.assertEquals("``", result);;
    }

    @Test
    public void quoteSqlIdentifier_ValueContainsWrap_Double() {
        String result = StringUtils.quoteSqlIdentifier("`", '`');
        Assert.assertEquals("````", result);;
    }

    @Test
    public void quoteOracleValue_SingleQuote_Doubled() {
        String result = StringUtils.quoteOracleValue("'");
        Assert.assertEquals("''''", result);;
    }

    @Test
    public void quoteOracleValue_DoubleQuote_Skipped() {
        String result = StringUtils.quoteOracleValue("\"");
        Assert.assertEquals("'\"'", result);;
    }

    @Test
    public void quoteMysqlValue_SingleQuoteAndSlash_Doubled() {
        String result = StringUtils.quoteMysqlValue("'\\");
        Assert.assertEquals("'''\\\\'", result);;
    }

    @Test
    public void quoteMysqlValue_DoubleQuote_Skipped() {
        String result = StringUtils.quoteMysqlValue("\"");
        Assert.assertEquals("'\"'", result);;
    }

    @Test
    public void unquoteOracleValue_SingleQuote_Doubled() {
        String result = StringUtils.unquoteOracleValue("''''");
        Assert.assertEquals("'", result);;
    }

    @Test
    public void unquoteOracleValue_DoubleQuote_Skipped() {
        String result = StringUtils.unquoteOracleValue("'\"'");
        Assert.assertEquals("\"", result);;
    }

    @Test
    public void unquoteMysqlValue_SingleQuoteAndSlash_Doubled() {
        String result = StringUtils.unquoteMysqlValue("'''\\\\'");
        Assert.assertEquals("'\\", result);;
    }

    @Test
    public void unquoteMysqlValue_DoubleQuote_Skipped() {
        String result = StringUtils.unquoteMysqlValue("'\"'");
        Assert.assertEquals("\"", result);;
    }

    @Test
    public void unquoteMysqlValue_Alpha_Unwrap() {
        String result = StringUtils.unquoteMysqlValue("'abc'");
        Assert.assertEquals("abc", result);;
    }

    @Test
    public void unquoteMysqlValue_Null_Null() {
        String result = StringUtils.unquoteMysqlValue(null);
        Assert.assertNull(result);
    }

    @Test
    public void unquoteMysqlValue_Empty_Empty() {
        String result = StringUtils.unquoteMysqlValue("");
        Assert.assertEquals("", result);
    }

    @Test
    public void unquoteMysqlValue_SingleChar_SingleChar() {
        String result = StringUtils.unquoteMysqlValue("'");
        Assert.assertEquals("'", result);
    }

    @Test
    public void unquoteSqlIdentifier_Null_Null() {
        String result = StringUtils.unquoteSqlIdentifier(null, '`');
        Assert.assertNull(result);;
    }

    @Test
    public void unquoteSqlIdentifier_Empty_DoNothing() {
        String result = StringUtils.unquoteSqlIdentifier("", '`');
        Assert.assertEquals("", result);;
    }

    @Test
    public void unquoteSqlIdentifier_EmptyWrapped_Unwrap() {
        String result = StringUtils.unquoteSqlIdentifier("``", '`');
        Assert.assertEquals("", result);;
    }

    @Test
    public void unquoteSqlIdentifier_ValueContainsWrap_Single() {
        String result = StringUtils.unquoteSqlIdentifier("````", '`');
        Assert.assertEquals("`", result);;
    }

    @Test
    public void unquoteSqlIdentifier_NoQuote_NoEscape() {
        String result = StringUtils.unquoteSqlIdentifier("a``b", '`');
        Assert.assertEquals("a``b", result);;
    }

    @Test
    public void singleLine_Empty_Empty() {
        String s = StringUtils.singleLine("");
        Assert.assertEquals("", s);
    }

    @Test
    public void singleLine_WindowsLineBreaker_Blank() {
        String s = StringUtils.singleLine("a\r\nb");
        Assert.assertEquals("a b", s);
    }

    @Test
    public void singleLine_LinuxLineBreaker_Blank() {
        String s = StringUtils.singleLine("a\nb");
        Assert.assertEquals("a b", s);
    }

    @Test
    public void singleLine_MacLineBreaker_Blank() {
        String s = StringUtils.singleLine("a\rb");
        Assert.assertEquals("a b", s);
    }

    @Test
    public void singleLine_MultipleBlank_Blank() {
        String s = StringUtils.singleLine("a    \t b");
        Assert.assertEquals("a b", s);
    }

    @Test
    public void escapeLike() {
        String escape = StringUtils.escapeLike("aa%bb%cc_dd_ee");
        Assert.assertEquals("aa\\%bb\\%cc\\_dd\\_ee", escape);
    }

    @Test
    public void escapeLikeEmpty() {
        String escape = StringUtils.escapeLike("");
        Assert.assertEquals("", escape);
    }

    @Test
    public void replaceVariables() {
        String replaced = StringUtils.replaceVariables("a${hello}b", TestCollections.asMap("hello=world"));
        Assert.assertEquals("aworldb", replaced);
    }

    @Test
    public void testUnquoteMysqlIdentifier() {
        String result = StringUtils.unquoteMySqlIdentifier("`param_name`");
        Assert.assertEquals("param_name", result);

        result = StringUtils.unquoteMySqlIdentifier("``param_name``");
        Assert.assertEquals("`param_name`", result);
    }

    @Test
    public void testUnquoteOracleIdentifier() {
        String result = StringUtils.unquoteOracleIdentifier("\"param_name\"");
        Assert.assertEquals("param_name", result);

        result = StringUtils.unquoteOracleIdentifier("\"\"param_name\"\"");
        Assert.assertEquals("\"param_name\"", result);
    }

    @Test
    public void testQuoteMysqlIdentifier() {
        String result = StringUtils.quoteMysqlIdentifier("");
        Assert.assertEquals("``", result);

        result = StringUtils.quoteMysqlIdentifier("abc");
        Assert.assertEquals("`abc`", result);

        result = StringUtils.quoteMysqlIdentifier("a`cd");
        Assert.assertEquals("`a``cd`", result);
    }

    @Test
    public void testQuoteOracleIdentifier() {
        String result = StringUtils.quoteOracleIdentifier("");
        Assert.assertEquals("\"\"", result);

        result = StringUtils.quoteOracleIdentifier("abc");
        Assert.assertEquals("\"abc\"", result);

        result = StringUtils.quoteOracleIdentifier("a\"cd");
        Assert.assertEquals("\"a\"\"cd\"", result);
    }

    @Test
    public void testBitString() {
        byte[] masks = {-83, 111, 70, 97, -80, -60, 120, 91};
        StringBuilder builder = new StringBuilder();
        for (byte m : masks) {
            builder.append(StringUtils.getBitString(m));
        }
        Assert.assertEquals("1010110101101111010001100110000110110000110001000111100001011011", builder.toString());
    }

    @Test
    public void urlEncode_no_special_expect_no_change() {
        Assert.assertEquals("www.domain.com", StringUtils.urlEncode("www.domain.com"));
    }

    @Test
    public void urlEncode_zh_CN() {
        Assert.assertEquals("%E4%B8%AD%E6%96%87%E6%96%87%E5%AD%97%E6%B5%8B%E8%AF%95",
                StringUtils.urlEncode("中文文字测试"));
    }

    @Test
    public void urlEncode_OffsetDateTime() {
        Assert.assertEquals("2020-02-20T11%3A57%3A58.231Z",
                StringUtils.urlEncode("2020-02-20T11:57:58.231Z"));
    }

    @Test
    public void urlDecode_no_special_expect_no_change() {
        Assert.assertEquals("www.domain.com", StringUtils.urlDecode("www.domain.com"));
    }

    @Test
    public void urlDecode_zh_CN() {
        Assert.assertEquals("中文文字测试",
                StringUtils.urlDecode("%e4%b8%ad%e6%96%87%e6%96%87%e5%ad%97%e6%b5%8b%e8%af%95"));
    }

    @Test
    public void urlDecode_OffsetDateTime() {
        Assert.assertEquals("2020-02-20T11:57:58.231Z",
                StringUtils.urlDecode("2020-02-20T11%3a57%3a58.231Z"));
    }

    @Test
    public void removeFirstStart_MatchOne() {
        String result = StringUtils.removeFirstStart("helloworld", "hell", "gret");
        Assert.assertEquals("oworld", result);
    }

    @Test
    public void removeFirstStart_MatchNone() {
        String result = StringUtils.removeFirstStart("helloworld", "hedll", "gret");
        Assert.assertEquals("helloworld", result);
    }

    @Test
    public void removeFirstStart_Null() {
        String result = StringUtils.removeFirstStart(null, "hedll", "gret");
        Assert.assertNull(result);
    }

    @Test
    public void test_from_close_begin_getBriefSql() {
        String sql =
                "select * as asasdjadnjabdasdahjdknkqndjbjfbcdcxvnsdwqjdnjwqdnscasndknadjbascbadnasndqwbdqwbjd  from t_user  where id=1";
        String briefSql = StringUtils.getBriefSql(sql, 70);
        System.out.println(briefSql);
        Assert.assertEquals(briefSql.length(), 70);
    }

    @Test
    public void test_from_close_end_getBriefSql() {
        String sql =
                "select * from t_user where id=1 and name='asasdjadnjabdasdahjdknkqndjbjfbcdcxvnsdqjdnjwqdnscasndknadjbascbadnasndqwbdqwbjd'";
        String briefSql = StringUtils.getBriefSql(sql, 70);
        System.out.println(briefSql);
        Assert.assertEquals(briefSql.length(), 70);
    }

    @Test
    public void test_from_close_middle_getBriefSql() {
        String sql =
                "select * as asasdjadnjabdasdahjdknkqndjbjfbcdcxvnsdwqjdnjwqdnscasndknadjbascbadnasndqwbdqwbjd  from t_user where id=1 and name='asasdjadnjabdasdahjdknkqndjbjfbcdcxvnsdqjdnjwqdnscasndknadjbascbadnasndqwbdqwbjd'";
        String briefSql = StringUtils.getBriefSql(sql, 70);
        System.out.println(briefSql);
        Assert.assertEquals(briefSql.length(), 70);
    }

    @Test
    public void test_no_fromgetBriefSql() {
        String sql =
                "insert into test_db (id,version,name) values(1,0,'awsdajsdashbdhabdhabshdbashdbashbdjhasbdhasbdhnczhdqwbdba')";
        String briefSql = StringUtils.getBriefSql(sql, 50);
        System.out.println(briefSql);
        Assert.assertEquals(briefSql.length(), 50);
    }
}
