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

import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOmsProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListOmsProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListOmsProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
public interface OmsProjectOpenApiService {

    /**
     * 查询项目
     *
     * @param request
     * @return
     */
    List<OmsProjectResponse> listProjects(ListOmsProjectRequest request);

    /**
     * 创建项目
     *
     * @param request
     * @return
     */
    String createProject(CreateOmsProjectRequest request);

    /**
     * 启动项目
     */
    void startProject(OmsProjectControlRequest request);

    /**
     * 暂停项目
     */
    void stopProject(OmsProjectControlRequest request);

    /**
     * 恢复项目
     */
    void resumeProject(OmsProjectControlRequest request);

    /**
     * 释放项目
     */
    void releaseProject(OmsProjectControlRequest request);

    /**
     * 删除项目
     */
    void deleteProject(OmsProjectControlRequest request);


    /**
     * 查询项目状态和进度
     */
    OmsProjectProgressResponse describeProjectProgress(OmsProjectControlRequest request);

    /**
     * 查询项目步骤详情
     *
     */
    List<OmsProjectStepVO> describeProjectSteps(OmsProjectControlRequest request);

    /**
     * 查询项目的全量校验结果
     * 
     * @param request
     * @return
     */
    OmsProjectFullVerifyResultResponse listProjectFullVerifyResult(ListOmsProjectFullVerifyResultRequest request);

}
