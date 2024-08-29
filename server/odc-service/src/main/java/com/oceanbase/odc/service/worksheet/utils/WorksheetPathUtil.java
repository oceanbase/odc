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

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstants.ROOT_PATH_NAME;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
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
     * <p>
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
     * @param renamePath
     * @param destinationPath
     * @return
     */
    public static void checkRenameValid(Path renamePath, Path destinationPath) {
        PreConditions.notNull(renamePath, "renamePath");
        PreConditions.notNull(destinationPath, "destinationPath");
        // The type of renamePath and destinationPath should be same
        PreConditions.validArgumentState(renamePath.getType() == destinationPath.getType(),
                ErrorCodes.BadArgument, null,
                "the type of renamePath:" + renamePath + " and destinationPath:" + destinationPath + " is not same");
        // System define path cannot rename
        PreConditions.validArgumentState(!renamePath.isSystemDefine(),
                ErrorCodes.BadArgument, null, renamePath + " can't rename");
        PreConditions.validArgumentState(!destinationPath.isSystemDefine(),
                ErrorCodes.BadArgument, null, "same path:" + destinationPath + " cannot be rename");
        // Same path cannot be renamed
        PreConditions.validArgumentState(!renamePath.equals(destinationPath),
                ErrorCodes.BadArgument, null,
                "rename path is equals to destination path:" + destinationPath + " cannot be rename");
        // The parents of renamePath and destinationPath should be same
        PreConditions.validArgumentState(
                CollectionUtils.isEqualCollection(renamePath.getParentPathItems(),
                        destinationPath.getParentPathItems()),
                ErrorCodes.BadArgument, null,
                "the parent of renamePath:" + renamePath + " and destinationPath:" + destinationPath + " is not same");
    }

    public static void checkMoveValid(Path movePath, Path destinationPath) {
        PreConditions.notNull(movePath, "movePath");
        PreConditions.notNull(destinationPath, "destinationPath");
        // move path cannot be system define path(type=Root/Worksheets/Repos/Git_Repo)
        PreConditions.validArgumentState(!movePath.isSystemDefine(),
                ErrorCodes.BadArgument, null, movePath + " can't move");
        // destination path cannot be type=Root/Repos path
        PreConditions.validArgumentState(destinationPath.getType() != WorksheetType.ROOT
                && destinationPath.getType() != WorksheetType.REPOS,
                ErrorCodes.BadArgument, null, movePath + " can't move");
        // the location between movePath and destinationPath must be same;
        PreConditions.validArgumentState(movePath.getLocation() == destinationPath.getLocation(),
                ErrorCodes.BadArgument, null,
                "the location of movePath:" + movePath + " and destinationPath:" + destinationPath + " is not same");
        // movePath cannot be the parent of destinationPath;
        PreConditions.validArgumentState(
                !destinationPath.isChildOfAny(movePath),
                ErrorCodes.BadArgument, null,
                "movePath:" + movePath + " cannot be the parent of destinationPath:" + destinationPath);
        // Same path cannot be moved
        PreConditions.validArgumentState(!movePath.equals(destinationPath),
                ErrorCodes.BadArgument, null,
                "move path is equals to destination path:" + destinationPath + " ,cannot be moved");

        // destinationPath can't be a file when movePath is a directory.
        PreConditions.validArgumentState(!(movePath.isDirectory() && destinationPath.isFile()),
                ErrorCodes.BadArgument, null,
                "destinationPath:" + destinationPath + " can't be a file when movePath:" + movePath
                        + " is a directory");
    }

    public static void checkMoveValidWithDestinationPathExist(Path movePath, Path destinationPath) {
        // when destinationPath has existed,movePath and destinationPath cannot both be files at the same
        // time
        if (movePath.isFile() && destinationPath.isFile()) {
            throw new BadRequestException(ErrorCodes.DuplicatedExists,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "destinationPath", destinationPath},
                    "duplicated path name for rename or move,movePath:" + movePath
                            + ",destinationPath:" + destinationPath);
        }
    }

    public static void checkMoveValidWithDestinationPathNotExist(Path movePath, Path destinationPath) {
        // when destinationPath is not exist and movePath is file, the destinationPath must not be a
        // directory/Worksheets/GitRepo.
        PreConditions.validArgumentState(!(movePath.isFile()
                && (destinationPath.isDirectory() || destinationPath.isWorksheets() || destinationPath.isGitRepo())),
                ErrorCodes.BadArgument, null,
                "when destinationPath is not exist and movePath:" + movePath
                        + " is file, the destinationPath:" + destinationPath + " must not be a"
                        + " directory/Worksheets/GitRepo");
    }

    public static Path findCommonPath(Set<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            throw new IllegalArgumentException("paths is empty");
        }
        if (paths.size() == 1) {
            return paths.iterator().next();
        }

        WorksheetLocation commonLocation = null;
        int index = 0;
        Integer commonIndex = null;
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
            commonIndex = index;
            index++;
        }
        if (commonIndex == null) {
            return Path.root();
        }
        return paths.iterator().next().getPathAt(commonIndex);
    }

    public static java.io.File createFileWithParent(String pathStr, boolean isDirectory) {
        java.io.File file = new File(pathStr);
        try {
            if (isDirectory) {
                FileUtils.forceMkdir(file);
            } else {
                FileUtils.touch(file);
            }
            return file;
        } catch (IOException e) {
            throw new InternalServerError("create file error,pathStr: " + pathStr, e);
        }
    }
}
