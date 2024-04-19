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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.druid.DbType;
import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;
import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;
import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.config.DefaultFlowableConfiguration.NoForeignKeyInitializer;
import com.oceanbase.odc.config.jpa.EnhancedJpaRepository;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.datasource.ProxyDataSource;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.task.TaskThreadFactory;

import lombok.NonNull;

/**
 * @author shaobo.zsb
 * @date 2018/09/17
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@MapperScan(basePackages = {"com.oceanbase.odc.metadb"},
        sqlSessionFactoryRef = "metadbSqlSessionFactory")
@EnableJpaRepositories(basePackages = {"com.oceanbase.odc.metadb"}, repositoryBaseClass = EnhancedJpaRepository.class,
        entityManagerFactoryRef = "metadbEntityManagerFactory", transactionManagerRef = "metadbTransactionManager")
@EntityScan({"com.oceanbase.odc.metadb"})
@EnableTransactionManagement
public class MetaDBConfiguration {

    static final String MAPPER_LOCATION = "classpath*:mapper/*.xml";
    private ThreadPoolExecutor bootstrapExecutor;

    @PostConstruct
    public void init() {
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        bootstrapExecutor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new TaskThreadFactory("bootstrap-executor"),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @PreDestroy
    public void destory() {
        ExecutorUtils.gracefulShutdown(bootstrapExecutor, "bootstrapExecutor", 5);
    }

    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Primary
    @Bean(name = "metadbTransactionManager")
    public PlatformTransactionManager metadbTransactionManager(
            LocalContainerEntityManagerFactoryBean metadbEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(metadbEntityManagerFactory.getObject()));
    }

    @Bean
    @ConditionalOnProperty(value = "odc.system.monitor.enabled", havingValue = "true")
    public DruidStatInterceptor druidStatInterceptor() {
        return new DruidStatInterceptor();
    }

    @Bean
    @ConditionalOnProperty(value = "odc.system.monitor.enabled", havingValue = "true")
    public StatFilter statFilter() {
        StatFilter statFilter = new StatFilter(){
            @Override
            public ConnectionProxy connection_connect(FilterChain chain, Properties info) throws SQLException {
                // disable foreign key
                return super.connection_connect(chain, info);
            }
        };
        // use mysql parser to merge sql
        statFilter.setDbType(DbType.mysql);
        statFilter.setMergeSql(true);
        return statFilter;
    }

    @Primary
    @Bean(name = "metadbSqlSessionFactory")
    @DependsOn("metadbMigrate")
    public SqlSessionFactory metadbSqlSessionFactory(DataSource dataSource) throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        Resource[] mapperLocations = new PathMatchingResourcePatternResolver()
                .getResources(MetaDBConfiguration.MAPPER_LOCATION);
        sessionFactory.setMapperLocations(mapperLocations);
        sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource("classpath:/mybatis-config.xml"));
        return sessionFactory.getObject();
    }

    @Primary
    @Bean(name = "metadbEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean metadbEntityManagerFactory(EntityManagerFactoryBuilder builder,
            DataSource dataSource) {
        builder.setBootstrapExecutor(new ConcurrentTaskExecutor(bootstrapExecutor));
        return builder.dataSource(dataSource).packages("com.oceanbase.odc.metadb").build();
    }



    public static class ProxyDruidDataSource extends DruidDataSource {


    }


    static class NoForeignKeyInitializer implements ConnectionInitializer {

        @Override
        public void init(Connection connection) throws SQLException {
            String result = getForeignKeyChecks(connection);
            if ("OFF".equalsIgnoreCase(result)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("set session foreign_key_checks='OFF'");
            }
        }

        private String getForeignKeyChecks(@NonNull Connection connection) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("show variables like 'foreign_key_checks'")) {
                    Verify.verify(resultSet.next(), "No variable value");
                    return resultSet.getString(2);
                }
            }
        }
    }


}
