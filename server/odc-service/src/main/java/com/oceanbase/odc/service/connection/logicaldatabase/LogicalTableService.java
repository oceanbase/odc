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
package com.oceanbase.odc.service.connection.logicaldatabase;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.hadoop.util.StringUtils;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.parser.DefaultLogicalTableExpressionParser;
import com.oceanbase.odc.service.connection.logicaldatabase.parser.LogicalTableExpression;

/**
 * @Author: Lebie
 * @Date: 2024/4/23 11:31
 * @Description: []
 */
@Service
public class LogicalTableService {
    private final DefaultLogicalTableExpressionParser parser = new DefaultLogicalTableExpressionParser();

    public List<DataNode> resolve(String expression) {
        PreConditions.notEmpty(expression, "expression");

        List<String> expressions = Arrays.asList(StringUtils.split(expression, ','));
        LogicalTableExpression logicalTableExpression;
        try {
            logicalTableExpression = (LogicalTableExpression) parser.parse(expression);
        } catch (Exception e) {
            throw new BadArgumentException("invalid logical table expression", e);
        }
        return logicalTableExpression.listNames().stream().map(name -> {
            String[] parts = name.split("\\.");
            if (parts.length != 2) {
                throw new UnexpectedException("invalid logical table expression");
            }
            return new DataNode(parts[0], parts[1]);
        }).collect(Collectors.toList());
    }
}
