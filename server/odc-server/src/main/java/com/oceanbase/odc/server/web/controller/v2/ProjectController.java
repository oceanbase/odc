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
package com.oceanbase.odc.server.web.controller.v2;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.collaboration.project.model.Project.ProjectMember;
import com.oceanbase.odc.service.collaboration.project.model.QueryProjectParams;
import com.oceanbase.odc.service.collaboration.project.model.SetArchivedReq;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/4/11 16:05
 * @Description: []
 */

@RestController
@RequestMapping("/api/v2/collaboration")
public class ProjectController {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private DatabaseService databaseService;

    @ApiOperation(value = "createProject", notes = "Create a project")
    @RequestMapping(value = "/projects", method = RequestMethod.POST)
    public SuccessResponse<Project> createProject(@RequestBody Project project) {
        return Responses.success(projectService.create(project));
    }

    @ApiOperation(value = "getProject", notes = "Detail a project")
    @RequestMapping(value = "/projects/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<Project> getProject(@PathVariable Long id) {
        return Responses.success(projectService.detail(id));
    }

    @ApiOperation(value = "listProjects", notes = "List all projects")
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public PaginatedResponse<Project> listProjects(
            @RequestParam(required = false, name = "name") String name,
            @RequestParam(required = false, name = "archived") Boolean archived,
            @RequestParam(required = false, name = "builtin", defaultValue = "false") Boolean builtin,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        QueryProjectParams params =
                QueryProjectParams.builder().name(name).archived(archived).builtin(builtin).build();
        return Responses.paginated(projectService.list(params, pageable));
    }

    @ApiOperation(value = "listBasicProjects", notes = "List all basic projects")
    @RequestMapping(value = "/projects/basic", method = RequestMethod.GET)
    public ListResponse<Project> listBasicProjects(
            @RequestParam(required = false, name = "archived") Boolean archived,
            @RequestParam(required = false, name = "builtin", defaultValue = "false") Boolean builtin) {
        return Responses.list(projectService.listBasicInfoForApply(archived, builtin));
    }

    @ApiOperation(value = "updateProject", notes = "Update a project")
    @RequestMapping(value = "/projects/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<Project> updateProject(@PathVariable Long id,
            @RequestBody Project project) {
        return Responses.success(projectService.update(id, project));
    }

    @ApiOperation(value = "stats projects", notes = "Stats all projects")
    @RequestMapping(value = "/projects/databases/stats", method = RequestMethod.GET)
    public ListResponse<ConnectionConfig> statsRelatedDataSource() {
        return Responses.list(databaseService.statsConnectionConfig());
    }

    @ApiOperation(value = "createProjectMembers", notes = "Create project members")
    @RequestMapping(value = "/projects/{id:[\\d]+}/members", method = RequestMethod.POST)
    public SuccessResponse<Project> createProjectMembers(@PathVariable Long id,
            @RequestBody List<ProjectMember> members) {
        return Responses.success(projectService.createProjectMembers(id, members));
    }

    @ApiOperation(value = "deleteProjectMember", notes = "Delete a project member")
    @RequestMapping(value = "/projects/{projectId:[\\d]+}/members/{userId}", method = RequestMethod.DELETE)
    public SuccessResponse<Boolean> deleteProjectMember(@PathVariable Long projectId, @PathVariable Long userId) {
        return Responses.success(projectService.deleteProjectMember(projectId, userId));
    }

    @ApiOperation(value = "updateProjectMember", notes = "Update a project member")
    @RequestMapping(value = "/projects/{projectId:[\\d]+}/members/{userId}", method = RequestMethod.PUT)
    public SuccessResponse<Boolean> updateProjectMember(@PathVariable Long projectId, @PathVariable Long userId,
            @RequestBody List<ProjectMember> members) {
        return Responses.success(projectService.updateProjectMember(projectId, userId, members));
    }

    @ApiOperation(value = "archiveProject", notes = "Archive a project")
    @RequestMapping(value = "/projects/{id:[\\d]+}/setArchived", method = RequestMethod.POST)
    public SuccessResponse<Project> setArchived(@PathVariable Long id,
            @RequestBody @Valid SetArchivedReq setArchivedReq) throws InterruptedException {
        return Responses.success(projectService.setArchived(id, setArchivedReq));
    }

}
