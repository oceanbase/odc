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
package com.oceanbase.odc.metadb.connection;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.ExpressionImpl;
import org.springframework.data.jpa.domain.JpaSort.JpaOrder;

import lombok.NonNull;

/**
 * {@link JpaOrderExpression}
 *
 * @author yh263208
 * @date 2022-11-30 14:57
 * @since ODC_release_4.1.0
 * @see ExpressionImpl
 */
public class JpaOrderExpression<T> extends ExpressionImpl<T> {

    private final JpaOrder jpaOrder;

    public JpaOrderExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> javaType, @NonNull JpaOrder jpaOrder) {
        super(criteriaBuilder, javaType);
        this.jpaOrder = jpaOrder;
    }

    @Override
    public void registerParameters(ParameterRegistry registry) {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @Override
    public String render(RenderingContext renderingContext) {
        return jpaOrder.getProperty();
    }

}
