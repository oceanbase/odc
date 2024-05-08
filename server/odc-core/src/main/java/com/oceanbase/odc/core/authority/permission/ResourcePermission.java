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
package com.oceanbase.odc.core.authority.permission;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.constant.ResourceType;

import lombok.Getter;
import lombok.NonNull;

/**
 * Permission abstraction for resources
 *
 * @author yh263208
 * @date 2021-07-12 17:42
 * @since ODC_release_3.2.0
 */
public class ResourcePermission implements Permission, Serializable {

    private static final long serialVersionUID = 7930723426638008763L;
    /**
     * 资源申请权限，有此权限才能申请资源的其他权限
     */
    public static final String RESOURCE_APPLY_ACTION = "apply";
    public static final String RESOURCE_READ_ACTION = "read";
    public static final String RESOURCE_CREATE_ACTION = "create";
    public static final String RESOURCE_UPDATE_ACTION = "update";
    public static final String RESOURCE_DELETE_ACTION = "delete";

    public static final int APPLY = 0x1;
    public static final int READ = 0x2 | APPLY;
    public static final int CREATE = 0x4 | APPLY;
    public static final int UPDATE = 0x8 | APPLY;
    public static final int DELETE = 0x10 | APPLY;
    /**
     * Higest {@link Permission}
     */
    public static final int ALL = APPLY | READ | CREATE | UPDATE | DELETE;
    /**
     * READ_WRITE {@link Permission} is the synonym for ALL {@link Permission}
     */
    public static final int NONE = 0x0;
    /**
     * Mask to indicate the permisssion. It can be calculated by an expression, eg. READ | WRITE |
     * DELETE
     */
    @Getter
    protected int mask;
    @Getter
    protected final String resourceId;
    @Getter
    protected final String resourceType;

    public ResourcePermission(@NonNull SecurityResource resource, String action) {
        this.resourceId = resource.resourceId();
        this.resourceType = resource.resourceType();
        Validate.notNull(resourceId, "ResourceId can not be null");
        Validate.notNull(resourceType, "ResourceType can not be null");
        initPermissionMask(getMaskFromAction(action));
    }

    public ResourcePermission(String resourceId, String resourceType, String action) {
        this(new SecurityResource() {
            @Override
            public String resourceId() {
                return resourceId;
            }

            @Override
            public String resourceType() {
                return resourceType;
            }
        }, action);
    }

    public ResourcePermission(@NonNull SecurityResource resource, int mask) {
        this.resourceId = resource.resourceId();
        this.resourceType = resource.resourceType();
        Validate.notNull(resourceId, "ResourceId can not be null");
        Validate.notNull(resourceType, "ResourceType can not be null");
        initPermissionMask(mask);
    }

    public ResourcePermission(String resourceId, String resourceType, int mask) {
        this(new SecurityResource() {
            @Override
            public String resourceId() {
                return resourceId;
            }

            @Override
            public String resourceType() {
                return resourceType;
            }
        }, mask);
    }

    protected int getMaskFromAction(String action) {
        int mask = NONE;
        if (action == null || StringUtils.isBlank(action)) {
            return mask;
        }
        String newAction = action.replaceAll(" |\r|\n|\f|\t", "");
        if ("*".equals(action)) {
            return ALL;
        }
        String[] actionList = newAction.split(",");
        for (String actionItem : actionList) {
            if (RESOURCE_CREATE_ACTION.equalsIgnoreCase(actionItem)) {
                mask |= CREATE;
            } else if (RESOURCE_READ_ACTION.equalsIgnoreCase(actionItem)) {
                mask |= READ;
            } else if (RESOURCE_UPDATE_ACTION.equalsIgnoreCase(actionItem)) {
                mask |= UPDATE;
            } else if (RESOURCE_DELETE_ACTION.equalsIgnoreCase(actionItem)) {
                mask |= DELETE;
            } else if (RESOURCE_APPLY_ACTION.equals(actionItem)) {
                mask |= APPLY;
            } else if ("*".equals(actionItem)) {
                return ALL;
            }
        }
        return mask;
    }

    /**
     * Return the canonical string representation of the actions. Always returns present actions in the
     * following order: read, write, execute, delete, readlink.
     *
     * @return the canonical string representation of the actions.
     */
    public static String getActions(int mask) {
        List<String> actionList = getActionList(mask);
        return String.join(",", actionList);
    }

    public static List<String> getActionList(int mask) {
        List<String> actionList = new LinkedList<>();
        if ((mask & ALL) == ALL) {
            actionList.add("*");
            return actionList;
        }
        if ((mask & READ) == READ) {
            actionList.add(RESOURCE_READ_ACTION);
        }
        if ((mask & CREATE) == CREATE) {
            actionList.add(RESOURCE_CREATE_ACTION);
        }
        if ((mask & UPDATE) == UPDATE) {
            actionList.add(RESOURCE_UPDATE_ACTION);
        }
        if ((mask & DELETE) == DELETE) {
            actionList.add(RESOURCE_DELETE_ACTION);
        }
        if ((mask & APPLY) == APPLY) {
            actionList.add(RESOURCE_APPLY_ACTION);
        }
        return actionList;
    }

    protected void initPermissionMask(int mask) {
        Validate.isTrue((mask & ALL) == mask, "Mask value is illegal");
        this.mask = mask;
    }

    public static Set<String> getAllActions() {
        return new HashSet<>(Arrays.asList(RESOURCE_READ_ACTION, RESOURCE_CREATE_ACTION,
                RESOURCE_UPDATE_ACTION, RESOURCE_DELETE_ACTION, RESOURCE_APPLY_ACTION));
    }

    /**
     * Checks if this {@link ResourcePermission} object "implies" the specified permission.
     * <P>
     * More specifically, this method returns true if:
     * <ul>
     * <li><i>p</i> is an instanceof <code>ResourcePermission</code>,
     * <li><i>p</i>'s actions are a proper subset of this object's actions, and
     * </ul>
     *
     * @param p the permission to check against.
     *
     * @return <code>true</code> if the specified permission is not <code>null</code> and is implied by
     *         this object, <code>false</code> otherwise.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof ResourcePermission)) {
            return false;
        }
        ResourcePermission that = (ResourcePermission) p;
        // we get the effective mask. i.e., the "and" of this and that.
        // They must be equal to that.mask for implies to return true.
        return ((this.mask & that.mask) == that.mask) && impliesIgnoreMask(that);
    }

    /**
     * Checks if the Permission's actions are a proper subset of the this object's actions. Returns the
     * effective mask if the this <code>ResourcePermission</code>'s resourceId and resourceType also
     * implies that {@link ResourcePermission}'s.
     *
     * @param that the {@link ResourcePermission} to check against.
     * @return the effective mask
     */
    private boolean impliesIgnoreMask(ResourcePermission that) {
        if (this == that) {
            return true;
        }
        boolean returnVal = (this.resourceId.equals(that.resourceId) || "*".equals(this.resourceId));
        returnVal &= (this.resourceType.equals(that.resourceType) || "*".equals(this.resourceType));
        return returnVal;
    }

    /**
     * Checks two {@link ResourcePermission} objects for equality. Checks that <i>obj</i> is a
     * {@link ResourcePermission}, and has the same pathname and actions as this object.
     *
     * @param obj the object we are testing for equality with this object.
     * @return <code>true</code> if obj is a {@link ResourcePermission}, and has the same resource meta
     *         data and actions as this {@link ResourcePermission} object, <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ResourcePermission)) {
            return false;
        }
        ResourcePermission that = (ResourcePermission) obj;
        return (this.mask == that.mask) && StringUtils.equals(this.resourceId, that.resourceId)
                && StringUtils.equals(this.resourceType, that.resourceType);
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return this.mask ^ this.resourceId.hashCode() ^ this.resourceType.hashCode();
    }

    @Override
    public String toString() {
        String resource = this.resourceType;
        try {
            resource = ResourceType.valueOf(this.resourceType).getLocalizedMessage();
        } catch (Exception e) {
            // eat exception
        }
        return resource + ":" + this.resourceId + ": " + getActions(this.mask);
    }

}
