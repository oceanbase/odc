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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.shared.constant.ResourceType;

/**
 * Permission for <code>Connection</code> object
 *
 * @author yh263208
 * @date 2021-08-06 14:59
 * @since ODC_release_3.2.0
 * @see Permission
 * @see ResourcePermission
 */
public class ConnectionPermission extends ResourcePermission {
    private static final long serialVersionUID = 7923423426638008763L;

    public static final String CONNECTION_READONLY = "readonlyconnect";
    public static final String CONNECTION_READWRITE = "connect";

    protected static final int CONNECT = 0x20;
    protected static final int MODIFY = 0x40;
    public static final int READONLY_CONNECT = ResourcePermission.READ | CONNECT;
    public static final int READWRITE_CONNECT = READONLY_CONNECT | MODIFY;

    public static final int ALL = ResourcePermission.ALL | CONNECT | MODIFY;

    public ConnectionPermission(String resourceId, String action) {
        super(resourceId, ResourceType.ODC_CONNECTION.name(), action);
    }

    public ConnectionPermission(String resourceId, int mask) {
        super(resourceId, ResourceType.ODC_CONNECTION.name(), mask);
    }

    @Override
    protected int getMaskFromAction(String action) {
        int mask = super.getMaskFromAction(action);
        if (action == null || StringUtils.isBlank(action)) {
            return mask;
        }
        String newAction = action.replaceAll(" |\r|\n|\f|\t", "");
        if ("*".equals(action)) {
            return ALL;
        }
        String[] actionList = newAction.split(",");
        for (String actionItem : actionList) {
            if (CONNECTION_READONLY.equalsIgnoreCase(actionItem)) {
                mask |= READONLY_CONNECT;
            } else if (CONNECTION_READWRITE.equalsIgnoreCase(actionItem)) {
                mask |= READWRITE_CONNECT;
            } else if ("*".equals(actionItem)) {
                return ALL;
            }
        }
        return mask;
    }

    public static Set<String> getAllActions() {
        Set<String> returnVal = ResourcePermission.getAllActions();
        returnVal.addAll(Arrays.asList(CONNECTION_READONLY, CONNECTION_READWRITE));
        return returnVal;
    }

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
        List<String> actions = ResourcePermission.getActionList(mask);
        if (CollectionUtils.containsAny(actions, "*")) {
            actionList.addAll(ResourcePermission.getAllActions());
        } else {
            actionList.addAll(actions);
        }
        if ((mask & READONLY_CONNECT) == READONLY_CONNECT) {
            actionList.add(CONNECTION_READONLY);
        }
        if ((mask & READWRITE_CONNECT) == READWRITE_CONNECT) {
            actionList.add(CONNECTION_READWRITE);
        }
        return actionList;
    }

    @Override
    protected void initPermissionMask(int mask) {
        Validate.isTrue((mask & ALL) == mask, "Mask value is illegal");
        this.mask = mask;
    }

    @Override
    public String toString() {
        return ResourceType.ODC_CONNECTION.getLocalizedMessage() + ":" + this.resourceId + ": " + getActions(this.mask);
    }

}
