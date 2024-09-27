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
package com.oceanbase.odc.config;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.config.jpa.EnhancedJpaRepository;
import com.oceanbase.odc.core.task.TaskThreadFactory;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@MapperScan(basePackages = {"com.oceanbase.odc.vectordb"})
@EnableJpaRepositories(
        basePackages = {"com.oceanbase.odc.vectordb"},
        repositoryBaseClass = EnhancedJpaRepository.class,
        transactionManagerRef = "vectordbTransactionManager",
        entityManagerFactoryRef = "vectordbEntityManagerFactory")
@EntityScan({"com.oceanbase.odc.vectordb"})
@EnableTransactionManagement
@ConditionalOnProperty(prefix = "odc.datasource.vectordb",
        name = {"jdbc-url", "driver-class-name", "username", "password"})
public class VectorDBConfiguration {

    private ThreadPoolExecutor vectorDbBootstrapExecutor;

    @PostConstruct
    public void init() {
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        this.vectorDbBootstrapExecutor = new ThreadPoolExecutor(poolSize, poolSize, 0,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new TaskThreadFactory("vectordb-bootstrap-executor"), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @PreDestroy
    public void destroy() {
        ExecutorUtils.gracefulShutdown(vectorDbBootstrapExecutor, "vectordb-bootstrap-executor", 5);
    }

    @Bean(name = "vectordbDataSource")
    @ConfigurationProperties("odc.datasource.vectordb")
    public DataSource vectordbDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "vectordbEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean vectordbEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("vectordbDataSource") DataSource dataSource) {
        builder.setBootstrapExecutor(new ConcurrentTaskExecutor(vectorDbBootstrapExecutor));
        return builder.dataSource(dataSource).packages("com.oceanbase.odc.vectordb").build();
    }

    @Bean(name = "vectordbTransactionManager")
    public PlatformTransactionManager vectordbTransactionManager(
            @Qualifier("vectordbEntityManagerFactory") LocalContainerEntityManagerFactoryBean vectordbEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(vectordbEntityManagerFactory.getObject()));
    }

}
