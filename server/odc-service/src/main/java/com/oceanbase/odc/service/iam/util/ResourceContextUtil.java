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
package com.oceanbase.odc.service.iam.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.resourcegroup.model.ResourceContext;

/**
 * @author wenniu.ly
 * @date 2021/7/28
 * @since ODC_release_3.2.0
 */
public class ResourceContextUtil {
    public static final String RESOURCE_IDENTIFIER_TOP_LEVEL_DELIMITER = "/";
    public static final String RESOURCE_IDENTIFIER_SECOND_LEVEL_DELIMITER = ";";
    public static final String RESOURCE_IDENTIFIER_ID_DELIMITER = ",";
    public static final String RESOURCE_IDENTIFIER_ALL_RESOURCE = "*";

    private static final String RESOURCE_FIELD_GROUP_NAME = "field";
    private static final String RESOURCE_ID_GROUP_NAME = "id";
    private static Pattern firstLevelPattern = Pattern.compile("^(?<field>([a-zA-Z0-9_])+):(?<id>((\\s*\\d)+)|\\*)$");
    private static Pattern secondLevelPattern =
            Pattern.compile("^(?<field>([a-zA-Z0-9_])+):(?<id>((\\s*\\d+)(,\\s*\\d+)*)|\\*)$");

    /**
     * @param resouceIdentifier
     * @implNote resource identifier expression can have at most two level, until now we do not allow
     *           expression which have more than two level which like A/B/C/.... Obviously for this case
     *           we can split it to three or more [which is n + 1] expressions: A/B, B/C, C/D.... A
     *           reasonable resource identifier example:
     *           resource_group:{id}/public_connection:id1,id2,id3;some_other_resource:id4,id5,id6
     */
    public static ResourceContext parseFromResourceIdentifier(String resouceIdentifier) {
        PreConditions.notEmpty(resouceIdentifier, "resourceIdentifier");
        String[] resourceElements = resouceIdentifier.split(RESOURCE_IDENTIFIER_TOP_LEVEL_DELIMITER);
        Validate.isTrue(resourceElements.length >= 1);
        ResourceContext resourceContext = new ResourceContext();
        String firstLayer = resourceElements[0];
        Matcher matcher = firstLevelPattern.matcher(firstLayer);
        boolean find = matcher.find();
        PreConditions.validArgumentState(find, ErrorCodes.IllegalArgument, null,
                "Field or id in resource identifier can not be null");

        if (StringUtils.isNumeric(matcher.group(RESOURCE_ID_GROUP_NAME))) {
            // id maybe *
            resourceContext.setId(Long.valueOf(matcher.group(RESOURCE_ID_GROUP_NAME)));
        }
        resourceContext.setField(matcher.group(RESOURCE_FIELD_GROUP_NAME));

        if (resourceElements.length >= 2) {
            List<ResourceContext> subResourceContexts = new ArrayList<>();
            String[] secondLevelElements = resourceElements[1].split(RESOURCE_IDENTIFIER_SECOND_LEVEL_DELIMITER);
            for (String element : secondLevelElements) {
                subResourceContexts.addAll(getSubResourceContexts(element));
            }
            resourceContext.setSubContexts(subResourceContexts);
        }
        return resourceContext;
    }

    private static List<ResourceContext> getSubResourceContexts(String subResourceIdentifier) {
        List<ResourceContext> resourceContexts = new ArrayList<>();
        Matcher matcher = secondLevelPattern.matcher(subResourceIdentifier);
        boolean find = matcher.find();
        PreConditions.validArgumentState(find, ErrorCodes.IllegalArgument, null,
                "Field or id in resource identifier can not be null");

        String field = matcher.group(RESOURCE_FIELD_GROUP_NAME);
        String idStr = matcher.group(RESOURCE_ID_GROUP_NAME);
        if (!RESOURCE_IDENTIFIER_ALL_RESOURCE.equals(idStr)) {
            String[] ids = idStr.split(RESOURCE_IDENTIFIER_ID_DELIMITER);
            for (String id : ids) {
                ResourceContext resourceContext = new ResourceContext();
                resourceContext.setId(Long.valueOf(id));
                resourceContext.setField(field);
                resourceContexts.add(resourceContext);
            }
        } else {
            ResourceContext resourceContext = new ResourceContext();
            resourceContext.setField(field);
            resourceContexts.add(resourceContext);
        }
        return resourceContexts;
    }

    public static String generateResourceIdentifierString(Long id, ResourceType resourceType) {
        /**
         * resource_group:10/public_connection:*
         */
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(resourceType).append(":").append(id);
        if (ResourceType.ODC_RESOURCE_GROUP == resourceType) {
            stringBuilder.append("/").append(ResourceType.ODC_CONNECTION)
                    .append(":").append("*");
        }
        return stringBuilder.toString();
    }

    public static boolean matchResourceIdentifier(String resourceIdentifier, Long id, ResourceType resourceType) {
        PreConditions.notNull(id, "resource_id");
        PreConditions.notNull(resourceType, "resource_type");
        ResourceContext resourceContext = parseFromResourceIdentifier(resourceIdentifier);
        return id.equals(resourceContext.getId()) && resourceType.name().equals(resourceContext.getField());
    }
}
