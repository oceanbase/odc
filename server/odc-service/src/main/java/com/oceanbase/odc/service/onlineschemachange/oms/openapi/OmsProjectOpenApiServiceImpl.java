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
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOmsProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListOmsProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListOmsProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;

/**
 * @author yaobin
 * @date 2023-06-03
 * @since 4.2.0
 */
@Service
public class OmsProjectOpenApiServiceImpl implements OmsProjectOpenApiService {
    private final OmsClient omsClient;

    public OmsProjectOpenApiServiceImpl(@Autowired OmsClient omsClient) {
        this.omsClient = omsClient;
    }

    @Override
    public List<OmsProjectResponse> listProjects(ListOmsProjectRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ListProjects")
                .setTypeReference(new TypeReference<OmsApiReturnResult<List<OmsProjectResponse>>>() {});

        return omsClient.postOmsInterface(params);
    }

    @Override
    public String createProject(CreateOmsProjectRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("CreateProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<String>>() {});

        return omsClient.postOmsInterface(params);
    }

    @Override
    public void startProject(OmsProjectControlRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("StartProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});
        omsClient.postOmsInterface(params);
    }

    @Override
    public void stopProject(OmsProjectControlRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("StopProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});

        omsClient.postOmsInterface(params);
    }

    @Override
    public void resumeProject(OmsProjectControlRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ResumeProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});

        omsClient.postOmsInterface(params);
    }

    @Override
    public void releaseProject(OmsProjectControlRequest request) {

        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ReleaseProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});

        omsClient.postOmsInterface(params);
    }

    @Override
    public void deleteProject(OmsProjectControlRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("DeleteProject")
                .setTypeReference(new TypeReference<OmsApiReturnResult<Void>>() {});

        omsClient.postOmsInterface(params);
    }

    @Override
    public OmsProjectProgressResponse describeProjectProgress(OmsProjectControlRequest request) {

        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("DescribeProjectProgress")
                .setTypeReference(new TypeReference<OmsApiReturnResult<OmsProjectProgressResponse>>() {});

        return omsClient.postOmsInterface(params);
    }

    @Override
    public List<OmsProjectStepVO> describeProjectSteps(OmsProjectControlRequest request) {

        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("DescribeProjectSteps")
                .setTypeReference(new TypeReference<OmsApiReturnResult<List<OmsProjectStepVO>>>() {});

        return omsClient.postOmsInterface(params);

    }

    @Override
    public OmsProjectFullVerifyResultResponse listProjectFullVerifyResult(
            ListOmsProjectFullVerifyResultRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ListProjectFullVerifyResult")
                .setTypeReference(new TypeReference<OmsApiReturnResult<OmsProjectFullVerifyResultResponse>>() {});

        return omsClient.postOmsInterface(params);
    }

}
