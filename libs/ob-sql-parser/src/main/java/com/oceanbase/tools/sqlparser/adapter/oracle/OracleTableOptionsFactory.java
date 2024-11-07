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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Compress_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Parallel_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Physical_attributes_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_option_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_option_list_space_seperatedContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.BoolValue;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

import lombok.NonNull;

/**
 * {@link OracleTableOptionsFactory}
 *
 * @author yh263208
 * @date 2023-05-29 18:57
 * @since ODC_release_4.2.0
 */
public class OracleTableOptionsFactory extends OBParserBaseVisitor<TableOptions>
        implements StatementFactory<TableOptions> {

    private final ParserRuleContext parserRuleContext;

    public OracleTableOptionsFactory(@NonNull Table_option_listContext tableOptionListContext) {
        this.parserRuleContext = tableOptionListContext;
    }

    public OracleTableOptionsFactory(@NonNull Table_option_list_space_seperatedContext tableOptionListContext) {
        this.parserRuleContext = tableOptionListContext;
    }

    @Override
    public TableOptions generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public TableOptions visitTable_option_list(Table_option_listContext ctx) {
        if (ctx.table_option_list_space_seperated() != null) {
            return visit(ctx.table_option_list_space_seperated());
        }
        TableOptions target = new TableOptions(ctx);
        target.merge(visit(ctx.table_option()));
        target.merge(visit(ctx.table_option_list()));
        return target;
    }

    @Override
    public TableOptions visitTable_option_list_space_seperated(Table_option_list_space_seperatedContext ctx) {
        TableOptions target = new TableOptions(ctx);
        target.merge(visit(ctx.table_option()));
        if (ctx.table_option_list_space_seperated() != null) {
            target.merge(visit(ctx.table_option_list_space_seperated()));
        }
        return target;
    }

    @Override
    public TableOptions visitTable_option(Table_optionContext ctx) {
        TableOptions target = new TableOptions(ctx);
        if (ctx.SORTKEY() != null) {
            target.setSortKeys(ctx.column_name_list().column_name()
                    .stream().map(c -> new ColumnReference(c, null, null, c.getText()))
                    .collect(Collectors.toList()));
        } else if (ctx.parallel_option() != null) {
            target.merge(visit(ctx.parallel_option()));
        } else if (ctx.TABLE_MODE() != null) {
            target.setTableMode(ctx.STRING_VALUE().getText());
        } else if (ctx.DUPLICATE_SCOPE() != null) {
            target.setDuplicateScope(ctx.STRING_VALUE().getText());
        } else if (ctx.LOCALITY() != null) {
            String force = ctx.FORCE() == null ? "" : " " + ctx.FORCE().getText();
            target.setLocality(ctx.locality_name().getText() + force);
        } else if (ctx.EXPIRE_INFO() != null) {
            target.setExpireInfo(new OracleExpressionFactory(ctx.bit_expr()).generate());
        } else if (ctx.PROGRESSIVE_MERGE_NUM() != null) {
            target.setProgressiveMergeNum(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.BLOCK_SIZE() != null) {
            target.setBlockSize(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.TABLE_ID() != null) {
            target.setTableId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.REPLICA_NUM() != null) {
            target.setReplicaNum(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.compress_option() != null) {
            target.merge(visit(ctx.compress_option()));
        } else if (ctx.USE_BLOOM_FILTER() != null) {
            target.setUseBloomFilter(Boolean.valueOf(ctx.BOOL_VALUE().getText()));
        } else if (ctx.PRIMARY_ZONE() != null) {
            target.setPrimaryZone(ctx.primary_zone_name().getText());
        } else if (ctx.TABLEGROUP() != null) {
            target.setTableGroup(ctx.relation_name_or_string().getText());
        } else if (ctx.read_only_or_write() != null) {
            if (ctx.read_only_or_write().ONLY() != null) {
                target.setReadOnly(true);
            } else if (ctx.read_only_or_write().WRITE() != null) {
                target.setReadWrite(true);
            }
        } else if (ctx.ENGINE_() != null) {
            target.setEngine(ctx.relation_name_or_string().getText());
        } else if (ctx.TABLET_SIZE() != null) {
            target.setTabletSize(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.MAX_USED_PART_ID() != null) {
            target.setMaxUsedPartId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.ENABLE() != null && ctx.ROW() != null) {
            target.setEnableRowMovement(true);
        } else if (ctx.DISABLE() != null && ctx.ROW() != null) {
            target.setDisableRowMovement(true);
        } else if (ctx.physical_attributes_option() != null) {
            target.merge(visit(ctx.physical_attributes_option()));
        } else if (ctx.ENABLE_EXTENDED_ROWID() != null) {
            target.setEnableExtendedRowId(Boolean.valueOf(ctx.BOOL_VALUE().getText()));
        } else if (ctx.LOCATION() != null) {
            target.setLocation(ctx.STRING_VALUE().getText());
        } else if (ctx.FORMAT() != null) {
            Map<String, Expression> formatMap = new HashMap<>();
            ctx.external_file_format_list().external_file_format().forEach(e -> {
                Expression value = null;
                if (e.STRING_VALUE() != null) {
                    value = new ConstExpression(e.STRING_VALUE());
                } else if (e.bit_expr() != null) {
                    value = new OracleExpressionFactory(e.bit_expr()).generate();
                } else if (e.INTNUM() != null) {
                    value = new ConstExpression(e.INTNUM());
                } else if (e.BOOL_VALUE() != null) {
                    value = new BoolValue(e.BOOL_VALUE());
                } else if (e.expr_list() != null) {
                    List<Expression> exprs = e.expr_list().bit_expr().stream()
                            .map(ex -> new OracleExpressionFactory(ex).generate())
                            .collect(Collectors.toList());
                    value = new CollectionExpression(e.expr_list(), exprs);
                } else if (e.compression_name() != null) {
                    value = new ConstExpression(e.compression_name());
                }
                formatMap.put(e.format_key.getText().toUpperCase(), value);
            });
            target.setFormat(formatMap);
        } else if (ctx.PATTERN() != null) {
            target.setPattern(ctx.STRING_VALUE().getText());
        } else if (ctx.PROPERTIES() != null) {
            Map<String, String> externalProperties = new HashMap<>();
            ctx.external_properties_list().external_properties().forEach(e -> {
                externalProperties.put(e.external_properties_key().getText(), e.STRING_VALUE().getText());
            });
            target.setExternalProperties(externalProperties);
        } else if (ctx.PARTITION_TYPE() != null) {
            target.setPartitionType(ctx.USER_SPECIFIED().getText());
        } else if (ctx.MICRO_INDEX_CLUSTERED() != null) {
            target.setMicroIndexClustered(Boolean.valueOf(ctx.BOOL_VALUE().getText()));
        } else if (ctx.AUTO_REFRESH() != null) {
            if (ctx.OFF() != null) {
                target.setAutoRefresh(ctx.OFF().getText());
            } else if (ctx.IMMEDIATE() != null) {
                target.setAutoRefresh(ctx.IMMEDIATE().getText());
            } else if (ctx.INTERVAL() != null) {
                target.setAutoRefresh(ctx.INTERVAL().getText());
            }
        }
        return target;
    }

    @Override
    public TableOptions visitParallel_option(Parallel_optionContext ctx) {
        TableOptions tableOptions = new TableOptions(ctx);
        if (ctx.NOPARALLEL() != null) {
            tableOptions.setNoParallel(true);
        } else {
            tableOptions.setParallel(Integer.valueOf(ctx.INTNUM().getText()));
        }
        return tableOptions;
    }

    @Override
    public TableOptions visitCompress_option(Compress_optionContext ctx) {
        TableOptions tableOptions = new TableOptions(ctx);
        if (ctx.NOCOMPRESS() != null) {
            tableOptions.setNoCompress(true);
            return tableOptions;
        }
        CharStream input = ctx.getStart().getInputStream();
        String str = input.getText(Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex()));
        int index = str.indexOf(ctx.COMPRESS().getText());
        if (index >= 0) {
            str = str.substring(index + ctx.COMPRESS().getText().length()).trim();
        }
        tableOptions.setCompress(str);
        return tableOptions;
    }

    @Override
    public TableOptions visitPhysical_attributes_option(Physical_attributes_optionContext ctx) {
        TableOptions tableOptions = new TableOptions(ctx);
        Integer num = ctx.INTNUM() == null ? null : Integer.valueOf(ctx.INTNUM().getText());
        if (ctx.PCTFREE() != null) {
            tableOptions.setPctFree(num);
        } else if (ctx.PCTUSED() != null) {
            tableOptions.setPctUsed(num);
        } else if (ctx.INITRANS() != null) {
            tableOptions.setIniTrans(num);
        } else if (ctx.MAXTRANS() != null) {
            tableOptions.setMaxTrans(num);
        } else if (ctx.STORAGE() != null) {
            tableOptions.setStorage(ctx.storage_options_list().storage_option().stream().map(i -> {
                CharStream input = i.getStart().getInputStream();
                return input.getText(Interval.of(i.getStart().getStartIndex(), i.getStop().getStopIndex()));
            }).collect(Collectors.toList()));
        } else if (ctx.TABLESPACE() != null) {
            tableOptions.setTableSpace(ctx.tablespace().getText());
        }
        return tableOptions;
    }

}
