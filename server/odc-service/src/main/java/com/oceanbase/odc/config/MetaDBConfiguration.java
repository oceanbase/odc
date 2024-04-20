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
import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;
import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.config.jpa.EnhancedJpaRepository;
import com.oceanbase.odc.core.task.TaskThreadFactory;

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
        StatFilter statFilter = new StatFilter();
        // use mysql parser to merge sql
        statFilter.setDbType(DbType.mysql);
        statFilter.setMergeSql(true);
        return statFilter;
    }

    @Bean
    public Filter foreignKeyChecksFilter() {
        return new ForeignKeyChecksFilter();
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
}
