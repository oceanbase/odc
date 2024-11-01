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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Index_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Index_using_algorithmContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Opt_index_optionsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Parallel_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Visibility_optionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link MySQLIndexOptionsFactory}
 *
 * @author yh263208
 * @date 2023-05-24 19:07
 * @since ODC_release_4.2.0
 */
public class MySQLIndexOptionsFactory extends OBParserBaseVisitor<IndexOptions>
        implements StatementFactory<IndexOptions> {

    private final Opt_index_optionsContext optIndexOptionsContext;

    public MySQLIndexOptionsFactory(@NonNull Opt_index_optionsContext optIndexOptionsContext) {
        this.optIndexOptionsContext = optIndexOptionsContext;
    }

    @Override
    public IndexOptions generate() {
        return visit(this.optIndexOptionsContext);
    }

    @Override
    public IndexOptions visitOpt_index_options(Opt_index_optionsContext ctx) {
        IndexOptions indexOptions = new IndexOptions(ctx);
        ctx.index_option().forEach(option -> {
            if (option.GLOBAL() != null) {
                indexOptions.setGlobal(true);
            } else if (option.LOCAL() != null) {
                indexOptions.setGlobal(false);
            } else if (option.BLOCK_SIZE() != null) {
                indexOptions.setBlockSize(getInteger(option));
            } else if (option.KEY_BLOCK_SIZE() != null) {
                indexOptions.setKeyBlockSize(getInteger(option));
            } else if (option.DATA_TABLE_ID() != null) {
                indexOptions.setDataTableId(getInteger(option));
            } else if (option.INDEX_TABLE_ID() != null) {
                indexOptions.setIndexTableId(getInteger(option));
            } else if (option.VIRTUAL_COLUMN_ID() != null) {
                indexOptions.setVirtualColumnId(getInteger(option));
            } else if (option.MAX_USED_PART_ID() != null) {
                indexOptions.setMaxUsedPartId(getInteger(option));
            } else if (option.COMMENT() != null) {
                indexOptions.setComment(option.STRING_VALUE().getText());
            } else if (option.STORING() != null) {
                indexOptions.setStoring(getReference(option));
            } else if (option.CTXCAT() != null) {
                indexOptions.setCtxcat(getReference(option));
            } else if (option.WITH() != null && option.PARSER() != null) {
                indexOptions.setWithParser(option.STRING_VALUE().getText());
            } else if (option.WITH() != null && option.ROWID() != null) {
                indexOptions.setWithRowId(true);
            } else if (option.WITH() != null && option.vec_index_params() != null) {
                Map<String, String> params = new HashMap<>();
                option.vec_index_params().vec_index_param()
                        .forEach(i -> params.put(i.relation_name().getText(), i.vec_index_param_value().getText()));
                indexOptions.setVectorIndexParams(params);
            } else if (option.index_using_algorithm() != null) {
                indexOptions.merge(visit(option.index_using_algorithm()));
            } else if (option.visibility_option() != null) {
                indexOptions.merge(visit(option.visibility_option()));
            } else if (option.parallel_option() != null) {
                indexOptions.merge(visit(option.parallel_option()));
            }
        });
        return indexOptions;
    }

    @Override
    public IndexOptions visitIndex_using_algorithm(Index_using_algorithmContext ctx) {
        IndexOptions indexOptions = new IndexOptions(ctx);
        if (ctx.BTREE() != null) {
            indexOptions.setUsingBtree(true);
        } else if (ctx.HASH() != null) {
            indexOptions.setUsingHash(true);
        }
        return indexOptions;
    }

    @Override
    public IndexOptions visitVisibility_option(Visibility_optionContext ctx) {
        IndexOptions indexOptions = new IndexOptions(ctx);
        indexOptions.setVisible(ctx.VISIBLE() != null);
        return indexOptions;
    }

    @Override
    public IndexOptions visitParallel_option(Parallel_optionContext ctx) {
        IndexOptions indexOptions = new IndexOptions(ctx);
        if (ctx.NOPARALLEL() != null) {
            indexOptions.setNoParallel(true);
        } else {
            indexOptions.setParallel(Integer.valueOf(ctx.INTNUM().getText()));
        }
        return indexOptions;
    }

    private List<ColumnReference> getReference(Index_optionContext option) {
        if (option.column_name_list() == null) {
            return null;
        }
        return option.column_name_list().column_name().stream()
                .map(c -> new ColumnReference(c, null, null, c.getText())).collect(Collectors.toList());
    }

    private Integer getInteger(Index_optionContext option) {
        if (option.INTNUM() == null) {
            return null;
        }
        return Integer.valueOf(option.INTNUM().getText());
    }

}
