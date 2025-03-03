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
package com.oceanbase.odc.service.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.PermissionProvider;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.shared.PermissionConfiguration;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionRepository;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.DefaultPermissionProvider;
import com.oceanbase.odc.service.iam.model.PermissionConfig;
import com.oceanbase.odc.service.iam.util.ResourceContextUtil;
import com.oceanbase.odc.service.resourcegroup.ResourceGroup;
import com.oceanbase.odc.service.resourcegroup.model.ResourceContext;
import com.oceanbase.odc.service.resourcegroup.model.ResourceIdentifier;

import lombok.NonNull;

/**
 * Mapper for <code>Permission</code>, from <code>Permission</code> to
 * <code>ResourcePermission</code>
 *
 * @author yh263208
 * @date 2021-09-01 11:20
 * @since ODC-release_3.2.0
 */
@Component
public class ResourcePermissionExtractor {

    private final PermissionProvider permissionProvider = new DefaultPermissionProvider();
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ResourceGroupRepository resourceGroupRepository;
    @Autowired
    private ResourceGroupConnectionRepository resourceGroupConnectionRepository;
    @Autowired
    private ResourceIdExtractRuleHandlerFactory ruleHandlerFactory;

    public List<Permission> getResourcePermissions(List<? extends PermissionConfiguration> entities) {
        Map<String, Set<String>> identifier2Actions = new HashMap<>();
        for (PermissionConfiguration entity : entities) {
            Set<String> actions =
                    identifier2Actions.computeIfAbsent(entity.resourceIdentifier(), s -> new HashSet<>());
            actions.addAll(entity.actions());
        }
        List<Permission> permissions = new LinkedList<>();
        Map<ResourceIdentifier, List<ResourceIdentifier>> resourceGroupCache = new HashMap<>();
        for (Entry<String, Set<String>> entry : identifier2Actions.entrySet()) {
            Set<String> actions = entry.getValue();
            List<SecurityResource> resources = getResourcesByIdentifier(entry.getKey(), resourceGroupCache);
            for (SecurityResource resource : resources) {
                Permission resourcePermission = permissionProvider.getPermissionByActions(resource, actions);
                if (!(resourcePermission instanceof ResourcePermission)) {
                    throw new IllegalStateException("Permission's type is illegal " + resourcePermission);
                }
                permissions.add(resourcePermission);
            }
        }
        return permissions;
    }


    public List<PermissionConfig> aggregatePermissions(List<PermissionEntity> permissionEntities) {
        Map<String, Set<String>> identifier2Actions = new HashMap<>();
        for (PermissionEntity permissionEntity : permissionEntities) {
            if (Objects.nonNull(permissionEntity)) {
                Set<String> action = identifier2Actions.computeIfAbsent(permissionEntity.getResourceIdentifier(),
                        e -> new HashSet<>());
                action.add(permissionEntity.getAction());
            }
        }
        List<PermissionConfig> aggregated = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : identifier2Actions.entrySet()) {
            Set<String> actions = entry.getValue();
            List<SecurityResource> resources = getResourcesByIdentifier(entry.getKey(), new HashMap<>());
            for (SecurityResource resource : resources) {
                aggregated.add(new PermissionConfig(String.valueOf(resource.resourceId()),
                        ResourceType.valueOf(resource.resourceType()), new ArrayList<>(actions)));
            }
        }
        return aggregated;
    }

    public List<PermissionConfig> aggregateResourceManagementPermissions(List<PermissionEntity> permissionEntities) {
        Map<String, Set<String>> identifier2Actions = new HashMap<>();
        for (PermissionEntity permissionEntity : permissionEntities) {
            if (Objects.nonNull(permissionEntity)) {
                Set<String> action = identifier2Actions.computeIfAbsent(permissionEntity.getResourceIdentifier(),
                        e -> new HashSet<>());
                action.add(permissionEntity.getAction());
            }
        }
        List<PermissionConfig> aggregated = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : identifier2Actions.entrySet()) {
            Set<String> actions = entry.getValue();
            List<SecurityResource> resources = getResourcesByIdentifier(entry.getKey(), new HashMap<>());
            for (SecurityResource resource : resources) {
                // Separately extract the permission configuration with "create" for resource management permissions
                if (entry.getValue().contains("create") && entry.getValue().size() > 1) {
                    aggregated.add(new PermissionConfig("*", ResourceType.valueOf(resource.resourceType()),
                            Collections.singletonList("create")));
                    entry.getValue().remove("create");
                }
                aggregated.add(new PermissionConfig(String.valueOf(resource.resourceId()),
                        ResourceType.valueOf(resource.resourceType()), new ArrayList<>(actions)));
            }
        }
        return aggregated;
    }

    public List<SecurityResource> getResourcesByIdentifier(@NonNull String identifier,
            @NonNull Map<ResourceIdentifier, List<ResourceIdentifier>> resourceGroupCache) {
        ResourceContext rootContext = ResourceContextUtil.parseFromResourceIdentifier(identifier);
        List<ResourceContext> contexts = new LinkedList<>();
        getContexts(contexts, rootContext, resourceGroupCache);
        return contexts.stream()
                .flatMap(context -> {
                    if (context.getId() == null) {
                        // it should be extracted by rules
                        return ruleHandlerFactory.getHandler(context.getIdExtractRule())
                                .handle(context, authenticationFacade)
                                .stream();
                    }
                    // return the single id
                    return Stream.of(new DefaultSecurityResource(String.valueOf(context.getId()), context.getField()));
                })
                .collect(Collectors.toList());
    }


    private void getContexts(List<ResourceContext> contexts, ResourceContext root,
            Map<ResourceIdentifier, List<ResourceIdentifier>> resourceGroupCache) {
        if (root.getSubContexts() == null || root.getSubContexts().isEmpty()) {
            contexts.add(root);
            return;
        }
        for (ResourceContext item : root.getSubContexts()) {
            if (item.getId() != null) {
                getContexts(contexts, item, resourceGroupCache);
                continue;
            }
            Verify.notNull(root.getId(), "ResourceContextId");
            Verify.verify(root.getField().equalsIgnoreCase(ResourceType.ODC_RESOURCE_GROUP.name()),
                    "ResourceType is illegal, " + root.getField());
            List<ResourceIdentifier> identifiers = resourceGroupCache.computeIfAbsent(
                    new ResourceIdentifier(root.getId(), ResourceType.ODC_RESOURCE_GROUP), identifier -> {
                        ResourceGroup resourceGroup;
                        try {
                            resourceGroup = new ResourceGroup(identifier.getResourceId(), resourceGroupRepository,
                                    resourceGroupConnectionRepository, authenticationFacade);
                        } catch (NotFoundException e) {
                            return Collections.emptyList();
                        }
                        if (!resourceGroup.isEnabled()) {
                            return Collections.emptyList();
                        }
                        return resourceGroup.getRelatedResources(ResourceType.ODC_CONNECTION);
                    });
            for (ResourceIdentifier identifier : identifiers) {
                ResourceContext resourceContext = new ResourceContext();
                resourceContext.setField(identifier.getResourceType().name());
                resourceContext.setId(identifier.getResourceId());
                getContexts(contexts, resourceContext, resourceGroupCache);
            }
        }
    }

}
