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
package com.oceanbase.odc.service.onlineschemachange;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.service.onlineschemachange.oms.client.ClientRequestParams;
import com.oceanbase.odc.service.onlineschemachange.oms.client.OmsClient;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsOceanBaseType;
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
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.SpecificTransferMapping;
import com.oceanbase.odc.service.onlineschemachange.oms.request.TableTransferObject;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.database.TestDBUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-02
 * @since 4.2.0
 */
@Slf4j
public class OmsOpenApiServiceTest {

    private static DataSourceOpenApiService dataSourceOpenApiService;
    private static ProjectOpenApiService projectOpenApiService;

    private static TestDBConfiguration config =
            TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();

    private static OmsClient omsClient;
    private static String datasourceId;
    private static String projectId;

    @BeforeClass
    public static void beforeClass() {

        omsClient = Mockito.mock(OmsClient.class);
        dataSourceOpenApiService = new DataSourceOpenApiServiceImpl(omsClient);
        projectOpenApiService = new ProjectOpenApiServiceImpl(omsClient);
        datasourceId = UUID.randomUUID().toString();
        projectId = UUID.randomUUID().toString();
    }

    @Test
    public void test_oms_openapi() {

        test_create_datasource();
        test_project_create_and_start();
        test_listProjectFullVerifyResult();
        test_project_control();
    }

    private void test_create_datasource() {

        CreateOceanBaseDataSourceRequest request = getCreateOceanBaseDataSourceRequest();
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("CreateOceanBaseDataSource")
                .setTypeReference(new TypeReference<OmsApiReturnResult<String>>() {});

        Mockito.when(omsClient.postOmsInterface(Mockito.any(ClientRequestParams.class))).thenReturn(datasourceId);
        Object o = omsClient.postOmsInterface(params);
        Assert.assertEquals(o, datasourceId);
        String datasourceIdReturn = dataSourceOpenApiService.createOceanBaseDataSource(request);
        Assert.assertEquals(datasourceId, datasourceIdReturn);
    }

    private void test_project_create_and_start() {

        CreateProjectRequest request = getCreateProjectRequest();
        Mockito.when(omsClient.postOmsInterface(Mockito.any(ClientRequestParams.class))).thenReturn(projectId);

        String projectIdResult = projectOpenApiService.createProject(request);
        Assert.assertEquals(projectIdResult, projectId);

        ProjectProgressResponse projectProgressResponse = new ProjectProgressResponse();
        projectProgressResponse.setStatus(ProjectStatusEnum.INIT);
        Mockito.when(omsClient.postOmsInterface(Mockito.any(ClientRequestParams.class))).thenReturn(
                projectProgressResponse);

        ProjectProgressResponse projectProgressResponseReturn = projectOpenApiService.describeProjectProgress(
                Mockito.any(ProjectControlRequest.class));
        Assert.assertEquals(ProjectStatusEnum.INIT, projectProgressResponseReturn.getStatus());

        Mockito.when(omsClient.postOmsInterface(Mockito.any(ClientRequestParams.class))).thenReturn(Void.class);
        projectOpenApiService.startProject(Mockito.any(ProjectControlRequest.class));

    }

    private void test_listProjectFullVerifyResult() {
        ListProjectFullVerifyResultRequest request = new ListProjectFullVerifyResultRequest();
        request.setProjectId(projectId);
        request.setSourceSchemas(new String[] {config.getDefaultDBName()});
        request.setDestSchemas(new String[] {config.getDefaultDBName()});
        request.setStatus(new String[] {"FINISHED", "SUSPEND", "RUNNING"});
        request.setPageSize(10);
        request.setPageNumber(1);

        ProjectFullVerifyResultResponse response = Mockito.mock(ProjectFullVerifyResultResponse.class);

        Mockito.when(omsClient.postOmsInterface(Mockito.any(ClientRequestParams.class))).thenReturn(response);

        ProjectFullVerifyResultResponse verifyResultResponse =
                projectOpenApiService.listProjectFullVerifyResult(request);

        Assert.assertEquals(verifyResultResponse, response);

    }

    private void test_project_control() {

        Mockito.when(omsClient.postOmsInterface(Mockito.any(ClientRequestParams.class))).thenReturn(Void.class);

        ProjectControlRequest request = new ProjectControlRequest();
        request.setUid("1");
        request.setId(projectId);

        projectOpenApiService.stopProject(request);

        projectOpenApiService.resumeProject(request);

        projectOpenApiService.releaseProject(request);

        projectOpenApiService.deleteProject(request);
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
        return request;
    }

    private CreateOceanBaseDataSourceRequest getCreateOceanBaseDataSourceRequest() {
        CreateOceanBaseDataSourceRequest request = new CreateOceanBaseDataSourceRequest();
        request.setName(UUID.randomUUID().toString());
        request.setType(OmsOceanBaseType.OB_MYSQL.name());
        request.setTenant(config.getTenant());
        request.setCluster(config.getCluster());
        request.setSchema(config.getDefaultDBName());
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

}
