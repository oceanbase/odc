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
import java.util.Collections;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleDataTypeFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Data_typeContext;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.NumberType;
import com.oceanbase.tools.sqlparser.statement.common.TimestampType;
import com.oceanbase.tools.sqlparser.statement.common.oracle.IntervalType;

/**
 * Test cases for {@link OracleDataTypeFactory}
 *
 * @author yh263208
 * @date 2023-05-16 21:41
 * @since ODC_release_4.2.0
 */
public class OracleDataTypeFactoryTest {

    @Test
    public void generate_int_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("int"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("int", null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_json_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("json"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("json", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_xmltype_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("xmltype"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("xmltype", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_floatWithPrecision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("float(12)"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("float", new BigDecimal("12"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_floatWithoutPrecision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("float"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("float", null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_realWithPrecision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("real(12)"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("real", new BigDecimal("12"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_binaryFloat_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("binary_float"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("binary_float", null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_decimalStar_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("decimal(*,12)"));
        DataType actual = factory.generate();

        NumberType expect = new NumberType("decimal", null, new BigDecimal("12"));
        expect.setStarPresicion(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_decimalOnlyStar_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("number(*)"));
        DataType actual = factory.generate();

        NumberType expect = new NumberType("number", null, null);
        expect.setStarPresicion(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_decimalWithDecimalPrescision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("dec(2E2)"));
        DataType actual = factory.generate();

        DataType expect = new NumberType("dec", new BigDecimal("2E2"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_timestampWithPrescision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("timestamp(12)"));
        DataType actual = factory.generate();

        DataType expect = new TimestampType(new BigDecimal("12"), false, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_timestampWithoutPrescision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("timestamp"));
        DataType actual = factory.generate();

        DataType expect = new TimestampType(null, false, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_timestampTZWithPrescision_generateSucceed() {
        StatementFactory<DataType> factory =
                new OracleDataTypeFactory(getDataTypeContext("timestamp(2E5) with time zone"));
        DataType actual = factory.generate();

        DataType expect = new TimestampType(new BigDecimal("2E5"), true, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_timestampTZWithoutPrescision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("timestamp with time zone"));
        DataType actual = factory.generate();

        DataType expect = new TimestampType(null, true, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_timestampLTZWithPrescision_generateSucceed() {
        StatementFactory<DataType> factory =
                new OracleDataTypeFactory(getDataTypeContext("timestamp(2E5) with local time zone"));
        DataType actual = factory.generate();

        DataType expect = new TimestampType(new BigDecimal("2E5"), false, true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_timestampLTZWithoutPrescision_generateSucceed() {
        StatementFactory<DataType> factory =
                new OracleDataTypeFactory(getDataTypeContext("timestamp with local time zone"));
        DataType actual = factory.generate();

        DataType expect = new TimestampType(null, false, true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_date_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("date"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("date", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_characterWithCharSizeBinaryU8_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(
                getDataTypeContext("character(10 char) binary charset utf_8 collate utf8mb4"));
        DataType actual = factory.generate();

        CharacterType expect = new CharacterType("character", new BigDecimal("10"));
        expect.setCollation("utf8mb4");
        expect.setCharset("utf_8");
        expect.setBinary(true);
        expect.setLengthOption("char");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_characterOnly_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("char"));
        DataType actual = factory.generate();

        CharacterType expect = new CharacterType("char", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_varchar2_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("varchar2(64)"));
        DataType actual = factory.generate();

        CharacterType expect = new CharacterType("varchar2", new BigDecimal("64"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_raw_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("raw(64)"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("raw", Collections.singletonList("64"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_blob_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("blob"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("blob", null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_clob_generateSucceed() {
        StatementFactory<DataType> factory =
                new OracleDataTypeFactory(getDataTypeContext("clob charset utf8 collate utf8mb4"));
        DataType actual = factory.generate();

        CharacterType expect = new CharacterType("clob", null);
        expect.setCollation("utf8mb4");
        expect.setCharset("utf8");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intervalYMWithPrecision_generateSucceed() {
        StatementFactory<DataType> factory =
                new OracleDataTypeFactory(getDataTypeContext("interval year(12) to month"));
        DataType actual = factory.generate();

        DataType expect = new IntervalType(new BigDecimal("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intervalYMWithoutPrecision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("interval year to month"));
        DataType actual = factory.generate();

        DataType expect = new IntervalType(null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intervalDSWithDayPrecision_generateSucceed() {
        StatementFactory<DataType> factory =
                new OracleDataTypeFactory(getDataTypeContext("interval day(12) to second"));
        DataType actual = factory.generate();

        DataType expect = new IntervalType(new BigDecimal("12"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intervalDSWithSecondPrecision_generateSucceed() {
        StatementFactory<DataType> factory =
                new OracleDataTypeFactory(getDataTypeContext("interval day to second(12)"));
        DataType actual = factory.generate();

        DataType expect = new IntervalType((BigDecimal) null, new BigDecimal("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intervalDSWithPrecision_generateSucceed() {
        StatementFactory<DataType> factory =
                new OracleDataTypeFactory(getDataTypeContext("interval day(24) to second(12)"));
        DataType actual = factory.generate();

        DataType expect = new IntervalType(new BigDecimal("24"), new BigDecimal("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_intervalDSWithoutPrecision_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("interval day to second"));
        DataType actual = factory.generate();

        DataType expect = new IntervalType((BigDecimal) null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rowid_generateSucceed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("rowid(12)"));
        DataType actual = factory.generate();

        DataType expect = new GeneralDataType("rowid", Collections.singletonList("12"));
        Assert.assertEquals(expect, actual);
    }

    @Test(expected = ParseCancellationException.class)
    public void generate_varcharWithoutLen_generateFailed() {
        StatementFactory<DataType> factory = new OracleDataTypeFactory(getDataTypeContext("varchar"));
        factory.generate();
    }

    private Data_typeContext getDataTypeContext(String type) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(type));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.data_type();
    }

}
