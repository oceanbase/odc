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
package com.oceanbase.tools.sqlparser.statement.common;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.NonNull;

public abstract class BaseOptions extends BaseStatement {

    protected BaseOptions() {
        super(null, null);
    }

    protected BaseOptions(TerminalNode terminalNode) {
        super(null, terminalNode);
    }

    protected BaseOptions(ParserRuleContext ruleNode) {
        super(ruleNode, null);
    }

    public <T extends BaseOptions> void merge(@NonNull T other) {
        if (!Objects.equals(other.getClass(), this.getClass())) {
            throw new IllegalArgumentException("Merged object's type is illegal");
        }
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(this.getClass(), BaseStatement.class);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                Object otherVal = pd.getReadMethod().invoke(other);
                if (otherVal == null) {
                    continue;
                }
                doMerge(pd, otherVal);
            }
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void doMerge(PropertyDescriptor pd, Object otherVal)
            throws InvocationTargetException, IllegalAccessException {
        if (pd.getWriteMethod() == null) {
            return;
        }
        pd.getWriteMethod().invoke(this, otherVal);
    }

}
