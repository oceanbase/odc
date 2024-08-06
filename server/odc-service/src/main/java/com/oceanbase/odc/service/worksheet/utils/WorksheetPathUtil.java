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
package com.oceanbase.odc.service.worksheet.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;
import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;

/**
 *
 * @author keyang
 * @date 2024/08/01
 * @since 4.3.2
 */
public class WorksheetPathUtil {

    private static final String OBJECT_STORAGE_BUCKET_NAME_PREFIX = "PROJECT_FILE_";
    /**
     * 文件路径标准分隔符
     */
    private static final String STANDARD_PATH_SEPARATOR = "/";

    /**
     * 将path分割为一个个item，支持按“/”和“\”进行分割。
     * </p>
     * 为了区分文件夹和文件，会保留最后一个分隔符
     * 
     * @param path
     * @return
     */
    public static List<String> splitPathToItems(String path) {
        if (StringUtils.isBlank(path)) {
            return new ArrayList<>();
        }

        path = path.trim();
        boolean isDirectory = path.endsWith("/") || path.endsWith("\\");

        String[] arrays = path.split("/");

        List<String> items = Stream.of(arrays)
                .filter(StringUtils::isNotBlank)
                .flatMap(item -> Stream.of(item.split("\\\\")))
                .filter(StringUtils::isNotBlank)
                .map(String::trim).collect(Collectors.toList());

        if (isDirectory) {
            items.add(STANDARD_PATH_SEPARATOR);
        }
        return items;
    }

    public static List<String> addSeparatorToItemsEnd(List<String> items) {
        if (CollectionUtils.isEmpty(items)
                || !StringUtils.equals(items.get(items.size() - 1), STANDARD_PATH_SEPARATOR)) {
            items.add(STANDARD_PATH_SEPARATOR);
        }
        return items;
    }

    /**
     * 将item转换成标准path
     * 
     * @param pathItems
     * @return
     */
    public static Optional<String> convertItemsToPath(List<String> pathItems) {
        if (CollectionUtils.isEmpty(pathItems)) {
            return Optional.empty();
        }
        int size = pathItems.size();
        StringBuilder sb = new StringBuilder();
        sb.append(STANDARD_PATH_SEPARATOR);
        for (int i = 0; i < size - 1; i++) {
            sb.append(pathItems.get(i));
            sb.append(STANDARD_PATH_SEPARATOR);
        }
        boolean isEndWithPathSeparator =
                StringUtils.equals(pathItems.get(size - 1), STANDARD_PATH_SEPARATOR);
        if (!isEndWithPathSeparator) {
            sb.append(pathItems.get(size - 1));
        }
        return Optional.of(sb.toString());
    }

    public static Optional<String> getPathName(List<String> pathItems) {
        if (CollectionUtils.isEmpty(pathItems)) {
            return Optional.empty();
        }
        int size = pathItems.size();
        boolean isEndWithPathSeparator =
                StringUtils.equals(pathItems.get(pathItems.size() - 1), STANDARD_PATH_SEPARATOR);
        if (isEndWithPathSeparator) {
            return size == 1 ? Optional.empty() : Optional.of(pathItems.get(size - 2));
        }
        return Optional.of(pathItems.get(size - 1));
    }

    public static Optional<WorksheetLocation> getPathLocation(List<String> pathItems) {
        if (CollectionUtils.isEmpty(pathItems)) {
            return Optional.empty();
        }
        return WorksheetLocation.getByName(pathItems.get(0));
    }

    public static Optional<WorksheetType> getPathType(List<String> pathItems) {
        Optional<WorksheetLocation> locationOptional = getPathLocation(pathItems);
        if (!locationOptional.isPresent()) {
            return Optional.empty();
        }
        int size = pathItems.size();
        boolean isEndWithPathSeparator =
                StringUtils.equals(pathItems.get(pathItems.size() - 1), STANDARD_PATH_SEPARATOR);

        switch (locationOptional.get()) {
            case REPOS:
                if (isEndWithPathSeparator) {
                    if (size <= 2) {
                        // 对于文件夹来说，由于Repos中必须要带有仓库，
                        // 所以path=/Repos/是不合法的，/Repos/转换成items长度为2（["Repos","/"]）
                        return Optional.empty();
                    } else if (size == 3) {
                        return Optional.of(WorksheetType.GIT_REPO);
                    } else {
                        return Optional.of(WorksheetType.DIRECTORY);
                    }
                }
                if (size <= 2) {
                    // 对于文件来说，由于Repos中必须要带有仓库，文件一定会在git仓库下，即path结构至少为：/Repos/RepoName/file；
                    // 当size<=2时，ptah结构只能为：/Repos、/Repos/RepoName，是不合法的，
                    return Optional.empty();
                }
                return Optional.of(WorksheetType.FILE);
            case WORKSHEETS:
                if (isEndWithPathSeparator) {
                    // 能到这一步，path结构至少为：/Worksheets/，确定是合法的
                    return Optional.of(WorksheetType.DIRECTORY);
                }
                if (size == 1) {
                    // 长度为1的时候，path结构只能为：/Worksheets，是不合法的
                    return Optional.empty();
                }
                return Optional.of(WorksheetType.FILE);
            default:
                return Optional.empty();
        }
    }

    /**
     * 判断重命名是否合法
     * 
     * @param from
     * @param destination
     * @return
     */
    public static boolean isRenameValid(Path from, Path destination) {
        // path相同不能重命名
        // path的parent不同不能重名
        // path的type不同不能重命名
        return from != null && destination != null
                && from.canRename()
                && destination.canRename()
                && !from.equals(destination)
                && CollectionUtils.isEqualCollection(from.getParentPathItems(), destination.getParentPathItems())
                && from.getType() == destination.getType();
    }

    public static String getObjectStorageBucketName(Long projectId) {
        return OBJECT_STORAGE_BUCKET_NAME_PREFIX + projectId;
    }
}
