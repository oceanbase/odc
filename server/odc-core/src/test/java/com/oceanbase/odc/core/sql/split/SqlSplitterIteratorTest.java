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

package com.oceanbase.odc.core.sql.split;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.test.dataloader.DataLoaders;
import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/11/28 19:06
 */
public class SqlSplitterIteratorTest {

    @Test
    public void split_Delimiter$$_DelimiterChanged() {
        SqlSplitter sqlSplitter = sqlSplitter();
        sqlSplitter.split("delimiter $$");
        Assert.assertEquals("$$", sqlSplitter.getDelimiter());
    }

    @Test
    public void split_Delimiter$$WithLine_DelimiterChanged() {
        SqlSplitter sqlSplitter = sqlSplitter();
        sqlSplitter.split("delimiter $$\n");
        Assert.assertEquals("$$", sqlSplitter.getDelimiter());
    }

    @Test
    public void split_Delimiter$$WithWhiteSpace_DelimiterChanged() {
        SqlSplitter sqlSplitter = sqlSplitter();
        sqlSplitter.split("delimiter $$ ");
        Assert.assertEquals("$$", sqlSplitter.getDelimiter());
    }

    @Test
    public void split_DelimiterDoubleSlash_DelimiterChanged() {
        SqlSplitter sqlSplitter = sqlSplitter();
        sqlSplitter.split("delimiter //");
        Assert.assertEquals("//", sqlSplitter.getDelimiter());
    }

    @Test
    public void split_Blank_Empty() {
        String sql = " ";
        SqlSplitter sqlSplitter = sqlSplitter();
        List<String> except = sqlSplitter.split(sql).stream().map(OffsetString::getStr).collect(Collectors.toList());
        SqlStatementIterator sqlStatementIterator = sqlStatementIterator(sql);
        List<String> actual = new ArrayList<>();
        while (sqlStatementIterator.hasNext()) {
            actual.add(sqlStatementIterator.next().getStr());
        }
        Assert.assertEquals(except, actual);
    }

    @Test
    public void split_Delimiter$() {
        String sql = "select 1 from dual $\nselect 2 from dual;";
        SqlSplitter sqlSplitter = sqlSplitter();
        List<String> except = sqlSplitter.split(sql).stream().map(OffsetString::getStr).collect(Collectors.toList());
        SqlStatementIterator sqlStatementIterator = sqlStatementIterator(sql);
        List<String> actual = new ArrayList<>();
        while (sqlStatementIterator.hasNext()) {
            actual.add(sqlStatementIterator.next().getStr());
        }
        Assert.assertEquals(except, actual);
    }

    @Test
    public void split_ChangeDelimiterFrom$ToOther() {
        String sql = "delimiter $\nselect 1 from dual $\ndelimiter ;\nselect 2 from dual;";
        SqlSplitter sqlSplitter = sqlSplitter();
        List<String> except = sqlSplitter.split(sql).stream().map(OffsetString::getStr).collect(Collectors.toList());
        SqlStatementIterator sqlStatementIterator = sqlStatementIterator(sql);
        List<String> actual = new ArrayList<>();
        while (sqlStatementIterator.hasNext()) {
            actual.add(sqlStatementIterator.next().getStr());
        }
        Assert.assertEquals(except, actual);
    }

    @Test
    public void split_MultiSqlWithComment() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-1-multi-sqls.yml");
    }

    @Test
    public void split_SingleSql() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-1-single-sql.yml");
    }

    @Test
    public void split_SelfDelimiter() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-1-single-sql-self-delimiter.yml");
    }

    @Test
    public void split_AnonymousBlockStartWithBegin() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-2-single-pl-anonymous-block-start-with-begin.yml");
    }

    @Test
    public void split_AnonymousBlockStartWithDeclare() {
        verifyByFileName(
                "src/test/resources/sql/split/sql-splitter-2-single-pl-anonymous-block-start-with-declare.yml");
    }

    @Test
    public void split_AnonymousBlock_With_Backslash() {
        verifyByFileName(
                "src/test/resources/sql/split/sql-splitter-2-single-pl-anonymous-block-with-backslash-inside.yml");
    }

    @Test
    public void split_AnonymousBlockStartWithInnerStringBlock() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-2-single-pl-anonymous-block-with-inner-string.yml");
    }

    @Test
    public void split_CreateFunction() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-3-single-pl-create-function.yml");
    }

    @Test
    public void split_CreateProcedure() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-4-single-pl-create-procedure.yml");
    }

    @Test
    public void split_CreatePackageAndBody() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-5-single-pl-create-package-and-body.yml");
    }

    @Test
    public void split_CreateTypeAndBody() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-6-single-pl-create-type-and-body.yml");
    }

    @Test
    public void split_CreateTrigger() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-7-single-pl-create-trigger.yml");
    }

    @Test
    public void split_CreatePackageAndBody_with_routine() {
        verifyByFileName(
                "src/test/resources/sql/split/sql-splitter-8-single-pl-create-package-and-body-with-routine.yml");
    }

    @Test
    public void split_Customer_Sample1() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-9-customer-sample-1.yml");
    }

    @Test
    public void split_Customer_SampleProcedure1() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-9-customer-sample-procedure-1.yml");
    }

    @Test
    public void split_Customer_SamplePackage1() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-9-customer-sample-package-1.yml");
    }

    @Test
    public void split_Customer_SampleTrigger1() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-9-customer-sample-trigger-1.yml");
    }

    @Ignore("Characters other than '/', ';' and '$' are not supported as delimiter")
    public void split_CommentProcessor1() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-10-comment-processor-1.yml");
    }

    @Test
    public void split_CommentWithSemicolonInside() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-10-comment-with-semicolon-inside.yml");
    }

    @Test
    public void split_StringWithBackslashInside() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-10-string-with-backslash-inside.yml");
    }

    @Test
    public void split_Multi_Loop_Block() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-11-multi-loop-block.yml");
    }

    @Test
    public void split_Multi_PL_Object_With_Language() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-12-multi-pl-object-with-language.yml");
    }

    @Test
    public void split_Multi_PL_With_Double_Dollar_Delimiter() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-13-multi-pl-object-with-dollar-delimiter.yml");
    }

    @Test
    public void split_Multi_PL_With_Backslash_Delimiter() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-14-multi-pl-object-with-backslash-delimiter.yml");
    }

    @Test
    public void split_Large_PL_With_CaseWhen_InSqlStmt() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-15-pl-with-case-when-in-sql-stmt.yml");
    }

    @Test
    public void split_PL_With_ELSE_IF() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-16-pl-with-else-and-if.yml");
    }

    @Test
    public void split_PL_With_RIGHTBRACKET() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-17-pl-with-right_bracket.yml");
    }

    @Test
    public void split_PL_With_LABELRIGHT() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-18-pl-with-label_right.yml");
    }

    @Test
    public void split_PL_With_CASE_END() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-19-pl-with-sum-case-end-as.yml");
    }

    @Test
    public void split_PL_With_LANGUAGE() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-20-pl-with-language.yml");
    }

    @Test
    public void split_PL_While_Loop() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-21-pl-while-loop.yml");
    }

    @Test
    public void split_PL_For_Loop() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-22-pl-for-loop.yml");
    }

    @Test
    public void split_Multi_Line_Comments_Inside() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-23-multi-lines-comment-inside.yml");
    }

    protected void verifyByFileName(String fileName) {
        TestData testData = DataLoaders.yaml().fromFile(fileName, TestData.class);
        SqlSplitter sqlSplitter = sqlSplitter();
        List<String> except =
                sqlSplitter.split(testData.origin).stream().map(OffsetString::getStr).collect(Collectors.toList());
        SqlStatementIterator sqlStatementIterator = sqlStatementIterator(testData.origin);
        List<String> actual = new ArrayList<>();
        while (sqlStatementIterator.hasNext()) {
            actual.add(sqlStatementIterator.next().getStr());
        }
        Assert.assertEquals(except, actual);
    }

    private SqlSplitter sqlSplitter() {
        return new SqlSplitter(PlSqlLexer.class);
    }

    private SqlStatementIterator sqlStatementIterator(String sql) {
        return SqlSplitter.iterator(new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8, ";");
    }

    @Data
    static class TestData {
        private String origin;
        private List<String> expected;
        private String expectedEndDelimiter;
    }

    /**
     * Test the {@link OffsetString#getOffset()} is accurate.
     * 
     * @throws IOException
     */
    @Test
    public void test_offset() throws IOException {
        String sql = getSqlFromFile("sql/split/sql-splitter-0-offset-test.sql");
        SqlStatementIterator sqlStatementIterator = sqlStatementIterator(sql);
        List<OffsetString> actual = new ArrayList<>();
        while (sqlStatementIterator.hasNext()) {
            actual.add(sqlStatementIterator.next());
        }
        List<OffsetString> expected = getSqls("sql/split/sql-splitter-0-offset-verify.yml");
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i), actual.get(i));
        }
    }

    private String getSqlFromFile(String fileName) throws IOException {
        InputStream input = this.getClass().getClassLoader().getResourceAsStream(fileName);
        assert input != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader((input)));
        StringWriter writer = new StringWriter();
        char[] buffer = new char[1024];
        int len = reader.read(buffer);
        while (len != -1) {
            writer.write(buffer, 0, len);
            len = reader.read(buffer, 0, len);
        }
        reader.close();
        writer.close();
        return writer.getBuffer().toString();
    }

    private List<OffsetString> getSqls(String fileName) {
        return YamlUtils.fromYamlList(fileName, OffsetString.class);
    }

}
