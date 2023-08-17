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
package com.oceanbase.odc.service.onlineschemachange.rename;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

/**
 * @author yaobin
 * @date 2023-08-04
 * @since 4.2.0
 */
public class DefaultRenameTableInvoker implements RenameTableInvoker {

    private final List<RenameTableInterceptor> interceptors;

    public DefaultRenameTableInvoker(ConnectionSession connSession,
            DBSessionManageFacade dbSessionManageFacade) {
        List<RenameTableInterceptor> interceptors = new LinkedList<>();

        LockRenameTableFactory lockRenameTableFactory = new LockRenameTableFactory();
        RenameTableInterceptor lockInterceptor =
                lockRenameTableFactory.generate(connSession, dbSessionManageFacade);
        interceptors.add(lockInterceptor);
        HandlerTableInterceptor handlerTableInterceptor =
                new HandlerTableInterceptor(connSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY));
        interceptors.add(handlerTableInterceptor);
        interceptors.add(new ForeignKeyInterceptor(connSession));
        this.interceptors = interceptors;
    }

    @Override
    public void invoke(Supplier<Void> renameTableSupplier, RenameTableParameters parameters) {
        renameInInterceptor(renameTableSupplier, parameters);
    }

    private void renameInInterceptor(Supplier<Void> renameTableSupplier, RenameTableParameters parameters) {
        try {
            preRename(parameters);
            renameTableSupplier.get();
            renameSucceed(parameters);
        } catch (Exception ex) {
            renameFailed(parameters);
        } finally {
            postRenamed(parameters);
        }
    }

    private void preRename(RenameTableParameters parameters) {
        interceptors.forEach(r -> r.preRename(parameters));
    }

    private void renameSucceed(RenameTableParameters parameters) {
        reverseConsumerInterceptor(r -> r.renameSucceed(parameters));
    }

    private void renameFailed(RenameTableParameters parameters) {
        reverseConsumerInterceptor(r -> r.renameFailed(parameters));
    }

    private void postRenamed(RenameTableParameters parameters) {
        reverseConsumerInterceptor(r -> r.postRenamed(parameters));
    }

    private void reverseConsumerInterceptor(Consumer<RenameTableInterceptor> interceptorConsumer) {
        ListIterator<RenameTableInterceptor> listIterator = interceptors.listIterator(interceptors.size());
        while (listIterator.hasPrevious()) {
            interceptorConsumer.accept(listIterator.previous());
        }
    }
}
