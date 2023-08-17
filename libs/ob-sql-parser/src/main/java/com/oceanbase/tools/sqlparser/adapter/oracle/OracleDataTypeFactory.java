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
package com.oceanbase.tools.sqlparser.adapter.oracle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Binary_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Cast_data_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Character_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Data_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Data_type_precisionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Datetime_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Double_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Float_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Int_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Interval_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Number_precisionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Number_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Rowid_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.String_length_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Timestamp_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Udt_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.NumberType;
import com.oceanbase.tools.sqlparser.statement.common.TimestampType;
import com.oceanbase.tools.sqlparser.statement.common.oracle.IntervalType;

import lombok.NonNull;

/**
 * {@link OracleDataTypeFactory}
 *
 * @author yh263208
 * @date 2023-05-16 16:32
 * @since ODC_release_4.2.0
 * @see StatementFactory
 */
public class OracleDataTypeFactory extends OBParserBaseVisitor<DataType> implements StatementFactory<DataType> {

    private final Data_typeContext dataTypeContext;
    private final Cast_data_typeContext castDataTypeContext;

    public OracleDataTypeFactory(@NonNull Data_typeContext dataTypeContext) {
        this.dataTypeContext = dataTypeContext;
        this.castDataTypeContext = null;
    }

    public OracleDataTypeFactory(@NonNull Cast_data_typeContext castDataTypeContext) {
        this.dataTypeContext = null;
        this.castDataTypeContext = castDataTypeContext;
    }

    @Override
    public DataType generate() {
        if (this.dataTypeContext != null) {
            return visit(this.dataTypeContext);
        }
        return visit(this.castDataTypeContext);
    }

    @Override
    public DataType visitData_type(Data_typeContext ctx) {
        if (ctx.STRING_VALUE() != null) {
            return new GeneralDataType(ctx, ctx.STRING_VALUE().getText(), null);
        } else if (ctx.character_type_i() != null) {
            CharacterType type = new CharacterType(ctx, (CharacterType) visit(ctx.character_type_i()));
            if (ctx.charset_name() != null) {
                type.setCharset(ctx.charset_name().getText());
            }
            if (ctx.collation() != null) {
                type.setCollation(ctx.collation().collation_name().getText());
            }
            return type;
        }
        return visitChildren(ctx);
    }

    @Override
    public DataType visitInt_type_i(Int_type_iContext ctx) {
        return new NumberType(ctx, ctx.getText(), null, null);
    }

    @Override
    public DataType visitFloat_type_i(Float_type_iContext ctx) {
        List<String> args = new ArrayList<>();
        if (ctx.data_type_precision() != null) {
            args.add(getPrecision(ctx.data_type_precision()));
        }
        BigDecimal first = null;
        BigDecimal second = null;
        if (!args.isEmpty()) {
            first = new BigDecimal(args.get(0));
            if (args.size() > 1) {
                second = new BigDecimal(args.get(1));
            }
        }
        return new NumberType(ctx, ctx.getChild(0).getText(), first, second);
    }

    @Override
    public DataType visitDouble_type_i(Double_type_iContext ctx) {
        return new NumberType(ctx, ctx.getText(), null, null);
    }

    @Override
    public DataType visitNumber_type_i(Number_type_iContext ctx) {
        List<String> args = new ArrayList<>();
        visitNumberPrecision(ctx.number_precision(), args);
        BigDecimal first = null;
        BigDecimal second = null;
        boolean starPrecision = false;
        if (!args.isEmpty()) {
            if (!"*".equals(args.get(0))) {
                first = new BigDecimal(args.get(0));
            } else {
                starPrecision = true;
            }
            if (args.size() > 1) {
                second = new BigDecimal(args.get(1));
            }
        }
        NumberType numberType = new NumberType(ctx, ctx.getChild(0).getText(), first, second);
        if (starPrecision) {
            numberType.setStarPresicion(true);
        }
        return numberType;
    }

    @Override
    public DataType visitTimestamp_type_i(Timestamp_type_iContext ctx) {
        BigDecimal precision = null;
        if (ctx.data_type_precision() != null) {
            precision = new BigDecimal(getPrecision(ctx.data_type_precision()));
        }
        if (ctx.WITH() == null) {
            return new TimestampType(ctx, precision, false, false);
        }
        if (ctx.LOCAL() != null) {
            return new TimestampType(ctx, precision, false, true);
        }
        return new TimestampType(ctx, precision, true, false);
    }

    @Override
    public DataType visitDatetime_type_i(Datetime_type_iContext ctx) {
        return new GeneralDataType(ctx, ctx.getText(), null);
    }

    @Override
    public DataType visitCharacter_type_i(Character_type_iContext ctx) {
        List<String> args = new ArrayList<>();
        String typeName = ctx.getChild(0).getText();
        visitStringLengthI(ctx.string_length_i(), args);
        CharacterType type;
        if (args.isEmpty()) {
            type = new CharacterType(ctx, typeName, null);
        } else {
            String[] arg = args.get(0).split(" ");
            type = new CharacterType(ctx, typeName, new BigDecimal(arg[0]));
            if (arg.length > 1) {
                type.setLengthOption(arg[1]);
            }
        }
        if (ctx.BINARY() != null) {
            type.setBinary(true);
        }
        return type;
    }

    @Override
    public DataType visitBinary_type_i(Binary_type_iContext ctx) {
        List<String> args = new ArrayList<>();
        if (ctx.zero_suffix_intnum() != null) {
            args.add(ctx.zero_suffix_intnum().getText());
        }
        return new GeneralDataType(ctx, ctx.getChild(0).getText(), args);
    }

    @Override
    public DataType visitInterval_type_i(Interval_type_iContext ctx) {
        if (ctx.YEAR() != null) {
            BigDecimal yearPrecision = null;
            if (ctx.year_precision != null) {
                yearPrecision = new BigDecimal(getPrecision(ctx.year_precision));
            }
            return new IntervalType(ctx, yearPrecision);
        }
        BigDecimal dayPrecision = null;
        BigDecimal secondPrecision = null;
        if (ctx.day_precision != null) {
            dayPrecision = new BigDecimal(getPrecision(ctx.day_precision));
        }
        if (ctx.second_precision != null) {
            secondPrecision = new BigDecimal(getPrecision(ctx.second_precision));
        }
        return new IntervalType(ctx, dayPrecision, secondPrecision);
    }

    @Override
    public DataType visitRowid_type_i(Rowid_type_iContext ctx) {
        List<String> args = new ArrayList<>();
        if (ctx.urowid_length_i() != null) {
            args.add(ctx.urowid_length_i().INTNUM().getText());
        }
        return new GeneralDataType(ctx, ctx.getChild(0).getText(), args);
    }

    @Override
    public DataType visitUdt_type_i(Udt_type_iContext ctx) {
        return new GeneralDataType(ctx, ctx.getText(), null);
    }

    private void visitNumberPrecision(Number_precisionContext ctx, List<String> args) {
        if (ctx == null) {
            return;
        }
        if (ctx.precision_decimal_num() != null) {
            args.add(ctx.precision_decimal_num().getText());
        } else {
            if (ctx.Star() != null) {
                args.add(ctx.Star().getText());
            }
            if (CollectionUtils.isNotEmpty(ctx.signed_int_num())) {
                ctx.signed_int_num().forEach(s -> args.add(s.getText()));
            }
        }
    }

    private void visitStringLengthI(String_length_iContext ctx, List<String> args) {
        if (ctx == null) {
            return;
        }
        StringBuilder builder = new StringBuilder(ctx.zero_suffix_intnum().getText());
        if (ctx.CHARACTER() != null || ctx.CHAR() != null || ctx.BYTE() != null) {
            builder.append(" ").append(ctx.getChild(2).getText());
        }
        args.add(builder.toString());
    }

    private String getPrecision(Data_type_precisionContext context) {
        if (context.precision_int_num() != null) {
            return context.precision_int_num().getText();
        }
        return context.precision_decimal_num().getText();
    }

}
