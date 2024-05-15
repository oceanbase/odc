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
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ResourceType;

/**
 * Permission for <code>Database</code> object.
 * 
 * @author gaoda.xy
 * @date 2024/1/2 14:19
 */
public class DatabasePermission extends ResourcePermission {

    private static final String DATABASE_QUERY_ACTION = "query";
    private static final String DATABASE_CHANGE_ACTION = "change";
    private static final String DATABASE_EXPORT_ACTION = "export";

    private static final int QUERY = 0x80;
    private static final int CHANGE = 0x100;
    private static final int EXPORT = 0x200;

    public static final int ALL = ResourcePermission.ALL | QUERY | CHANGE | EXPORT;

    public DatabasePermission(String resourceId, String action) {
        super(resourceId, ResourceType.ODC_DATABASE.name(), action);
    }

    public DatabasePermission(String resourceId, int mask) {
        super(resourceId, ResourceType.ODC_DATABASE.name(), mask);
    }

    @Override
    protected int getMaskFromAction(String action) {
        int mask = super.getMaskFromAction(action);
        if (action == null || StringUtils.isBlank(action)) {
            return mask;
        }
        String newAction = action.replaceAll(" |\r|\n|\f|\t", "");
        if ("*".equals(newAction)) {
            return ALL;
        }
        String[] actionList = newAction.split(",");
        for (String actionItem : actionList) {
            if (DATABASE_QUERY_ACTION.equalsIgnoreCase(actionItem)) {
                mask |= QUERY;
            } else if (DATABASE_CHANGE_ACTION.equalsIgnoreCase(actionItem)) {
                mask |= CHANGE;
            } else if (DATABASE_EXPORT_ACTION.equalsIgnoreCase(actionItem)) {
                mask |= EXPORT;
            } else if ("*".equals(actionItem)) {
                return ALL;
            }
        }
        return mask;
    }

    @Override
    protected void initPermissionMask(int mask) {
        Validate.isTrue((mask & ALL) == mask, "Mask value is illegal");
        this.mask = mask;
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

    public static Set<String> getAllActions() {
        Set<String> returnVal = ResourcePermission.getAllActions();
        returnVal.addAll(Arrays.asList(DATABASE_QUERY_ACTION, DATABASE_CHANGE_ACTION, DATABASE_EXPORT_ACTION));
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
        if ((mask & QUERY) == QUERY) {
            actionList.add(DATABASE_QUERY_ACTION);
        }
        if ((mask & CHANGE) == CHANGE) {
            actionList.add(DATABASE_CHANGE_ACTION);
        }
        if ((mask & EXPORT) == EXPORT) {
            actionList.add(DATABASE_EXPORT_ACTION);
        }
        return actionList;
    }

}
