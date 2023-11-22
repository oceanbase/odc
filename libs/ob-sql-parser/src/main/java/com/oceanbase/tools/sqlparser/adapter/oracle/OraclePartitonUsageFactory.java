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

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Name_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Use_partitionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.select.PartitionType;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;

import lombok.NonNull;

/**
 * {@link OraclePartitonUsageFactory}
 *
 * @author jingtian
 * @date 2023/4/24
 * @since ODC_4.2.0
 */
public class OraclePartitonUsageFactory extends OBParserBaseVisitor<PartitionUsage>
        implements StatementFactory<PartitionUsage> {

    private final Use_partitionContext usePartitionContext;

    public OraclePartitonUsageFactory(@NonNull Use_partitionContext usePartitionContext) {
        this.usePartitionContext = usePartitionContext;
    }

    @Override
    public PartitionUsage generate() {
        return visit(this.usePartitionContext);
    }

    @Override
    public PartitionUsage visitUse_partition(Use_partitionContext ctx) {
        PartitionType type = PartitionType.PARTITION;
        if (ctx.SUBPARTITION() != null) {
            type = PartitionType.SUB_PARTITION;
        }
        List<String> nameList = new ArrayList<>();
        visitNameList(ctx.name_list(), nameList);
        return new PartitionUsage(ctx, type, nameList);
    }

    private void visitNameList(Name_listContext ctx, List<String> nameList) {
        if (ctx.relation_name() != null && ctx.name_list() == null) {
            nameList.add(ctx.relation_name().getText());
            return;
        }
        visitNameList(ctx.name_list(), nameList);
        nameList.add(ctx.relation_name().getText());
    }

}
