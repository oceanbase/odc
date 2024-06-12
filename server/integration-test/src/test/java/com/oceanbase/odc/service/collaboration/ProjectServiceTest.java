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
package com.oceanbase.odc.service.collaboration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectMapper;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.collaboration.project.model.Project.ProjectMember;
import com.oceanbase.odc.service.collaboration.project.model.QueryProjectParams;
import com.oceanbase.odc.service.collaboration.project.model.SetArchivedReq;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.UserOrganizationService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.model.UserResourceRole;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * @Author: Lebie
 * @Date: 2023/5/5 16:16
 * @Description: []
 */
public class ProjectServiceTest extends ServiceTestEnv {
    private final ProjectMapper mapper = ProjectMapper.INSTANCE;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private ResourceRoleService resourceRoleService;

    @MockBean
    private UserOrganizationService userOrganizationService;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private ResourceRoleRepository resourceRoleRepository;

    @Before
    public void setUp() {
        Mockito.when(userService.nullSafeGet(Mockito.anyLong())).thenReturn(getUserEntity());
        Mockito.when(userOrganizationService.userBelongsToOrganization(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        Mockito.when(authenticationFacade.currentUser()).thenReturn(getUser());
        Mockito.when(resourceRoleRepository.findAll()).thenReturn(listAllProjectResourceRoles());
        databaseRepository.deleteAll();
        projectRepository.deleteAll();

    }

    @After
    public void tearDown() {
        databaseRepository.deleteAll();
        projectRepository.deleteAll();
    }


    @Test
    public void testCreateProject_Success() {
        projectService.create(getProject());
        int actual = projectRepository.findAll().size();
        Assert.assertEquals(1, actual);
    }

    @Test
    public void testGetProject_Success() {
        Project saved = projectService.create(getProject());
        Mockito.when(
                resourceRoleService.listByResourceTypeAndId(Mockito.eq(ResourceType.ODC_PROJECT), Mockito.anyLong()))
                .thenReturn(listUserResourceRole(saved.getId()));
        Project actual = projectService.detail(saved.getId());
        Assert.assertNotNull(actual);
    }

    @Test
    public void testGetProject_syncTimeIsNull() {
        Date syncTime = new Date();
        Project saved = projectService.create(getProject());
        Mockito.when(
                resourceRoleService.listByResourceTypeAndId(Mockito.eq(ResourceType.ODC_PROJECT), Mockito.anyLong()))
                .thenReturn(listUserResourceRole(saved.getId()));
        createDatabase(saved.getId(), null);
        createDatabase(saved.getId(), syncTime);
        Project actual = projectService.detail(saved.getId());
        Assert.assertNull(actual.getDbObjectLastSyncTime());
    }

    @Test
    public void testGetProject_syncTimeNotNull() {
        Date syncTime = new Date();
        Project saved = projectService.create(getProject());
        Mockito.when(
                resourceRoleService.listByResourceTypeAndId(Mockito.eq(ResourceType.ODC_PROJECT), Mockito.anyLong()))
                .thenReturn(listUserResourceRole(saved.getId()));
        createDatabase(saved.getId(), syncTime);
        createDatabase(saved.getId(), DateUtils.addDays(syncTime, 1));
        Project actual = projectService.detail(saved.getId());
        Assert.assertEquals(syncTime, actual.getDbObjectLastSyncTime());
    }

    @Test
    public void testUpdateProject_Success() {
        Mockito.when(resourceRoleService.saveAll(Mockito.any())).thenReturn(listUserResourceRole(1L));
        Project saved = projectService.create(getProject());
        saved.setName("another_name");
        Project updated = projectService.update(saved.getId(), saved);
        Assert.assertEquals("another_name", updated.getName());
    }

    @Test
    public void testArchiveProject_Archived() throws InterruptedException {
        Project saved = projectService.create(getProject());
        Mockito.when(resourceRoleService.saveAll(Mockito.any())).thenReturn(listUserResourceRole(saved.getId()));
        SetArchivedReq req = new SetArchivedReq();
        req.setArchived(true);
        Project archived = projectService.setArchived(saved.getId(), req);
        Assert.assertTrue(archived.getArchived());
    }

    @Test(expected = BadRequestException.class)
    public void testArchiveProject_NotArchived() throws InterruptedException {
        Project saved = projectService.create(getProject());
        Mockito.when(resourceRoleService.saveAll(Mockito.any())).thenReturn(listUserResourceRole(saved.getId()));
        SetArchivedReq req = new SetArchivedReq();
        req.setArchived(false);
        projectService.setArchived(saved.getId(), req);
    }

    @Test
    public void test_listBasicInfoForApply() {
        ProjectEntity entity = projectRepository.save(getProjectEntity());
        List<Project> projects = projectService.listBasicInfoForApply(null, null);
        Assert.assertEquals(1, projects.size());
        Assert.assertEquals(entity.getId(), projects.get(0).getId());
    }

    @Test
    public void testListProjects_HaveJoinedProject_Success() {
        ProjectEntity entity = getProjectEntity();
        ProjectEntity saved = projectRepository.save(entity);
        Mockito.when(resourceRoleService.listByOrganizationIdAndUserId(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(listUserResourceRole(saved.getId()));

        QueryProjectParams params = QueryProjectParams.builder().name("whatever_").build();
        Page<Project> projects = projectService.list(params, Pageable.unpaged());
        Assert.assertEquals(1, projects.getTotalElements());
    }

    @Test
    public void testListProjects_NoJoinedProject_Success() {
        ProjectEntity entity = getProjectEntity();
        projectRepository.save(entity);

        QueryProjectParams params = QueryProjectParams.builder().name("whatever_").build();
        Mockito.when(resourceRoleService.listByOrganizationIdAndUserId(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(ListUtils.EMPTY_LIST);
        Page<Project> projects = projectService.list(params, Pageable.unpaged());
        Assert.assertEquals(0, projects.getTotalElements());
    }

    @Test
    public void testListProjects_ListArchivedProjects_Success() {
        ProjectEntity entity = getProjectEntity();
        entity.setArchived(true);
        ProjectEntity saved = projectRepository.save(entity);
        Mockito.when(resourceRoleService.saveAll(Mockito.any())).thenReturn(listUserResourceRole(saved.getId()));

        QueryProjectParams params = QueryProjectParams.builder().name("whatever_").archived(Boolean.TRUE).build();
        Mockito.when(resourceRoleService.listByOrganizationIdAndUserId(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(listUserResourceRole(saved.getId()));
        Page<Project> projects = projectService.list(params, Pageable.unpaged());
        Assert.assertEquals(1, projects.getTotalElements());
    }

    @Test
    public void testListProjects_ListNotArchivedProject_Success() {
        ProjectEntity entity = getProjectEntity();
        entity.setArchived(true);
        ProjectEntity saved = projectRepository.save(entity);;

        QueryProjectParams params = QueryProjectParams.builder().name("whatever_").archived(Boolean.FALSE).build();
        Mockito.when(resourceRoleService.listByOrganizationIdAndUserId(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(listUserResourceRole(saved.getId()));
        Page<Project> projects = projectService.list(params, Pageable.unpaged());
        Assert.assertEquals(0, projects.getTotalElements());
    }


    private Project getProject() {
        Project project = new Project();
        project.setName("whatever_name");
        project.setDescription("whatever_description");
        project.setMembers(listProjectMembers());
        return project;
    }

    private ProjectEntity getProjectEntity() {
        ProjectEntity entity = mapper.modelToEntity(getProject());
        entity.setId(1L);
        entity.setBuiltin(false);
        entity.setArchived(false);
        entity.setOrganizationId(1L);
        entity.setLastModifierId(1L);
        entity.setCreatorId(1L);
        entity.setUniqueIdentifier("ODC_" + UUID.randomUUID());
        return entity;
    }

    private List<ProjectMember> listProjectMembers() {
        List<ProjectMember> members = new ArrayList<>();

        ProjectMember owner = new ProjectMember();
        owner.setId(1L);
        owner.setRole(ResourceRoleName.OWNER);
        members.add(owner);

        ProjectMember dba = new ProjectMember();
        dba.setId(1L);
        dba.setRole(ResourceRoleName.DBA);
        members.add(dba);

        ProjectMember developer = new ProjectMember();
        developer.setId(1L);
        developer.setRole(ResourceRoleName.DEVELOPER);
        members.add(developer);

        return members;
    }

    private UserEntity getUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setAccountName("whatever_account_name");
        user.setName("whatever_name");
        return user;
    }

    private User getUser() {
        return new User(getUserEntity());
    }

    private List<ResourceRoleEntity> listAllProjectResourceRoles() {
        List<ResourceRoleEntity> roles = new ArrayList<>();
        ResourceRoleEntity owner = new ResourceRoleEntity();
        owner.setId(1L);
        owner.setRoleName(ResourceRoleName.OWNER);
        owner.setResourceType(ResourceType.ODC_PROJECT);
        roles.add(owner);

        ResourceRoleEntity dba = new ResourceRoleEntity();
        owner.setId(2L);
        owner.setRoleName(ResourceRoleName.DBA);
        owner.setResourceType(ResourceType.ODC_PROJECT);
        roles.add(dba);

        ResourceRoleEntity developer = new ResourceRoleEntity();
        owner.setId(3L);
        owner.setRoleName(ResourceRoleName.DEVELOPER);
        owner.setResourceType(ResourceType.ODC_PROJECT);
        roles.add(developer);

        return roles;
    }

    private List<UserResourceRole> listUserResourceRole(Long resourceId) {
        List<UserResourceRole> userResourceRoles = new ArrayList<>();

        UserResourceRole owner = new UserResourceRole();
        owner.setUserId(1L);
        owner.setResourceId(resourceId);
        owner.setResourceRole(ResourceRoleName.OWNER);
        owner.setResourceType(ResourceType.ODC_PROJECT);
        userResourceRoles.add(owner);

        UserResourceRole dba = new UserResourceRole();
        dba.setUserId(1L);
        dba.setResourceId(1L);
        dba.setResourceRole(ResourceRoleName.DBA);
        dba.setResourceType(ResourceType.ODC_PROJECT);
        userResourceRoles.add(dba);

        UserResourceRole developer = new UserResourceRole();
        developer.setUserId(1L);
        developer.setResourceId(resourceId);
        developer.setResourceRole(ResourceRoleName.DEVELOPER);
        developer.setResourceType(ResourceType.ODC_PROJECT);
        userResourceRoles.add(developer);

        return userResourceRoles;
    }

    private void createDatabase(Long projectId, Date lastSyncTime) {
        DatabaseEntity entity = TestRandom.nextObject(DatabaseEntity.class);
        entity.setProjectId(projectId);
        entity.setDatabaseId(UUID.randomUUID().toString());
        entity.setExisted(true);
        entity.setObjectLastSyncTime(lastSyncTime);
        databaseRepository.save(entity);
    }

}
