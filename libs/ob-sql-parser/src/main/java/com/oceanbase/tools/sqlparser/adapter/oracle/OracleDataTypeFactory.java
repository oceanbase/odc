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

import org.antlr.v4.runtime.ParserRuleContext;
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
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Js_agg_returning_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Js_query_return_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Js_return_default_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Js_return_text_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Js_return_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Js_value_return_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Nstring_length_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Number_precisionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Number_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_jt_value_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Rowid_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.String_length_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Timestamp_type_iContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Treat_data_typeContext;
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

    private final ParserRuleContext parserRuleContext;

    public OracleDataTypeFactory(@NonNull Data_typeContext dataTypeContext) {
        this.parserRuleContext = dataTypeContext;
    }

    public OracleDataTypeFactory(@NonNull Cast_data_typeContext castDataTypeContext) {
        this.parserRuleContext = castDataTypeContext;
    }

    public OracleDataTypeFactory(@NonNull Treat_data_typeContext treatDataTypeContext) {
        this.parserRuleContext = treatDataTypeContext;
    }

    public OracleDataTypeFactory(@NonNull Opt_jt_value_typeContext optJtValueTypeContext) {
        this.parserRuleContext = optJtValueTypeContext;
    }

    public OracleDataTypeFactory(@NonNull Js_value_return_typeContext jsValueReturnTypeContext) {
        this.parserRuleContext = jsValueReturnTypeContext;
    }

    public OracleDataTypeFactory(@NonNull Js_query_return_typeContext jsQueryReturnTypeContext) {
        this.parserRuleContext = jsQueryReturnTypeContext;
    }

    public OracleDataTypeFactory(@NonNull Js_return_typeContext jsReturnTypeContext) {
        this.parserRuleContext = jsReturnTypeContext;
    }

    public OracleDataTypeFactory(@NonNull Js_agg_returning_typeContext jsAggReturningTypeContext) {
        this.parserRuleContext = jsAggReturningTypeContext;
    }

    @Override
    public DataType generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public DataType visitData_type(Data_typeContext ctx) {
        if (ctx.STRING_VALUE() != null
                || ctx.JSON() != null
                || ctx.XMLTYPE() != null
                || ctx.SDO_GEOMETRY() != null) {
            return new GeneralDataType(ctx, ctx.getChild(0).getText(), null);
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
    public DataType visitOpt_jt_value_type(Opt_jt_value_typeContext ctx) {
        if (ctx.CHAR() != null || ctx.NVARCHAR2() != null || ctx.NCHAR() != null) {
            return getDataType(new TextTypeOpt() {
                @Override
                public ParserRuleContext getCtx() {
                    return ctx;
                }

                @Override
                public String getTypeName() {
                    return ctx.getChild(0).getText();
                }

                @Override
                public String_length_iContext getStringLengthIContext() {
                    return ctx.string_length_i();
                }

                @Override
                public Nstring_length_iContext getNstringLengthIContext() {
                    return ctx.nstring_length_i();
                }

                @Override
                public boolean isBinary() {
                    return ctx.BINARY() != null;
                }
            });
        }
        return visitChildren(ctx);
    }

    @Override
    public DataType visitJs_value_return_type(Js_value_return_typeContext ctx) {
        if (ctx.NUMBER() != null) {
            return getNumberType(ctx.NUMBER().getText(), ctx, ctx.number_precision());
        }
        return visitChildren(ctx);
    }

    @Override
    public DataType visitJs_query_return_type(Js_query_return_typeContext ctx) {
        if (ctx.BLOB() != null || ctx.JSON() != null) {
            return new GeneralDataType(ctx, ctx.getChild(0).getText(), null);
        }
        return visitChildren(ctx);
    }

    @Override
    public DataType visitJs_agg_returning_type(Js_agg_returning_typeContext ctx) {
        if (ctx.RAW() != null) {
            List<String> args = new ArrayList<>();
            if (ctx.zero_suffix_intnum() != null) {
                args.add(ctx.zero_suffix_intnum().getText());
            }
            return new GeneralDataType(ctx, ctx.RAW().getText(), args);
        }
        return getDataType(new TextTypeOpt() {
            @Override
            public ParserRuleContext getCtx() {
                return ctx;
            }

            @Override
            public String getTypeName() {
                return ctx.NVARCHAR2().getText();
            }

            @Override
            public String_length_iContext getStringLengthIContext() {
                return null;
            }

            @Override
            public Nstring_length_iContext getNstringLengthIContext() {
                return ctx.nstring_length_i();
            }

            @Override
            public boolean isBinary() {
                return false;
            }
        });
    }

    @Override
    public DataType visitJs_return_type(Js_return_typeContext ctx) {
        if (ctx.BLOB() != null || ctx.JSON() != null) {
            return new GeneralDataType(ctx, ctx.getChild(0).getText(), null);
        }
        return visitChildren(ctx);
    }

    @Override
    public DataType visitJs_return_text_type(Js_return_text_typeContext ctx) {
        return getDataType(new TextTypeOpt() {
            @Override
            public ParserRuleContext getCtx() {
                return ctx;
            }

            @Override
            public String getTypeName() {
                return ctx.getChild(0).getText();
            }

            @Override
            public String_length_iContext getStringLengthIContext() {
                return ctx.string_length_i();
            }

            @Override
            public Nstring_length_iContext getNstringLengthIContext() {
                return null;
            }

            @Override
            public boolean isBinary() {
                return ctx.BINARY() != null;
            }
        });
    }

    @Override
    public DataType visitTreat_data_type(Treat_data_typeContext ctx) {
        if (ctx.JSON() != null) {
            return new GeneralDataType(ctx, ctx.JSON().getText(), null);
        } else if (ctx.obj_access_ref_cast() != null) {
            return new GeneralDataType(ctx, ctx.obj_access_ref_cast().getText(), null);
        }
        return visitChildren(ctx);
    }

    @Override
    public DataType visitJs_return_default_type(Js_return_default_typeContext ctx) {
        return null;
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
        return getNumberType(ctx.getChild(0).getText(), ctx, ctx.number_precision());
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
        return getDataType(new TextTypeOpt() {
            @Override
            public ParserRuleContext getCtx() {
                return ctx;
            }

            @Override
            public String getTypeName() {
                return ctx.getChild(0).getText();
            }

            @Override
            public String_length_iContext getStringLengthIContext() {
                return ctx.string_length_i();
            }

            @Override
            public Nstring_length_iContext getNstringLengthIContext() {
                return null;
            }

            @Override
            public boolean isBinary() {
                return ctx.BINARY() != null;
            }
        });
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

    private DataType getNumberType(String typeName, ParserRuleContext parent,
            Number_precisionContext numberPrecisionContext) {
        List<String> args = new ArrayList<>();
        visitNumberPrecision(numberPrecisionContext, args);
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
        NumberType numberType = new NumberType(parent, typeName, first, second);
        if (starPrecision) {
            numberType.setStarPresicion(true);
        }
        return numberType;
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

    private static void visitStringLengthI(String_length_iContext ctx, List<String> args) {
        if (ctx == null) {
            return;
        }
        StringBuilder builder = new StringBuilder(ctx.zero_suffix_intnum().getText());
        if (ctx.CHARACTER() != null || ctx.CHAR() != null || ctx.BYTE() != null) {
            builder.append(" ").append(ctx.getChild(2).getText());
        }
        args.add(builder.toString());
    }

    private static void visitNstringLengthI(Nstring_length_iContext ctx, List<String> args) {
        if (ctx == null) {
            return;
        }
        args.add(ctx.zero_suffix_intnum().getText());
    }

    private String getPrecision(Data_type_precisionContext context) {
        if (context.precision_int_num() != null) {
            return context.precision_int_num().getText();
        }
        return context.precision_decimal_num().getText();
    }

    public static DataType getDataType(TextTypeOpt opt) {
        List<String> args = new ArrayList<>();
        visitStringLengthI(opt.getStringLengthIContext(), args);
        visitNstringLengthI(opt.getNstringLengthIContext(), args);
        CharacterType type;
        if (args.isEmpty()) {
            type = new CharacterType(opt.getCtx(), opt.getTypeName(), null);
        } else {
            String[] arg = args.get(0).split(" ");
            type = new CharacterType(opt.getCtx(), opt.getTypeName(), new BigDecimal(arg[0]));
            if (arg.length > 1) {
                type.setLengthOption(arg[1]);
            }
        }
        if (opt.isBinary()) {
            type.setBinary(true);
        }
        return type;
    }

    interface TextTypeOpt {
        ParserRuleContext getCtx();

        String getTypeName();

        String_length_iContext getStringLengthIContext();

        Nstring_length_iContext getNstringLengthIContext();

        boolean isBinary();
    }

}
