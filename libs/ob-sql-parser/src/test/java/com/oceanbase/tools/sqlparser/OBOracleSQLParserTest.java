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
package com.oceanbase.tools.sqlparser;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.insert.InsertBody;
import com.oceanbase.tools.sqlparser.statement.insert.SingleTableInsert;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.update.Update;
import com.oceanbase.tools.sqlparser.statement.update.UpdateAssign;

/**
 * Test cases for {@link OBOracleSQLParser}
 *
 * @author yh263208
 * @date 2023-02-15 20:52
 * @since sqlparser_1.0.0_SNAPSHOT
 */
public class OBOracleSQLParserTest {

    @Test
    public void parse_selectStatement_parseSucceed() {
        SQLParser parser = new OBOracleSQLParser();
        Statement actual = parser.parse(new StringReader("select col.* abc from dual"));

        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parse_updateStatement_parseSucceed() {
        SQLParser sqlParser = new OBOracleSQLParser();
        Statement actual = sqlParser.parse(new StringReader("update tab set col=1 where col=100"));

        FromReference tableReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(tableReference),
                Arrays.asList(new UpdateAssign(Arrays.asList(new ColumnReference(null, null, "col")),
                        new ConstExpression("1"), false)));
        RelationReference left = new RelationReference("col", null);
        ConstExpression right = new ConstExpression("100");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parse_deleteStatement_parseSucceed() {
        SQLParser sqlParser = new OBOracleSQLParser();
        Statement actual = sqlParser.parse(new StringReader("delete from tab where col=100"));

        NameReference nameReference = new NameReference(null, "tab", null);
        Delete expect = new Delete(nameReference);
        RelationReference left = new RelationReference("col", null);
        ConstExpression right = new ConstExpression("100");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parse_insertStatement_parseSucceed() {
        SQLParser sqlParser = new OBOracleSQLParser();
        Statement acutal = sqlParser.parse(new StringReader("insert into tab values(1,1,2)"));

        SingleTableInsert expect = new SingleTableInsert(new InsertBody());
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void parse_createtableStatement_parseSucceed() {
        SQLParser sqlParser = new OBOracleSQLParser();
        Statement actual = sqlParser.parse(new StringReader("create table abcd (id varchar(64))"));

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

}
