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
package com.oceanbase.odc.service.session.factory;

import java.util.Map;

import javax.sql.DataSource;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.datasource.ProxyDataSource;

import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ProxyDataSource} factory, used to lazy load
 *
 * @author yh263208
 * @date 2022-01-07 16:15
 * @since ODC_release_3.3.0
 */
public class ProxyDataSourceFactory implements CloneableDataSourceFactory {
    @Setter
    private ConnectionInitializer initializer;
    private final CloneableDataSourceFactory dataSourceFactory;

    public ProxyDataSourceFactory(@NonNull CloneableDataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public ProxyDataSource getDataSource() {
        DataSource target = this.dataSourceFactory.getDataSource();
        if (target == null) {
            throw new IllegalStateException("Target dataSource can not be null");
        }
        ProxyDataSource proxyDataSource = new ProxyDataSource(target);
        proxyDataSource.setInitializer(initializer);
        return proxyDataSource;
    }

    @Override
    public CloneableDataSourceFactory deepCopy() {
        CloneableDataSourceFactory deepCopy = dataSourceFactory.deepCopy();
        return new ProxyDataSourceFactory(deepCopy);
    }

    @Override
    public void resetUsername(@NonNull CloneableDataSourceFactory.ValueMapper<String> mapper) {
        dataSourceFactory.resetUsername(mapper);
    }

    @Override
    public void resetPassword(@NonNull CloneableDataSourceFactory.ValueMapper<String> mapper) {
        dataSourceFactory.resetPassword(mapper);
    }

    @Override
    public void resetHost(@NonNull CloneableDataSourceFactory.ValueMapper<String> mapper) {
        dataSourceFactory.resetHost(mapper);
    }

    @Override
    public void resetPort(@NonNull CloneableDataSourceFactory.ValueMapper<Integer> mapper) {
        dataSourceFactory.resetPort(mapper);
    }

    @Override
    public void resetSchema(@NonNull CloneableDataSourceFactory.ValueMapper<String> mapper) {
        dataSourceFactory.resetSchema(mapper);
    }

    @Override
    public void resetParameters(@NonNull CloneableDataSourceFactory.ValueMapper<Map<String, String>> mapper) {
        dataSourceFactory.resetParameters(mapper);
    }
}
