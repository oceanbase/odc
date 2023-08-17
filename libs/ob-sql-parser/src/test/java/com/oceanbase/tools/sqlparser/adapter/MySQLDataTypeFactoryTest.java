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
package com.oceanbase.tools.sqlparser.adapter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLDataTypeFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Data_typeContext;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.NumberType;
import com.oceanbase.tools.sqlparser.statement.common.TimestampType;
import com.oceanbase.tools.sqlparser.statement.common.mysql.CollectionType;

/**
 * Test cases for {@link MySQLDataTypeFactoryTest}
 * 
 * @author yh263208
 * @date 2023-05-18 15:55
 * @since ODC_release_4.2.0
 */
public class MySQLDataTypeFactoryTest {

    @Test
    public void generate_int_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("int"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("int", null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intWithPrecision_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("int(11)"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("int", new BigDecimal("11"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intWithPrecisionUnsigned_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("int(11) unsigned"));
        DataType actual = factory.generate();

        NumberType expect = new NumberType("int", new BigDecimal("11"), null);
        expect.setSigned(false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intWithPrecisionSignedZeroFill_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("int(11) unsigned zerofill"));
        DataType actual = factory.generate();

        NumberType expect = new NumberType("int", new BigDecimal("11"), null);
        expect.setSigned(false);
        expect.setZeroFill(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_floatWithPrecision_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("float(12)"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("float", new BigDecimal("12"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_floatWithoutPrecision_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("float"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("float", null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_realWithPrecisionUnsigned_generateSucceed() {
        StatementFactory<DataType> factory =
                new MySQLDataTypeFactory(getDataTypeContext("real precision(2E2) unsigned"));
        DataType actual = factory.generate();

        NumberType expect = new NumberType("real precision", new BigDecimal("2E2"), null);
        expect.setSigned(false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_doubleWithPrecisionSignedZerofill_generateSucceed() {
        StatementFactory<DataType> factory =
                new MySQLDataTypeFactory(getDataTypeContext("double precision(12, 13) signed zerofill"));
        DataType actual = factory.generate();

        NumberType expect = new NumberType("double precision", new BigDecimal("12"), new BigDecimal("13"));
        expect.setSigned(true);
        expect.setZeroFill(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_decimalWithDecimalPrescision_generateSucceed() {
        StatementFactory<DataType> factory =
                new MySQLDataTypeFactory(getDataTypeContext("numeric(2,3) unsigned zerofill"));
        DataType actual = factory.generate();

        NumberType expect = new NumberType("numeric", new BigDecimal("2"), new BigDecimal("3"));
        expect.setSigned(false);
        expect.setZeroFill(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_bool_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("bool"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("bool", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_timestampWithPrescision_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("timestamp(12)"));
        DataType actual = factory.generate();

        DataType expect = new TimestampType(new BigDecimal("12"), false, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_datetime_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("datetime"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("datetime", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_time_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("time(12)"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("time", Collections.singletonList("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_year_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("year(12)"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("year", Collections.singletonList("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_longText_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("longtext(12) binary"));
        DataType actual = factory.generate();

        CharacterType expect = new CharacterType("longtext", new BigDecimal("12"));
        expect.setBinary(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_textWithCharSetAndCollation_generateSucceed() {
        StatementFactory<DataType> factory =
                new MySQLDataTypeFactory(getDataTypeContext("text(12) binary charset utf8 collate utf8mb4"));
        DataType actual = factory.generate();

        CharacterType expect = new CharacterType("text", new BigDecimal("12"));
        expect.setBinary(true);
        expect.setCollation("utf8mb4");
        expect.setCharset("utf8");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_varcharWithCharSetAndCollation_generateSucceed() {
        StatementFactory<DataType> factory =
                new MySQLDataTypeFactory(getDataTypeContext("varchar(12) binary charset utf8 collate utf8mb4"));
        DataType actual = factory.generate();

        CharacterType expect = new CharacterType("varchar", new BigDecimal("12"));
        expect.setBinary(true);
        expect.setCollation("utf8mb4");
        expect.setCharset("utf8");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_characterWithCharSetAndCollation_generateSucceed() {
        StatementFactory<DataType> factory =
                new MySQLDataTypeFactory(getDataTypeContext("character(12) binary charset utf8 collate utf8mb4"));
        DataType actual = factory.generate();

        CharacterType expect = new CharacterType("character", new BigDecimal("12"));
        expect.setBinary(true);
        expect.setCollation("utf8mb4");
        expect.setCharset("utf8");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_blob_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("blob(12)"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("blob", Collections.singletonList("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_varbinary_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("varbinary(12)"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("varbinary", Collections.singletonList("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_bit_generateSucceed() {
        StatementFactory<DataType> factory = new MySQLDataTypeFactory(getDataTypeContext("bit(12)"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("bit", Collections.singletonList("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_set_generateSucceed() {
        StatementFactory<DataType> factory =
                new MySQLDataTypeFactory(getDataTypeContext("set('1', '2') binary charset utf8 collate utf8mb4"));
        DataType actual = factory.generate();

        CollectionType expect = new CollectionType("set", Arrays.asList("'1'", "'2'"));
        expect.setBinary(true);
        expect.setCollation("utf8mb4");
        expect.setCharset("utf8");
        Assert.assertEquals(expect, actual);
    }

    private Data_typeContext getDataTypeContext(String type) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(type));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.data_type();
    }
}
