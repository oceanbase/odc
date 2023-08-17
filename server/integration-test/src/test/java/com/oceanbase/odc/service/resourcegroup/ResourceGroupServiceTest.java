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
package com.oceanbase.odc.service.resourcegroup;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionRepository;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupRepository;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.resourcegroup.model.ModifyResourceGroupReq;
import com.oceanbase.odc.service.resourcegroup.model.ModifyResourceGroupReq.ConnectionMetaInfo;
import com.oceanbase.odc.service.resourcegroup.model.QueryResourceGroupReq;
import com.oceanbase.odc.service.resourcegroup.model.ResourceIdentifier;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * Test object for <code>ResourceGroupService</code>
 *
 * @author yh263208
 * @date 2021-07-28 14:49
 * @since ODC-release_3.2.0
 */
@Ignore
public class ResourceGroupServiceTest extends MockedAuthorityTestEnv {

    @Autowired
    private ResourceGroupService service;
    @Autowired
    private ResourceGroupRepository resourceGroupRepository;
    @Autowired
    private ResourceGroupConnectionRepository resourceGroupConnectionRepository;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    public ConnectionConfigRepository connectionRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;


    @After
    public void clearAll() {
        resourceGroupRepository.deleteAll();
        connectionRepository.deleteAll();
        resourceGroupConnectionRepository.deleteAll();
        DefaultLoginSecurityManager.removeContext();
        DefaultLoginSecurityManager.removeSecurityContext();
    }

    @Before
    public void setUp() {
        when(authenticationFacade.currentUserId()).thenReturn(1L);
        when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        when(authenticationFacade.currentUser()).thenReturn(User.of(1L));
        resourceGroupRepository.deleteAll();
        connectionRepository.deleteAll();
        resourceGroupConnectionRepository.deleteAll();
        // Grant all permissions
        grantAllPermissions(ResourceType.ODC_RESOURCE_GROUP, ResourceType.ODC_CONNECTION);
    }

    @Test
    public void testInsertResourceGroup() {
        insertBatchResourceGroup(15);
        Page<ResourceGroup> result = service.list(getQueryParam(null, null), PageRequest.of(0, 100));
        Assert.assertEquals(15, result.getTotalElements());
    }

    @Test
    public void testInertAnExistedResourceGroup() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(13);
        ResourceGroup resourceGroup = resourceGroups.get(0);
        Assert.assertTrue(resourceGroup.exists());
        thrown.expectMessage("ResourceGroup: " + resourceGroup.getId() + " already exists");
        thrown.expect(RuntimeException.class);
        resourceGroup.create();
    }

    @Test
    public void testDeleteResurceGroup() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(15);
        ResourceGroup resourceGroup = resourceGroups.get(0);
        resourceGroup.delete();
        Page<ResourceGroup> resourceGroupPage = service.list(getQueryParam(null, null), PageRequest.of(0, 1000));
        Assert.assertEquals(14, resourceGroupPage.getTotalElements());
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteResurceGroupDoesNotExist() {
        ResourceGroupEntity entity = getEntity("aaa");
        entity.setId(12345L);
        ResourceGroup resourceGroup =
                new ResourceGroup(entity, resourceGroupRepository, resourceGroupConnectionRepository,
                        authenticationFacade);
        resourceGroup.delete();
    }

    @Test
    public void testUpdateResurceGroup() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(15);
        ResourceGroup resourceGroup = resourceGroups.get(0);
        resourceGroup.setEnabled(!resourceGroup.isEnabled());
        resourceGroup.setName("hahaha");
        resourceGroup.update();

        // Test total number
        Page<ResourceGroup> resourceGroupPage = service.list(getQueryParam(null, null), PageRequest.of(0, 1000));
        Assert.assertEquals(15, resourceGroupPage.getTotalElements());

        // Test modify field
        ResourceGroup resourceGroup1 = service.get(resourceGroup.getId());
        resourceGroup1.setCreatorName(null);
        Assert.assertEquals(resourceGroup, resourceGroup1);
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateResurceGroupDoesNotExist() {
        ResourceGroupEntity entity = getEntity("aaa");
        entity.setId(12345L);
        ResourceGroup resourceGroup =
                new ResourceGroup(entity, resourceGroupRepository, resourceGroupConnectionRepository,
                        authenticationFacade);
        resourceGroup.update();
    }

    @Test
    public void testGetResourceGroup() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(25);
        ResourceGroup resourceGroup = resourceGroups.get(0);
        ResourceGroup resourceGroup1 = service.get(resourceGroup.getId());
        Assert.assertEquals(resourceGroup.getId(), resourceGroup1.getId());
    }

    @Test
    public void testListTrueResourceGroup() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(100);
        Page<ResourceGroup> resourceGroupPage = service.list(getQueryParam(null, true), PageRequest.of(0, 100));
        List<ResourceGroup> filterdResourceGroup1 =
                resourceGroups.stream().filter(ResourceGroup::isEnabled).collect(Collectors.toList());
        Assert.assertEquals(filterdResourceGroup1.size(), resourceGroupPage.getTotalElements());
    }

    @Test
    public void testListnameLikeResourceGroup() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(100);
        String nameLike = "5";
        long totalCount = 0;
        for (ResourceGroup resourceGroup : resourceGroups) {
            if (resourceGroup.getId().toString().contains(nameLike) || resourceGroup.getName().contains(nameLike)) {
                totalCount++;
            }
        }
        Page<ResourceGroup> resourceGroupPage = service.list(getQueryParam("5", null), PageRequest.of(0, 100));
        Assert.assertEquals(totalCount, resourceGroupPage.getTotalElements());
    }

    @Test
    public void testExists() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(1);
        ResourceGroup resourceGroup = resourceGroups.get(0);
        Assert.assertTrue(service.exists(resourceGroup.getName()));
    }

    @Test
    public void testResourceGroupConstructorDoesNotExist() {
        thrown.expectMessage("ResourceGroup: 123 does not exist");
        thrown.expect(NotFoundException.class);
        new ResourceGroup(123, resourceGroupRepository, resourceGroupConnectionRepository, authenticationFacade);
    }

    @Test
    public void testResourceGroupConstructor() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(1);
        ResourceGroup resourceGroup = resourceGroups.get(0);
        ResourceGroup resourceGroup1 =
                new ResourceGroup(resourceGroup.getId(), resourceGroupRepository, resourceGroupConnectionRepository,
                        authenticationFacade);
        Assert.assertEquals(resourceGroup, resourceGroup1);
    }

    @Test
    public void testResourceGroupConstructor1() {
        ResourceGroup resourceGroup1 = new ResourceGroup("name", "desp", true,
                resourceGroupRepository, resourceGroupConnectionRepository, authenticationFacade);
        resourceGroup1.create();
        ResourceGroup resourceGroup = service.get(resourceGroup1.getId());
        resourceGroup.setCreatorName(null);
        Assert.assertEquals(resourceGroup, resourceGroup1);
    }

    @Test
    public void testBindResource() {
        // Prepare data
        insertBatchResourceGroup(2);
        List<ResourceGroup> resourceGroups =
                service.list(getQueryParam(null, null), PageRequest.of(0, 100)).getContent();
        insertBatchConnection(5);
        List<ConnectionEntity> connections = connectionRepository.findAll();
        Assert.assertEquals(2, resourceGroups.size());
        Assert.assertEquals(5, connections.size());

        // Bind Connection to ResourceGroup
        for (ResourceGroup resourceGroup : resourceGroups) {
            for (ConnectionEntity connection : connections) {
                resourceGroup.bind(new ResourceIdentifier(connection.getId(), ResourceType.ODC_CONNECTION));
            }
        }

        // Verify
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll();
        Assert.assertEquals(10, entities.size());
    }

    @Test(expected = NotFoundException.class)
    public void testBindResourceWithNullResourceGroup() {
        ResourceGroupEntity entity = getEntity("name");
        entity.setId(123L);
        ResourceGroup resourceGroup =
                new ResourceGroup(entity, resourceGroupRepository, resourceGroupConnectionRepository,
                        authenticationFacade);
        resourceGroup.bind(new ResourceIdentifier(12L, ResourceType.ODC_CONNECTION));
    }

    @Test
    public void testBindAnExistResource() {
        // Prepare resource group
        ResourceGroupEntity entity = getEntity("name");
        entity.setId(123L);
        ResourceGroup resourceGroup =
                new ResourceGroup(entity, resourceGroupRepository, resourceGroupConnectionRepository,
                        authenticationFacade);
        resourceGroup.create();

        // Bind resource
        ResourceIdentifier identifier = new ResourceIdentifier(1234L, ResourceType.ODC_CONNECTION);
        resourceGroup.bind(identifier);
        resourceGroup.bind(new ResourceIdentifier(1234L, ResourceType.OB_CLUSTER));
        resourceGroup.bind(new ResourceIdentifier(12345L, ResourceType.ODC_CONNECTION));

        // Verify
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll();
        Assert.assertEquals(3, entities.size());
        thrown.expect(BadArgumentException.class);
        resourceGroup.bind(identifier);
    }

    @Test
    public void testBindResourceWithMultiType() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(1);
        ResourceGroup resourceGroup = resourceGroups.get(0);

        Set<ResourceIdentifier> identifierSet =
                new HashSet<>(Arrays.asList(new ResourceIdentifier(1234L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(12345L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(1234L, ResourceType.ODC_CONNECT_LABEL)));
        Set<ResourceIdentifier> identifiers = resourceGroup.bind(identifierSet);

        Assert.assertEquals(identifierSet.size(), identifiers.size());
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll();
        Assert.assertEquals(3, entities.size());
    }

    @Test
    public void testBindResourceWithMultiTypeAndExistResource() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(1);
        ResourceGroup resourceGroup = resourceGroups.get(0);

        ResourceIdentifier resource = new ResourceIdentifier(1234L, ResourceType.ODC_CONNECT_LABEL);
        resourceGroup.bind(resource);
        Set<ResourceIdentifier> identifierSet =
                new HashSet<>(Arrays.asList(new ResourceIdentifier(1234L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(12345L, ResourceType.ODC_CONNECTION), resource));
        Set<ResourceIdentifier> identifiers = resourceGroup.bind(identifierSet);

        Assert.assertEquals(identifierSet.size(), identifiers.size() + 1);
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll();
        Assert.assertEquals(3, entities.size());
    }

    @Test
    public void testUnbindResource() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(1);
        ResourceGroup resourceGroup = resourceGroups.get(0);

        ResourceIdentifier resource = new ResourceIdentifier(12345L, ResourceType.ODC_CONNECTION);
        ResourceIdentifier resource1 = new ResourceIdentifier(1234L, ResourceType.ODC_CONNECT_LABEL);
        Set<ResourceIdentifier> identifierSet = new HashSet<>(
                Arrays.asList(new ResourceIdentifier(1234L, ResourceType.ODC_CONNECTION), resource, resource1));
        resourceGroup.bind(identifierSet);

        resourceGroup.release(new HashSet<>(Arrays.asList(resource, resource1)));

        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll();
        Assert.assertEquals(1, entities.size());

        ResourceGroupConnectionEntity entity = entities.get(0);
        Assert.assertEquals(1234, (long) entity.getResourceId());
        Assert.assertEquals(ResourceType.ODC_CONNECTION.name(), entity.getResourceType());
    }

    @Test
    public void testDeleteResourcGroupWithBindingResource() {
        // Prepare data
        insertBatchResourceGroup(2);
        List<ResourceGroup> resourceGroups =
                service.list(getQueryParam(null, null), PageRequest.of(0, 100)).getContent();
        Set<ResourceIdentifier> identifierSet =
                new HashSet<>(Arrays.asList(new ResourceIdentifier(1234L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(12345L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(1234L, ResourceType.ODC_CONNECT_LABEL)));

        // Bind Connection to ResourceGroup
        for (ResourceGroup resourceGroup : resourceGroups) {
            Set<ResourceIdentifier> identifiers = resourceGroup.bind(identifierSet);
            Assert.assertEquals(identifierSet.size(), identifiers.size());
        }
        resourceGroups.get(0).release();
        resourceGroups.get(0).delete();

        // Verify
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll();
        Assert.assertEquals(3, entities.size());
        for (ResourceGroupConnectionEntity entity : entities) {
            Assert.assertEquals(resourceGroups.get(1).getId(), entity.getResourceGroupId());
        }
    }

    @Test
    public void testGetRelatedResources() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(1);
        ResourceGroup resourceGroup = resourceGroups.get(0);
        Set<ResourceIdentifier> identifierSet =
                new HashSet<>(Arrays.asList(new ResourceIdentifier(1234L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(12345L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(1234L, ResourceType.ODC_CONNECT_LABEL)));
        resourceGroup.bind(identifierSet);

        List<ResourceIdentifier> identifiers = resourceGroup.getRelatedResources(ResourceType.ODC_CONNECTION);
        Assert.assertEquals(2, identifiers.size());

        identifiers = resourceGroup.getRelatedResources(ResourceType.ODC_CONNECT_LABEL);
        Assert.assertEquals(1, identifiers.size());
    }

    @Test
    public void testGetRelatedConnections() {
        // Prepare data
        insertBatchResourceGroup(1);
        List<ResourceGroup> resourceGroups =
                service.list(getQueryParam(null, null), PageRequest.of(0, 100)).getContent();
        insertBatchConnection(5);
        List<ConnectionEntity> connections = connectionRepository.findAll();

        // Bind Connection to ResourceGroup
        ResourceGroup resourceGroup = resourceGroups.get(0);
        for (ConnectionEntity connection : connections) {
            resourceGroup.bind(new ResourceIdentifier(connection.getId(), ResourceType.ODC_CONNECTION));
        }

        // Verify
        List<ConnectionConfig> connections1 = resourceGroup.getRelatedConnections(connectionRepository);
        Assert.assertEquals(5, connections1.size());
    }

    @Test
    public void testGetRelatedResourceGroups() {
        // Prepare Data
        insertBatchResourceGroup(3);
        List<ResourceGroup> resourceGroups =
                service.list(getQueryParam(null, null), PageRequest.of(0, 100)).getContent();

        ResourceIdentifier identifier = new ResourceIdentifier(1234L, ResourceType.ODC_CONNECTION);
        ResourceIdentifier identifier1 = new ResourceIdentifier(12345L, ResourceType.ODC_CONNECTION);
        ResourceIdentifier identifier2 = new ResourceIdentifier(1234L, ResourceType.ODC_FILE);

        ResourceGroup resourceGroup = resourceGroups.get(0);
        ResourceGroup resourceGroup1 = resourceGroups.get(1);
        ResourceGroup resourceGroup2 = resourceGroups.get(2);

        resourceGroup.bind(identifier);
        resourceGroup.bind(identifier1);

        resourceGroup1.bind(identifier1);
        resourceGroup1.bind(identifier2);

        resourceGroup2.bind(identifier);
        resourceGroup2.bind(identifier1);
        resourceGroup2.bind(identifier2);

        // Get Result
        Map<ResourceIdentifier, List<ResourceGroup>> result =
                service.getRelatedResourceGroups(Arrays.asList(identifier, identifier1));

        // Verify
        List<ResourceGroup> resourceGroups1 = result.get(identifier1);
        Assert.assertEquals(3, resourceGroups1.size());
        resourceGroups1 = result.get(identifier);
        Assert.assertEquals(2, resourceGroups1.size());
    }

    @Test
    public void testGetRelatedResourcesMethod() {
        List<ResourceGroup> resourceGroups = insertBatchResourceGroup(3);
        ResourceGroup resourceGroup = resourceGroups.get(0);
        Set<ResourceIdentifier> identifierSet =
                new HashSet<>(Arrays.asList(new ResourceIdentifier(1234L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(12345L, ResourceType.ODC_CONNECTION),
                        new ResourceIdentifier(1234L, ResourceType.ODC_CONNECT_LABEL)));
        resourceGroup.bind(identifierSet);

        resourceGroup = resourceGroups.get(1);
        identifierSet = new HashSet<>(Arrays.asList(new ResourceIdentifier(12347L, ResourceType.ODC_CONNECTION),
                new ResourceIdentifier(12734L, ResourceType.ODC_CONNECT_LABEL)));
        resourceGroup.bind(identifierSet);

        resourceGroup = resourceGroups.get(2);
        identifierSet = new HashSet<>(Arrays.asList(new ResourceIdentifier(125347L, ResourceType.ODC_CONNECTION),
                new ResourceIdentifier(127234L, ResourceType.ODC_SYSTEM_CONFIG)));
        resourceGroup.bind(identifierSet);

        Map<ResourceGroup, List<ResourceIdentifier>> rg2Res = service.getRelatedResources(resourceGroups,
                identifier -> ResourceType.ODC_CONNECTION.equals(identifier.getResourceType()));
        Assert.assertEquals(3, rg2Res.size());
        for (List<ResourceIdentifier> list : rg2Res.values()) {
            for (ResourceIdentifier identifier : list) {
                Assert.assertEquals(ResourceType.ODC_CONNECTION, identifier.getResourceType());
            }
        }
    }

    @Test
    public void testInsertResourceGroupByService() {
        // Prepare Data
        ConnectionMetaInfo connectionMetaInfo = new ConnectionMetaInfo();
        connectionMetaInfo.setId(2L);
        ModifyResourceGroupReq request = getModifyRequest("David", true, "This is Desp",
                Collections.singletonList(connectionMetaInfo));
        ConnectionEntity connectionEntity = TestRandom.nextObject(ConnectionEntity.class);
        connectionEntity.setId(2L);
        connectionEntity.setOrganizationId(2L);
        connectionRepository.save(connectionEntity);
        try {
            service.create(request);
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof NotFoundException);
        }
        List<ResourceGroupEntity> resourceGroupEntities = resourceGroupRepository.findAll();
        Assert.assertEquals(0, resourceGroupEntities.size());
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll();
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void testInsertResourceGroupByService1() {
        // Prepare Data
        ConnectionConfig connection = TestRandom.nextObject(ConnectionConfig.class);
        connection.setId(null);
        ModifyResourceGroupReq request = getModifyRequest("David", true, "This is Desp",
                Collections.singletonList(new ConnectionMetaInfo(connection)));
        try {
            service.create(request);
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof BadRequestException);
        }
        List<ResourceGroupEntity> resourceGroupEntities = resourceGroupRepository.findAll();
        Assert.assertEquals(0, resourceGroupEntities.size());
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll();
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void testInsertResourceGroupByService2() {
        // Prepare Data
        List<ConnectionMetaInfo> connections =
                insertBatchConnection(5).stream().map(ConnectionMetaInfo::new).collect(Collectors.toList());
        ModifyResourceGroupReq request = getModifyRequest("David", true, "This is Desp", connections);
        ResourceGroup resourceGroup = service.create(request);

        List<ConnectionConfig> connections1 = resourceGroup.getRelatedConnections(connectionRepository);
        Assert.assertEquals(connections.size(), connections1.size());
    }

    @Test
    public void testUpdateResourceGroupByService() {
        // Prepare Data
        List<ConnectionMetaInfo> connections =
                insertBatchConnection(5).stream().map(ConnectionMetaInfo::new).collect(Collectors.toList());
        ModifyResourceGroupReq request = getModifyRequest("David", true, "This is Desp", connections);
        ResourceGroup resourceGroup = service.create(request);

        connections = insertBatchConnection(3).stream().map(ConnectionMetaInfo::new).collect(Collectors.toList());
        request = getModifyRequest("Davidsss", false, "This isss Desp", connections);
        resourceGroup = service.update(resourceGroup.getId(), request);

        List<ConnectionConfig> connections1 = resourceGroup.getRelatedConnections(connectionRepository);
        Assert.assertEquals(connections.size(), connections1.size());
    }

    private ModifyResourceGroupReq getModifyRequest(String name, boolean status, String desp,
            List<ConnectionMetaInfo> metaInfos) {
        ModifyResourceGroupReq request = new ModifyResourceGroupReq();
        request.setName(name);
        request.setEnabled(status);
        request.setDescription(desp);
        request.setConnections(metaInfos);
        return request;
    }

    private ResourceGroupEntity getEntity(String name) {
        ResourceGroupEntity entity = new ResourceGroupEntity();
        entity.setDescription("This is a Test object");
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        entity.setCreatorId(1L);
        entity.setName(name);
        entity.setEnabled(new Random().nextInt(10) > 5);
        return entity;
    }

    private List<ConnectionConfig> insertBatchConnection(int batchSize) {
        List<ConnectionConfig> returnVal = new LinkedList<>();
        for (int i = 0; i < batchSize; i++) {
            ConnectionEntity connectionEntity = TestRandom.nextObject(ConnectionEntity.class);
            connectionEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
            connectionEntity = connectionRepository.save(connectionEntity);
            returnVal.add(ConnectionMapper.INSTANCE.entityToModel(connectionEntity));
        }
        ResourceType[] types = new ResourceType[] {ResourceType.ODC_CONNECTION, ResourceType.ODC_RESOURCE_GROUP};
        List<PermissionEntity> entities = new ArrayList<>(types.length);
        for (ResourceType type : types) {
            for (ConnectionConfig config : returnVal) {
                PermissionEntity entity = new PermissionEntity();
                entity.setId(config.getId());
                entity.setResourceIdentifier(type.name() + ":*");
                entity.setAction("*");
                entities.add(entity);
            }
        }
        Mockito.when(repository.findAll((Specification<PermissionEntity>) Mockito.any()))
                .thenReturn(entities);
        Mockito.when(repository.findByUserIdAndRoleStatusAndOrganizationId(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(entities);

        return returnVal;
    }

    private List<ResourceGroup> insertBatchResourceGroup(int batchSize) {
        List<ResourceGroup> returnVal = new LinkedList<>();
        for (int i = 0; i < batchSize; i++) {
            ResourceGroup resourceGroup =
                    new ResourceGroup(getEntity(i + ""), resourceGroupRepository, resourceGroupConnectionRepository,
                            authenticationFacade);
            resourceGroup.create();
            returnVal.add(resourceGroup);
        }
        return returnVal;
    }

    private QueryResourceGroupReq getQueryParam(String nameLike, Boolean status) {
        QueryResourceGroupReq request = new QueryResourceGroupReq();
        request.setFuzzySearchKeyword(nameLike);
        request.setStatuses(Stream.of(status).filter(Objects::nonNull).collect(Collectors.toList()));
        return request;
    }

}
