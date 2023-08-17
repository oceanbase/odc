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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

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

    public PartitionUsage(@NonNull ParserRuleContext context, PartitionType type, @NonNull List<String> nameList) {
        super(context);
        this.type = type;
        this.nameList = nameList;
    }

    public PartitionUsage(PartitionType type, @NonNull List<String> nameList) {
        this.type = type;
        this.nameList = nameList;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (type == PartitionType.PARTITION) {
            buffer.append("PARTITION ");
        } else {
            buffer.append("SUBPARTITION ");
        }
        return buffer.append("(").append(String.join(",", nameList)).append(")").toString();
    }

}
