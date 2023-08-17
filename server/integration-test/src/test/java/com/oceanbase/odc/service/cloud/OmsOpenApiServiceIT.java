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
package com.oceanbase.odc.service.cloud;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.ITConfigurations;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.config.RestTemplateConfig;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.oms.client.DefaultPrivateCloudOmsClient;
import com.oceanbase.odc.service.onlineschemachange.oms.client.OmsClient;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.CheckerObjectStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.CheckerResultType;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsOceanBaseType;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.ProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiServiceImpl;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiServiceImpl;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CommonTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.DatabaseTransferObject;
import com.oceanbase.odc.service.onlineschemachange.oms.request.FullTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.IncrTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.SpecificTransferMapping;
import com.oceanbase.odc.service.onlineschemachange.oms.request.TableTransferObject;
import com.oceanbase.odc.service.onlineschemachange.oms.response.FullVerifyTableStatisticVO;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectStepVO;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.database.TestDBUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-02
 * @since 4.2.0
 */
@Ignore("manual verify only, may cost plenty of time and cause oms working thread exhausted")
@Slf4j
public class OmsOpenApiServiceIT {

    private static DataSourceOpenApiService dataSourceOpenApiService;
    private static ProjectOpenApiService projectOpenApiService;

    private static TestDBConfiguration config;
    private static JdbcTemplate jdbcTemplate;

    private String datasourceId;
    private String projectId;

    @BeforeClass
    public static void beforeClass() {
        config = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        jdbcTemplate = new JdbcTemplate(config.getDataSource());
        OnlineSchemaChangeProperties configuration = ITConfigurations.getOscPrivateCloudProperties();
        OmsClient omsClient = new DefaultPrivateCloudOmsClient(configuration,
                new RestTemplateConfig().omsRestTemplate());
        dataSourceOpenApiService = new DataSourceOpenApiServiceImpl(omsClient);
        projectOpenApiService = new ProjectOpenApiServiceImpl(omsClient);
    }

    @AfterClass
    public static void afterClass() {
        dropTable();
    }

    @Test
    public void test_oms_openapi() {
        test_create_datasource();
        try {
            test_project_start();
            test_project_migrate();
            test_listProjectFullVerifyResult();
            test_project_control();
        } finally {
            test_project_release_and_delete();
        }
    }

    private void test_create_datasource() {
        CreateOceanBaseDataSourceRequest request = getCreateOceanBaseDataSourceRequest();

        datasourceId = dataSourceOpenApiService.createOceanBaseDataSource(request);
        Assert.assertNotNull("datasourceId is empty", datasourceId);
    }

    private void test_project_start() {
        createTable();

        CreateProjectRequest createProjectRequest = getCreateProjectRequest();
        projectId = projectOpenApiService.createProject(createProjectRequest);
        Assert.assertNotNull(projectId);

        ProjectControlRequest request = getProjectControlRequest(projectId);
        ProjectProgressResponse projectProgressResponse = projectOpenApiService.describeProjectProgress(request);
        projectOpenApiService.startProject(request);
        Assert.assertEquals(ProjectStatusEnum.INIT, projectProgressResponse.getStatus());
        projectProgressResponse = projectOpenApiService.describeProjectProgress(request);
        Assert.assertEquals(ProjectStatusEnum.RUNNING, projectProgressResponse.getStatus());
    }

    private void test_project_migrate() {

        updateTable();

        checkStepFinished(OmsStepName.TRANSFER_PRECHECK, Duration.ofSeconds(30));
        checkStepFinished(OmsStepName.TRANSFER_INCR_LOG_PULL, Duration.ofSeconds(90));
        checkStepFinished(OmsStepName.FULL_TRANSFER, Duration.ofSeconds(60));
        checkStepFinished(OmsStepName.INCR_TRANSFER, Duration.ofSeconds(30));
        checkStepFinished(OmsStepName.FULL_VERIFIER, Duration.ofSeconds(60));

    }

    private void checkStepFinished(OmsStepName omsStepName, Duration duration) {
        Awaitility.with().pollInterval(Duration.ofSeconds(5))
                .and().pollDelay(Duration.ofSeconds(5))
                .and().atMost(duration)
                .await("wait for oms execute over, check step name is " + omsStepName.name())
                .until(() -> {
                    List<ProjectStepVO> projectStepsVO =
                            projectOpenApiService.describeProjectSteps(getProjectControlRequest(projectId));
                    // test get step status cycle
                    Map<OmsStepName, ProjectStepVO> projectStepMap =
                            projectStepsVO.stream().collect(Collectors.toMap(ProjectStepVO::getName, a -> a));
                    log.info("oms return result: {}", JsonUtils.toJson(projectStepMap));
                    Assert.assertNotNull(projectStepsVO);
                    return checkStepFinished(projectStepMap, omsStepName);

                });
    }

    private void test_listProjectFullVerifyResult() {
        ListProjectFullVerifyResultRequest request = new ListProjectFullVerifyResultRequest();
        request.setProjectId(projectId);
        request.setSourceSchemas(new String[] {config.getDefaultDBName()});
        request.setDestSchemas(new String[] {config.getDefaultDBName()});
        request.setStatus(new String[] {"FINISHED", "SUSPEND", "RUNNING"});
        request.setPageSize(10);
        request.setPageNumber(1);

        ProjectFullVerifyResultResponse verifyResultResponse =
                projectOpenApiService.listProjectFullVerifyResult(request);

        Assert.assertNotNull(verifyResultResponse.getDifferentNumber());
        Assert.assertEquals(0, verifyResultResponse.getDifferentNumber().longValue());
        List<FullVerifyTableStatisticVO> fullVerifyTableStatistics =
                verifyResultResponse.getFullVerifyTableStatistics();
        Assert.assertTrue(fullVerifyTableStatistics.size() > 0);
        Assert.assertEquals(CheckerObjectStatus.FINISHED, fullVerifyTableStatistics.get(0).getStatus());
        Assert.assertEquals(CheckerResultType.CONSISTENT, fullVerifyTableStatistics.get(0).getResultType());

    }

    private boolean checkStepFinished(Map<OmsStepName, ProjectStepVO> projectStepMap, OmsStepName name) {
        Assert.assertNotNull(projectStepMap.get(name));
        OmsStepStatus status = projectStepMap.get(name).getStatus();
        Integer progress = projectStepMap.get(name).getProgress();

        Function<Integer, Boolean> competedFunc = (p -> p != null && p == 100);

        switch (name) {
            case INCR_TRANSFER:
                return status == OmsStepStatus.MONITORING && competedFunc.apply(progress);
            case FULL_VERIFIER:
                return status == OmsStepStatus.RUNNING && competedFunc.apply(progress);
            default:
                return status == OmsStepStatus.FINISHED && competedFunc.apply(progress);
        }

    }

    private void test_project_control() {
        ProjectControlRequest request = getProjectControlRequest(projectId);

        projectOpenApiService.stopProject(request);
        ProjectProgressResponse projectProgressResponse = projectOpenApiService.describeProjectProgress(request);
        Assert.assertEquals(ProjectStatusEnum.SUSPEND, projectProgressResponse.getStatus());

        projectOpenApiService.resumeProject(request);
        projectProgressResponse = projectOpenApiService.describeProjectProgress(request);
        Assert.assertEquals(ProjectStatusEnum.RUNNING, projectProgressResponse.getStatus());

        projectOpenApiService.stopProject(request);
        projectProgressResponse = projectOpenApiService.describeProjectProgress(request);
        Assert.assertEquals(ProjectStatusEnum.SUSPEND, projectProgressResponse.getStatus());

    }

    private void test_project_release_and_delete() {
        if (projectId == null) {
            return;
        }

        ProjectControlRequest request = getProjectControlRequest(projectId);

        ProjectProgressResponse projectProgressResponse = projectOpenApiService.describeProjectProgress(request);
        if (projectProgressResponse.getStatus() == ProjectStatusEnum.RUNNING) {
            projectOpenApiService.stopProject(request);
        }

        projectOpenApiService.releaseProject(request);
        projectProgressResponse = projectOpenApiService.describeProjectProgress(request);
        Assert.assertTrue(ProjectStatusEnum.RELEASING == projectProgressResponse.getStatus() ||
                ProjectStatusEnum.RELEASED == projectProgressResponse.getStatus());

        Awaitility.with().pollInterval(Duration.ofSeconds(5))
                .and().pollDelay(Duration.ofSeconds(5))
                .and().atMost(Duration.ofSeconds(70))
                .await("wait for oms released project")
                .until(() -> {
                    ProjectProgressResponse response = projectOpenApiService.describeProjectProgress(request);
                    return response.getStatus() == ProjectStatusEnum.RELEASED;

                });

        projectOpenApiService.deleteProject(request);
    }

    private void createTable() {

        jdbcTemplate.execute("create table t1(id int(20) primary key, name1 varchar(20))");
        jdbcTemplate.execute("insert into t1 values(1,'abc'),(2,'efg')");
        jdbcTemplate.execute("create table t1_gho(id int(20) primary key, name1 varchar(20), name2 varchar(20))");
    }

    private void updateTable() {
        jdbcTemplate.execute("insert into t1 values(3,'abc'),(4,'efg')");
        jdbcTemplate.execute("update t1 set id = 5 where id = 3");
        jdbcTemplate.execute("delete from t1 where id = 2");
    }

    private static void dropTable() {
        jdbcTemplate.execute("drop table if exists t1");
        jdbcTemplate.execute("drop table if exists t1_gho");
    }

    private CreateProjectRequest getCreateProjectRequest() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setSourceEndpointId(datasourceId);
        request.setSinkEndpointId(datasourceId);

        List<DatabaseTransferObject> databaseTransferObjects = new ArrayList<>();
        DatabaseTransferObject databaseTransferObject = new DatabaseTransferObject();
        databaseTransferObject.setName(config.getDefaultDBName());
        databaseTransferObject.setMappedName(config.getDefaultDBName());
        databaseTransferObjects.add(databaseTransferObject);

        List<TableTransferObject> tables = new ArrayList<>();
        TableTransferObject tableTransferObject = new TableTransferObject();
        tableTransferObject.setName("t1");
        tableTransferObject.setMappedName("t1_gho");
        tables.add(tableTransferObject);
        databaseTransferObject.setTables(tables);

        SpecificTransferMapping transferMapping = new SpecificTransferMapping();
        transferMapping.setDatabases(databaseTransferObjects);
        request.setTransferMapping(transferMapping);

        CommonTransferConfig commonTransferConfig = new CommonTransferConfig();
        request.setCommonTransferConfig(commonTransferConfig);

        FullTransferConfig fullTransferConfig = new FullTransferConfig();
        request.setFullTransferConfig(fullTransferConfig);

        IncrTransferConfig incrTransferConfig = new IncrTransferConfig();
        request.setIncrTransferConfig(incrTransferConfig);

        request.setEnableFullVerify(Boolean.TRUE);
        request.setName("IT-" + StringUtils.uuidNoHyphen());
        return request;
    }

    private CreateOceanBaseDataSourceRequest getCreateOceanBaseDataSourceRequest() {
        CreateOceanBaseDataSourceRequest request = new CreateOceanBaseDataSourceRequest();
        request.setName("IT-" + StringUtils.uuidNoHyphen());
        request.setType(OmsOceanBaseType.OB_MYSQL.name());
        request.setTenant(config.getTenant());
        request.setCluster(config.getCluster());
        request.setVpcId(null);
        request.setIp(config.getHost());
        request.setPort(config.getPort());
        request.setUserName(config.getUsername());
        request.setPassword(Base64.getEncoder().encodeToString(config.getSysPassword().getBytes()));

        request.setRegion("cn-anhui");
        request.setDescription(null);
        request.setOcpName(null);

        String configUrl = getConfigUrl();
        request.setConfigUrl(configUrl);
        request.setDrcUserName(config.getSysUsername());
        request.setDrcPassword(Base64.getEncoder().encodeToString(config.getSysPassword().getBytes()));
        return request;
    }

    private String getConfigUrl() {
        String queryClusterUrlSql = "show parameters like 'obconfig_url'";
        String configUrl;
        try (Connection connection = DriverManager.getConnection(
                TestDBUtil.buildUrl(config.getHost(), config.getPort(), config.getDefaultDBName(), "OB_MYSQL"),
                TestDBUtil.buildUser(config.getSysUsername(), config.getTenant(), config.getCluster()),
                config.getSysPassword())) {
            configUrl = new JdbcTemplate(new SingleConnectionDataSource(connection, false))
                    .query(queryClusterUrlSql, rs -> {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Get ob config_url is empty");
                        }
                        return rs.getString("value");
                    });

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return configUrl;
    }

    private ProjectControlRequest getProjectControlRequest(String projectId) {
        ProjectControlRequest request = new ProjectControlRequest();
        request.setId(projectId);
        return request;
    }

}
