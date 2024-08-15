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

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.ROOT_PATH_NAME;

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
            return size == 1 ? Optional.of(ROOT_PATH_NAME) : Optional.of(pathItems.get(size - 2));
        }
        return Optional.of(pathItems.get(size - 1));
    }

    public static Optional<WorksheetLocation> getPathLocation(List<String> standardPathItems) {
        if (CollectionUtils.isEmpty(standardPathItems)) {
            return Optional.empty();
        }
        return WorksheetLocation.getByValue(standardPathItems.get(0));
    }

    public static Optional<WorksheetType> getPathType(List<String> standardPathItems) {
        Optional<WorksheetLocation> locationOptional = getPathLocation(standardPathItems);
        if (!locationOptional.isPresent()) {
            return Optional.empty();
        }
        int size = standardPathItems.size();
        boolean isEndWithPathSeparator =
                StringUtils.equals(standardPathItems.get(standardPathItems.size() - 1), STANDARD_PATH_SEPARATOR);

        switch (locationOptional.get()) {
            case ROOT:
                if (size != 1) {
                    return Optional.empty();
                }
                return Optional.of(WorksheetType.ROOT);
            case REPOS:
                // the formats of REPOS are:
                // [Repos,/],[Repos,RepoName,...,/],[Repos,RepoName,file,...],[Repos,RepoName,folder,...,/]

                if (size == 1) {
                    // for standardPathItems = [Repos],it's invalid.
                    return Optional.empty();
                }
                if (isEndWithPathSeparator) {
                    if (size == 2) {
                        return Optional.of(WorksheetType.REPOS);
                    } else if (size == 3) {
                        return Optional.of(WorksheetType.GIT_REPO);
                    } else {
                        return Optional.of(WorksheetType.DIRECTORY);
                    }
                }
                if (size == 2) {
                    // for standardPathItems = [Repos,RepoName],it's invalid.
                    return Optional.empty();
                }
                return Optional.of(WorksheetType.FILE);
            case WORKSHEETS:
                // the formats of WORKSHEETS are:
                // [Worksheets,/],[Worksheets,folder,...,/],[Worksheets,file,...]

                if (size == 1) {
                    // for standardPathItems = [Worksheets],it's invalid.
                    return Optional.empty();
                }
                if (isEndWithPathSeparator) {
                    if (size == 2) {
                        // [Worksheets,/]
                        return Optional.of(WorksheetType.WORKSHEETS);
                    }
                    // [Worksheets,folder,...,/]
                    return Optional.of(WorksheetType.DIRECTORY);
                }
                // [Worksheets,file,...]
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

    public static Path findCommonParentPath(Set<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            throw new IllegalArgumentException("paths is empty");
        }
        if (paths.size() == 1) {
            return paths.iterator().next();
        }

        WorksheetLocation commonLocation = null;
        int index = 0;
        Integer commonParentIndex = null;
        while (true) {
            Path pathAtIndex = null;
            boolean isContinue = true;
            for (Path path : paths) {
                if (commonLocation == null) {
                    commonLocation = path.getLocation();
                } else if (commonLocation != path.getLocation()) {
                    isContinue = false;
                    break;
                }
                if (index == path.getLevelNum()) {
                    isContinue = false;
                    break;
                }
                if (pathAtIndex == null) {
                    pathAtIndex = path.getPathAt(index);
                } else if (!pathAtIndex.equals(path.getPathAt(index))) {
                    isContinue = false;
                    break;
                }
            }
            if (!isContinue || pathAtIndex == null) {
                break;
            }
            commonParentIndex = index;
            index++;
        }
        if (commonParentIndex == null) {
            return Path.root();
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
