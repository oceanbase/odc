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
package com.oceanbase.tools.sqlparser.adapter.mysql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Binary_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Bit_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Blob_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Bool_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Cast_data_typeContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Character_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Collection_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Data_typeContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Data_type_precisionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Date_year_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Datetime_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Float_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Int_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Json_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Number_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Precision_int_numContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.String_length_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Text_type_iContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.NumberType;
import com.oceanbase.tools.sqlparser.statement.common.TimestampType;
import com.oceanbase.tools.sqlparser.statement.common.mysql.CollectionType;

import lombok.NonNull;

/**
 * {@link MySQLDataTypeFactory}
 *
 * @author yh263208
 * @date 2022-12-09 19:40
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLDataTypeFactory extends OBParserBaseVisitor<DataType> implements StatementFactory<DataType> {

    private final Data_typeContext dataTypeContext;
    private final Cast_data_typeContext castDataTypeContext;

    public MySQLDataTypeFactory(@NonNull Cast_data_typeContext castDataTypeContext) {
        this.dataTypeContext = null;
        this.castDataTypeContext = castDataTypeContext;
    }

    public MySQLDataTypeFactory(@NonNull Data_typeContext dataTypeContext) {
        this.castDataTypeContext = null;
        this.dataTypeContext = dataTypeContext;
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
        } else if (ctx.text_type_i() != null) {
            CharacterType characterType = new CharacterType(ctx, (CharacterType) visit(ctx.text_type_i()));
            if (ctx.charset_name() != null) {
                characterType.setCharset(ctx.charset_name().getText());
            }
            if (ctx.collation() != null) {
                characterType.setCollation(ctx.collation().collation_name().getText());
            }
            return characterType;
        }
        return visitChildren(ctx);
    }

    @Override
    public DataType visitCast_data_type(Cast_data_typeContext ctx) {
        if (ctx.SIGNED() != null || ctx.UNSIGNED() != null || ctx.INTEGER() != null) {
            StringBuilder builder = new StringBuilder();
            if (ctx.SIGNED() != null) {
                builder.append(ctx.SIGNED().getText());
            } else {
                builder.append(ctx.UNSIGNED().getText());
            }
            if (ctx.INTEGER() != null) {
                builder.append(" ").append(ctx.INTEGER().getText());
            }
            return new GeneralDataType(ctx, builder.toString(), null);
        }
        return visitChildren(ctx);
    }


    @Override
    public DataType visitBinary_type_i(Binary_type_iContext ctx) {
        List<String> args = new ArrayList<>();
        String arg = getLength(ctx.string_length_i());
        if (arg != null) {
            args.add(arg);
        }
        return new GeneralDataType(ctx, ctx.getChild(0).getText(), args);
    }

    @Override
    public DataType visitCharacter_type_i(Character_type_iContext ctx) {
        BigDecimal len = null;
        String arg = getLength(ctx.string_length_i());
        if (arg != null) {
            len = new BigDecimal(arg);
        }
        CharacterType type = new CharacterType(ctx, ctx.CHARACTER().getText(), len);
        if (ctx.BINARY() != null) {
            type.setBinary(true);
        }
        return type;
    }

    @Override
    public DataType visitDatetime_type_i(Datetime_type_iContext ctx) {
        if (ctx.TIMESTAMP() != null) {
            BigDecimal precision = null;
            if (ctx.precision_int_num() != null) {
                precision = new BigDecimal(getArgs(ctx.precision_int_num()).get(0));
            }
            return new TimestampType(ctx, precision, false, false);
        }
        return new GeneralDataType(ctx, ctx.getChild(0).getText(), getArgs(ctx.precision_int_num()));
    }

    @Override
    public DataType visitDate_year_type_i(Date_year_type_iContext ctx) {
        return new GeneralDataType(ctx, ctx.getChild(0).getText(), getArgs(ctx.precision_int_num()));
    }

    @Override
    public DataType visitFloat_type_i(Float_type_iContext ctx) {
        String typeName = ctx.getChild(0).getText();
        if (ctx.PRECISION() != null) {
            typeName = typeName + " " + ctx.PRECISION().getText();
        }
        List<String> args = new ArrayList<>(getArgs(ctx.data_type_precision()));
        args.addAll(getArgs(ctx.precision_int_num()));
        BigDecimal first = null;
        BigDecimal second = null;
        if (!args.isEmpty()) {
            first = new BigDecimal(args.get(0));
            if (args.size() > 1) {
                second = new BigDecimal(args.get(1));
            }
        }
        NumberType numberType = new NumberType(ctx, typeName, first, second);
        setNumberTypeOptions(numberType, ctx.UNSIGNED(), ctx.SIGNED(), ctx.ZEROFILL());
        return numberType;
    }

    @Override
    public DataType visitNumber_type_i(Number_type_iContext ctx) {
        String typeName = ctx.getChild(0).getText();
        List<String> args = getArgs(ctx.precision_int_num());
        BigDecimal first = null;
        BigDecimal second = null;
        if (!args.isEmpty()) {
            first = new BigDecimal(args.get(0));
            if (args.size() > 1) {
                second = new BigDecimal(args.get(1));
            }
        }
        NumberType numberType = new NumberType(ctx, typeName, first, second);
        setNumberTypeOptions(numberType, ctx.UNSIGNED(), ctx.SIGNED(), ctx.ZEROFILL());
        return numberType;
    }

    @Override
    public DataType visitJson_type_i(Json_type_iContext ctx) {
        return new GeneralDataType(ctx, ctx.JSON().getText(), null);
    }

    @Override
    public DataType visitInt_type_i(Int_type_iContext ctx) {
        List<String> args = getArgs(ctx.precision_int_num());
        BigDecimal precision = null;
        if (!args.isEmpty()) {
            precision = new BigDecimal(args.get(0));
        }
        NumberType numberType = new NumberType(ctx, ctx.getChild(0).getText(), precision, null);
        setNumberTypeOptions(numberType, ctx.UNSIGNED(), ctx.SIGNED(), ctx.ZEROFILL());
        return numberType;
    }

    @Override
    public DataType visitBool_type_i(Bool_type_iContext ctx) {
        return new GeneralDataType(ctx, ctx.getChild(0).getText(), null);
    }

    @Override
    public DataType visitText_type_i(Text_type_iContext ctx) {
        if (ctx.character_type_i() != null) {
            return visit(ctx.character_type_i());
        }
        BigDecimal len = null;
        String arg = getLength(ctx.string_length_i());
        if (arg != null) {
            len = new BigDecimal(arg);
        }
        CharacterType characterType = new CharacterType(ctx, ctx.getChild(0).getText(), len);
        if (ctx.BINARY() != null) {
            characterType.setBinary(true);
        }
        return characterType;
    }

    @Override
    public DataType visitBlob_type_i(Blob_type_iContext ctx) {
        String arg = getLength(ctx.string_length_i());
        List<String> args = arg == null ? null : Collections.singletonList(arg);
        return new GeneralDataType(ctx, ctx.getChild(0).getText(), args);
    }

    @Override
    public DataType visitBit_type_i(Bit_type_iContext ctx) {
        return new GeneralDataType(ctx, ctx.BIT().getText(), getArgs(ctx.precision_int_num()));
    }

    @Override
    public DataType visitCollection_type_i(Collection_type_iContext ctx) {
        List<String> args = ctx.string_list().text_string().stream()
                .map(RuleContext::getText).collect(Collectors.toList());
        CollectionType type = new CollectionType(ctx, ctx.getChild(0).getText(), args);
        if (ctx.BINARY() != null) {
            type.setBinary(true);
        }
        if (ctx.charset_name() != null) {
            type.setCharset(ctx.charset_name().getText());
        }
        if (ctx.collation() != null) {
            type.setCollation(ctx.collation().collation_name().getText());
        }
        return type;
    }

    private void setNumberTypeOptions(NumberType numberType, TerminalNode unsigned,
            TerminalNode signed, TerminalNode zeroFill) {
        if (unsigned != null) {
            numberType.setSigned(false);
        }
        if (signed != null) {
            numberType.setSigned(true);
        }
        if (zeroFill != null) {
            numberType.setZeroFill(true);
        }
    }

    private List<String> getArgs(Precision_int_numContext ctx) {
        if (ctx == null) {
            return Collections.emptyList();
        }
        return ctx.INTNUM().stream().map(ParseTree::getText).collect(Collectors.toList());
    }

    private List<String> getArgs(Data_type_precisionContext ctx) {
        if (ctx == null) {
            return Collections.emptyList();
        }
        if (ctx.precision_decimal_num() != null) {
            return Collections.singletonList(ctx.precision_decimal_num().DECIMAL_VAL().getText());
        }
        return getArgs(ctx.precision_int_num());
    }

    private String getLength(String_length_iContext ctx) {
        if (ctx == null) {
            return null;
        }
        return ctx.number_literal().getText();
    }

}
