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
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Parallel_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_option_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_option_list_space_seperatedContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link MySQLTableOptionsFactory}
 *
 * @author yh263208
 * @date 2023-05-29 21:26
 * @since ODC_release_4.2.0
 */
public class MySQLTableOptionsFactory extends OBParserBaseVisitor<TableOptions>
        implements StatementFactory<TableOptions> {

    private final ParserRuleContext parserRuleContext;

    public MySQLTableOptionsFactory(@NonNull Table_option_listContext tableOptionListContext) {
        this.parserRuleContext = tableOptionListContext;
    }

    public MySQLTableOptionsFactory(@NonNull Table_option_list_space_seperatedContext context) {
        this.parserRuleContext = context;
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
        ctx.table_option().forEach(c -> target.merge(visit(c)));
        return target;
    }

    @Override
    public TableOptions visitTable_option(Table_optionContext ctx) {
        TableOptions target = new TableOptions(ctx);
        if (ctx.SORTKEY() != null) {
            target.setSortKeys(ctx.column_name_list().column_name()
                    .stream().map(c -> new ColumnReference(c, null, null, c.getText()))
                    .collect(Collectors.toList()));
        } else if (ctx.TABLE_MODE() != null) {
            target.setTableMode(ctx.STRING_VALUE().getText());
        } else if (ctx.DUPLICATE_SCOPE() != null) {
            target.setDuplicateScope(ctx.STRING_VALUE().getText());
        } else if (ctx.COMMENT() != null) {
            target.setComment(ctx.STRING_VALUE().getText());
        } else if (ctx.COMPRESSION() != null) {
            target.setCompression(ctx.STRING_VALUE().getText());
        } else if (ctx.LOCALITY() != null) {
            String force = ctx.FORCE() == null ? "" : " " + ctx.FORCE().getText();
            target.setLocality(ctx.locality_name().getText() + force);
        } else if (ctx.EXPIRE_INFO() != null) {
            target.setExpireInfo(new MySQLExpressionFactory(ctx.expr()).generate());
        } else if (ctx.PROGRESSIVE_MERGE_NUM() != null) {
            target.setProgressiveMergeNum(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.BLOCK_SIZE() != null) {
            target.setBlockSize(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.TABLE_ID() != null) {
            target.setTableId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.REPLICA_NUM() != null) {
            target.setReplicaNum(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.STORAGE_FORMAT_VERSION() != null) {
            target.setStorageFormatVersion(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.TABLET_SIZE() != null) {
            target.setTabletSize(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.PCTFREE() != null) {
            target.setPctFree(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.MAX_USED_PART_ID() != null) {
            target.setMaxUsedPartId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.ROW_FORMAT() != null) {
            target.setRowFormat(ctx.row_format_option().getText());
        } else if (ctx.USE_BLOOM_FILTER() != null) {
            target.setUseBloomFilter(Boolean.valueOf(ctx.BOOL_VALUE().getText()));
        } else if (ctx.charset_name() != null) {
            target.setCharset(ctx.charset_name().getText());
        } else if (ctx.collation_name() != null) {
            target.setCollation(ctx.collation_name().getText());
        } else if (ctx.PRIMARY_ZONE() != null) {
            target.setPrimaryZone(ctx.primary_zone_name().getText());
        } else if (ctx.TABLEGROUP() != null) {
            target.setTableGroup(ctx.relation_name_or_string().getText());
        } else if (ctx.ENGINE_() != null) {
            target.setEngine(ctx.relation_name_or_string().getText());
        } else if (ctx.AUTO_INCREMENT() != null) {
            target.setAutoIncrement(new BigDecimal(ctx.int_or_decimal().getText()));
        } else if (ctx.read_only_or_write() != null) {
            if (ctx.read_only_or_write().ONLY() != null) {
                target.setReadOnly(true);
            } else if (ctx.read_only_or_write().WRITE() != null) {
                target.setReadWrite(true);
            }
        } else if (ctx.TABLESPACE() != null) {
            target.setTableSpace(ctx.tablespace().getText());
        } else if (ctx.parallel_option() != null) {
            target.merge(visit(ctx.parallel_option()));
        } else if (ctx.DELAY_KEY_WRITE() != null) {
            target.setDelayKeyWrite(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.AVG_ROW_LENGTH() != null) {
            target.setAvgRowLength(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.CHECKSUM() != null) {
            target.setChecksum(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.AUTO_INCREMENT_MODE() != null) {
            target.setAutoIncrementMode(ctx.STRING_VALUE().getText());
        } else if (ctx.ENABLE_EXTENDED_ROWID() != null) {
            target.setEnableExtendedRowId(Boolean.valueOf(ctx.BOOL_VALUE().getText()));
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

}
