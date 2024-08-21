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
package com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.dropindex.DropIndex;
import com.oceanbase.tools.sqlparser.statement.droptable.DropTable;
import com.oceanbase.tools.sqlparser.statement.droptable.TableList;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 17:11
 * @Description: []
 */
public class RelationFactorRewriter implements SqlRewriter {
    @Override
    public RewriteResult rewrite(RewriteContext context) {
        Statement sql = context.getSql();
        if (!supports(sql)) {
            throw new UnsupportedException(
                    "Support statement: CreateTable, CreateIndex, AlterTable, DropTable, DropIndex");
        }
        Pair<Integer, Integer> replacePosition = getRelationFactorPosition(sql);
        Set<DataNode> dataNodes = context.getDataNodes();
        if (CollectionUtils.isEmpty(dataNodes)) {
            return new RewriteResult(Collections.emptyMap());
        }
        Map<DataNode, String> rewriteSqls = new HashMap<>();
        RewriteResult result = new RewriteResult(rewriteSqls);
        dataNodes.stream().forEach(dataNode -> {
            StringBuilder sb = new StringBuilder(sql.getText());
            rewriteSqls.putIfAbsent(dataNode,
                    sb.replace(replacePosition.left, replacePosition.right + 1, dataNode.getFullName()).toString());
        });
        return result;
    }

    @Override
    public boolean supports(Statement statement) {
        return statement instanceof CreateTable || statement instanceof CreateIndex || statement instanceof AlterTable
                || statement instanceof DropTable || statement instanceof DropIndex;
    }

    private Pair<Integer, Integer> getRelationFactorPosition(Statement statement) {
        if (statement instanceof CreateTable) {
            RelationFactor relation = ((CreateTable) statement).getRelation();
            return new Pair<>(relation.getStart(), relation.getStop());
        } else if (statement instanceof CreateIndex) {
            RelationFactor relation = ((CreateIndex) statement).getOn();
            return new Pair<>(relation.getStart(), relation.getStop());
        } else if (statement instanceof AlterTable) {
            RelationFactor relation = ((AlterTable) statement).getRelation();
            return new Pair<>(relation.getStart(), relation.getStop());
        } else if (statement instanceof DropTable) {
            DropTable dropTable = (DropTable) statement;
            TableList tableList = dropTable.getTableList();
            if (Objects.nonNull(tableList)) {
                return new Pair<>(tableList.getStart(), tableList.getStop());
            } else {
                List<RelationFactor> relations = dropTable.getRelations();
                if (CollectionUtils.isNotEmpty(relations)) {
                    return new Pair<>(relations.get(0).getStart(), relations.get(0).getStop());
                }
            }
        } else if (statement instanceof DropIndex) {
            RelationFactor relation = ((DropIndex) statement).getRelation();
            return new Pair<>(relation.getStart(), relation.getStop());
        }
        throw new UnsupportedException("Support statement: CreateTable, CreateIndex, AlterTable, DropTable, DropIndex");
    }
}
