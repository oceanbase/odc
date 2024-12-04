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
package com.oceanbase.tools.sqlparser.statement.select;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link PartitionUsage}
 *
 * @author yh263208
 * @date 2022-12-05 20:38
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class PartitionUsage extends BaseStatement {

    private final PartitionType type;
    private final List<String> nameList;
    private final Map<String, Expression> externalTablePartition;

    public PartitionUsage(@NonNull ParserRuleContext context, PartitionType type, @NonNull List<String> nameList) {
        super(context);
        this.type = type;
        this.nameList = nameList;
        this.externalTablePartition = null;
    }

    public PartitionUsage(PartitionType type, @NonNull List<String> nameList) {
        this.type = type;
        this.nameList = nameList;
        this.externalTablePartition = null;
    }

    public PartitionUsage(@NonNull ParserRuleContext context, PartitionType type,
            @NonNull Map<String, Expression> externalTablePartition) {
        super(context);
        this.type = type;
        this.nameList = null;
        this.externalTablePartition = externalTablePartition;
    }

    public PartitionUsage(PartitionType type, @NonNull Map<String, Expression> externalTablePartition) {
        this.type = type;
        this.nameList = null;
        this.externalTablePartition = externalTablePartition;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (type == PartitionType.PARTITION) {
            buffer.append("PARTITION ");
        } else {
            buffer.append("SUBPARTITION ");
        }
        if (this.nameList != null) {
            return buffer.append("(").append(String.join(",", nameList)).append(")").toString();
        }
        return buffer.append("(").append(this.externalTablePartition.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "))).append(")").toString();
    }

}
