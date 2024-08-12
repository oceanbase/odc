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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;

/**
 *
 * @author keyang
 * @date 2024/08/01
 * @since 4.3.2
 */
public class WorksheetPathUtil {
    /**
     * standard delimiter of path
     */
    private static final String STANDARD_PATH_SEPARATOR = "/";

    /**
     * Split the path into individual items, supporting splitting by "/" and "\".
     * </p>
     * To distinguish between folders and files, the last delimiter will be retained
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
        List<String> result = new ArrayList<>(items);
        if (CollectionUtils.isEmpty(result)
                || !StringUtils.equals(result.get(result.size() - 1), STANDARD_PATH_SEPARATOR)) {
            result.add(STANDARD_PATH_SEPARATOR);
        }
        return result;
    }

    /**
     * convert item list to standard path
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
                        // For folders, since a repository must be included in the Repos,
                        // So path=/Repos/ is illegal, converting /Repos/ to items with a length of 2 (["Repos", "/"])
                        return Optional.empty();
                    } else if (size == 3) {
                        return Optional.of(WorksheetType.GIT_REPO);
                    } else {
                        return Optional.of(WorksheetType.DIRECTORY);
                    }
                }
                if (size <= 2) {
                    // For files, since a repository must be included in the Repos,
                    // the file will always be in the Git repository, with a path structure of at
                    // least:/Repos/RepoName/file;
                    // When size<=2, the ptah structure can only be:/Repos,/Repos/RepoName, which is illegal,
                    return Optional.empty();
                }
                return Optional.of(WorksheetType.FILE);
            case WORKSHEETS:
                if (isEndWithPathSeparator) {
                    // At this point, the path structure should be at least:/Worksheets/, and it should be confirmed to
                    // be legal
                    return Optional.of(WorksheetType.DIRECTORY);
                }
                if (size == 1) {
                    // When the length is 1, the path structure can only be:/Worksheets, which is illegal
                    return Optional.empty();
                }
                return Optional.of(WorksheetType.FILE);
            default:
                return Optional.empty();
        }
    }

    /**
     * Determine whether renaming is legal
     * 
     * @param from
     * @param destinationPath
     * @return
     */
    public static boolean isRenameValid(Path from, Path destinationPath) {
        // Same path cannot be renamed
        // The parents of the from and destinationPath are same nam
        // Cannot rename paths with different types
        return from != null && destinationPath != null
                && from.canRename()
                && destinationPath.canRename()
                && !from.equals(destinationPath)
                && CollectionUtils.isEqualCollection(from.getParentPathItems(), destinationPath.getParentPathItems())
                && from.getType() == destinationPath.getType();
    }

    public static Optional<Path> findCommonParentPath(Set<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            return Optional.empty();
        }
        if (paths.size() == 1) {
            return Optional.of(paths.iterator().next());
        }

        WorksheetLocation commonLocation = null;
        int index = 0;
        Integer commonParentIndex = null;
        while (true) {
            String itemAtIndex = null;
            boolean isContinue = true;
            for (Path path : paths) {
                if (commonLocation == null) {
                    commonLocation = path.getLocation();
                } else if (commonLocation != path.getLocation()) {
                    isContinue = false;
                    break;
                }
                if (index == (path.getParentPathItems().size())) {
                    isContinue = false;
                    break;
                }
                if (itemAtIndex == null) {
                    itemAtIndex = path.getParentPathItems().get(index);
                } else if (!StringUtils.equals(itemAtIndex, path.getParentPathItems().get(index))) {
                    isContinue = false;
                    break;
                }
            }
            if (!isContinue || itemAtIndex == null) {
                break;
            }
            commonParentIndex = index;
            index++;
        }
        if (commonParentIndex == null) {
            return Optional.empty();
        }
        return paths.iterator().next().getPathAt(commonParentIndex);
    }

    public static java.nio.file.Path createFileWithParent(String pathStr, boolean isDirectory) {
        java.nio.file.Path filePath = Paths.get(pathStr);
        try {
            java.nio.file.Path parentDir = filePath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir); // 创建所有缺失的父目录
            }

            return isDirectory ? Files.createDirectory(filePath) : Files.createFile(filePath);
        } catch (IOException e) {
            throw new InternalServerError("create file error,pathStr: " + pathStr, e);
        }
    }
}
