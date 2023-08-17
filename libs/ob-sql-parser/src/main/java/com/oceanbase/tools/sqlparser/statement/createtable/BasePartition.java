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
package com.oceanbase.tools.sqlparser.statement.createtable;

import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = false)
abstract class BasePartition extends BaseStatement implements Partition {

    private final List<? extends Expression> targets;
    private final List<? extends PartitionElement> elements;
    private final Integer partitionsNum;
    private final SubPartitionOption subPartitionOption;

    public BasePartition(@NonNull ParserRuleContext context,
            List<? extends Expression> targets,
            List<? extends PartitionElement> elements,
            SubPartitionOption subPartitionOption, Integer partitionsNum) {
        super(context);
        this.partitionsNum = partitionsNum;
        this.subPartitionOption = subPartitionOption;
        this.targets = targets;
        this.elements = elements;
    }

    public BasePartition(List<? extends Expression> targets,
            List<? extends PartitionElement> elements,
            SubPartitionOption subPartitionOption, Integer partitionsNum) {
        this.partitionsNum = partitionsNum;
        this.subPartitionOption = subPartitionOption;
        this.targets = targets;
        this.elements = elements;
    }

    @Override
    public List<? extends Expression> getPartitionTargets() {
        return this.targets;
    }

    @Override
    public SubPartitionOption getSubPartitionOption() {
        return this.subPartitionOption;
    }

    @Override
    public Integer getPartitionsNum() {
        return this.partitionsNum;
    }

    @Override
    public List<? extends PartitionElement> getPartitionElements() {
        return CollectionUtils.isEmpty(elements) ? Collections.emptyList() : this.elements;
    }

}
