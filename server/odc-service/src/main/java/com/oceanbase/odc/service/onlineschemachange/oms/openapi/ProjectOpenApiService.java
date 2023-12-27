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

import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectStepVO;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectVO;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
public interface ProjectOpenApiService {

    /**
     * 查询项目
     *
     * @param request
     * @return
     */
    List<ProjectVO> listProjects(ListProjectRequest request);
    /**
     * 创建项目
     *
     * @param request
     * @return
     */
    String createProject(CreateProjectRequest request);

    /**
     * 启动项目
     */
    void startProject(ProjectControlRequest request);

    /**
     * 暂停项目
     */
    void stopProject(ProjectControlRequest request);

    /**
     * 恢复项目
     */
    void resumeProject(ProjectControlRequest request);

    /**
     * 释放项目
     */
    void releaseProject(ProjectControlRequest request);

    /**
     * 删除项目
     */
    void deleteProject(ProjectControlRequest request);


    /**
     * 查询项目状态和进度
     */
    ProjectProgressResponse describeProjectProgress(ProjectControlRequest request);

    /**
     * 查询项目步骤详情
     *
     */
    List<ProjectStepVO> describeProjectSteps(ProjectControlRequest request);

    /**
     * 查询项目的全量校验结果
     * 
     * @param request
     * @return
     */
    ProjectFullVerifyResultResponse listProjectFullVerifyResult(ListProjectFullVerifyResultRequest request);

}
