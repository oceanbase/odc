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
package com.oceanbase.odc.service.task.service;

import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
public class SpringTransactionManager implements TransactionManager {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionManager(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T doInTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    @Override
    public void doInTransactionWithoutResult(Supplier<Void> action) {
        transactionTemplate.executeWithoutResult(status -> action.get());
    }
}
