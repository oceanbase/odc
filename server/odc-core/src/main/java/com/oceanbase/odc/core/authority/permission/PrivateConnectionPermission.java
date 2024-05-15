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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.shared.constant.ResourceType;

/**
 * Permission for private <code>Connection</code> object
 *
 * @author yh263208
 * @date 2021-09-06 19:23
 * @since ODC_release_3.2.0
 * @see Permission
 * @see ResourcePermission
 */
public class PrivateConnectionPermission extends ResourcePermission {
    private static final long serialVersionUID = 7923423426638008112L;

    public static final String CONNECTION_USE = "use";

    public static final int CONNECT = 0x20;
    public static final int USE = ResourcePermission.READ | CONNECT;
    public static final int ALL = ResourcePermission.ALL | CONNECT;

    public PrivateConnectionPermission(String resourceId, String action) {
        super(resourceId, ResourceType.ODC_PRIVATE_CONNECTION.name(), action);
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
            if (CONNECTION_USE.equalsIgnoreCase(actionItem)) {
                mask |= USE;
            } else if ("*".equals(actionItem)) {
                return ALL;
            }
        }
        return mask;
    }

    public static Set<String> getAllActions() {
        Set<String> returnVal = ResourcePermission.getAllActions();
        returnVal.addAll(Collections.singletonList(CONNECTION_USE));
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
        if ((mask & USE) == USE) {
            actionList.add(CONNECTION_USE);
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
        return ResourceType.ODC_PRIVATE_CONNECTION.getLocalizedMessage()
                + ":" + this.resourceId + ": " + getActions(this.mask);
    }

}
