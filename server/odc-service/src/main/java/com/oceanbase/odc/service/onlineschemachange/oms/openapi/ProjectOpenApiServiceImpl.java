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
package com.oceanbase.odc.service.onlineschemachange.oms.openapi;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.service.onlineschemachange.oms.client.ClientRequestParams;
import com.oceanbase.odc.service.onlineschemachange.oms.client.OmsClient;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectStepVO;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectVO;

/**
 * @author yaobin
 * @date 2023-06-03
 * @since 4.2.0
 */
@Service
public class ProjectOpenApiServiceImpl implements ProjectOpenApiService {
    private final OmsClient omsClient;

    public ProjectOpenApiServiceImpl(@Autowired OmsClient omsClient) {
        this.omsClient = omsClient;
    }

    @Override
    public List<ProjectVO> listProjects(ListProjectRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ListProjects")
                .setTypeReference(new TypeReference<OmsApiReturnResult<List<ProjectVO>>>() {});

        return omsClient.postOmsInterface(params);
    }

    @Override
    public String createProject(CreateProjectRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("CreateProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<String>>() {});

        return omsClient.postOmsInterface(params);
    }

    @Override
    public void startProject(ProjectControlRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("StartProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});
        omsClient.postOmsInterface(params);
    }

    @Override
    public void stopProject(ProjectControlRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("StopProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});

        omsClient.postOmsInterface(params);
    }

    @Override
    public void resumeProject(ProjectControlRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ResumeProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});

        omsClient.postOmsInterface(params);
    }

    @Override
    public void releaseProject(ProjectControlRequest request) {

        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ReleaseProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});

        omsClient.postOmsInterface(params);
    }

    @Override
    public void deleteProject(ProjectControlRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("DeleteProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});

        omsClient.postOmsInterface(params);
    }

    @Override
    public ProjectProgressResponse describeProjectProgress(ProjectControlRequest request) {

        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("DescribeProjectProgress")
                .setTypeReference(new TypeReference<OmsApiReturnResult<ProjectProgressResponse>>() {});

        return omsClient.postOmsInterface(params);
    }

    @Override
    public List<ProjectStepVO> describeProjectSteps(ProjectControlRequest request) {

        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("DescribeProjectSteps")
                .setTypeReference(new TypeReference<OmsApiReturnResult<List<ProjectStepVO>>>() {});

        return omsClient.postOmsInterface(params);

    }

    @Override
    public ProjectFullVerifyResultResponse listProjectFullVerifyResult(ListProjectFullVerifyResultRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ListProjectFullVerifyResult")
                .setTypeReference(new TypeReference<OmsApiReturnResult<ProjectFullVerifyResultResponse>>() {});

        return omsClient.postOmsInterface(params);
    }

}
