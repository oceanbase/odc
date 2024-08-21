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
package com.oceanbase.odc.service.worksheet.domain;

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.druid.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;

import lombok.Data;

/**
 * worksheet handle.
 * 
 * <pre>
 * <code>
 * /Worksheets/
 *  |__folder1/
 *      |__file2.sql
 *      |__folder4
 *          |__file5.sql
 *  |__folder3/
 *      |__file3.sql
 *  |__file1.sql
 * </code>
 * </pre>
 * 
 * The above is an example worksheet tree in this class. The example current worksheet path is
 * <code>/Worksheets/folder1/</code>.
 *
 * @author keyangs
 * @date 2024/8/1
 * @since 4.3.2
 */
@Data
public class Worksheet {
    private Long id;
    private Date createTime;
    private Date updateTime;
    private Long projectId;
    private Path path;
    private Long creatorId;
    private Long version;
    /**
     * When reading the version, if this value is not empty, version verification is required when
     * updating
     */
    private Long readVersion;
    private String contentDownloadUrl;
    private String objectId;

    private boolean isChanged = false;

    /**
     * The worksheets that are children of current direct parent, excluding the current and its sub
     * worksheets.
     * <p>
     * for example in this class, the paths of sameParentAtPrevLevelWorksheets are
     * <code>[/Worksheets/folder3/,/Worksheets/folder3/file3.sql,/Worksheets/file1.sql]</code>.
     * </p>
     */
    private Set<Worksheet> sameDirectParentWorksheets;
    /**
     * All sub worksheets of current worksheet.
     * <p>
     * for example in this class,the paths of subWorksheets are
     * <code>[/Worksheets/folder1/folder4/,/Worksheets/folder1/file2.sql]</code>.
     * </p>
     */
    private Set<Worksheet> subWorksheets;

    public static Worksheet ofTemp(Long projectId, Path path) {
        return Worksheet.of(projectId, path == null ? Path.root() : path, null, null);
    }

    public boolean isTemp() {
        return this.id == null;
    }

    public static Worksheet of(Long projectId, Path path, String objectId, Long creatorId) {
        return new Worksheet(null, null, null, projectId, path, creatorId,
                null, objectId, null, null);
    }

    public Worksheet(Long id, Date createTime, Date updateTime, Long projectId, Path path, Long creatorId, Long version,
            String objectId, Set<Worksheet> sameDirectParentWorksheets, Set<Worksheet> subWorksheets) {
        this.id = id;
        this.createTime = createTime;
        this.updateTime = updateTime;
        PreConditions.notNull(projectId, "projectId");
        this.projectId = projectId;
        PreConditions.notNull(path, "path");
        this.path = path;
        this.creatorId = creatorId;
        this.version = version == null ? 0L : version;
        if (path.isFile() && id != null) {
            PreConditions.notBlank(objectId, "objectId");
        }
        this.objectId = objectId;
        this.sameDirectParentWorksheets =
                sameDirectParentWorksheets == null ? new HashSet<>() : sameDirectParentWorksheets;
        this.subWorksheets = subWorksheets == null ? new HashSet<>() : subWorksheets;

    }

    public List<Worksheet> getSubWorksheetsInDepth(Integer depth) {
        PreConditions.notNull(depth, "depth");
        PreConditions.validArgumentState(depth >= 0, ErrorCodes.IllegalArgument, null,
                "depth must be greater than or equal to 0");
        if (CollectionUtils.isEmpty(subWorksheets)) {
            return new ArrayList<>();
        }
        return subWorksheets.stream()
                .flatMap(worksheet -> splitWorksheetWithLevelNumBiggerThanCurrent(worksheet).stream())
                .filter(worksheet -> depth == 0
                        || (long) worksheet.getPath().getLevelNum() <= (long) this.path.levelNum + (long) depth)
                .collect(Collectors.toMap(Worksheet::getPath, w -> w, (w1, w2) -> {
                    if (w1.getId() == null && w2.getId() == null) {
                        if (w1.getCreateTime() == null) {
                            return w2;
                        } else if (w2.getCreateTime() == null) {
                            return w1;
                        }
                        return w1.getCreateTime().compareTo(w2.getCreateTime()) > 0 ? w2 : w1;
                    }
                    if (w1.getId() != null && w2.getId() != null) {
                        if (w1.getUpdateTime() == null) {
                            return w2;
                        } else if (w2.getUpdateTime() == null) {
                            return w1;
                        }
                        return w1.getUpdateTime().compareTo(w2.getUpdateTime()) > 0 ? w1 : w2;
                    }
                    if (w1.getId() == null) {
                        return w2;
                    }
                    return w1;
                })).values().stream()
                .sorted((o1, o2) -> Path.getPathComparator().compare(o1.getPath(), o2.getPath()))
                .collect(Collectors.toList());
    }

    /**
     * split {@param worksheet} to get every level parent worksheets,and the level num of paren
     * worksheets should bigger than the level num of current worksheet.
     * <p>
     * for example: current worksheet path is <code>/Worksheets/dir1/</code>, the split worksheet is
     * <code>/Worksheets/dir1/dir2/file1</code>, and the result worksheets are
     * <code>[/Worksheets/dir1/dir2/,/Worksheets/dir1/dir2/file1]</code>
     * </p>
     * 
     * @param worksheet the worksheet need to split
     * @return
     */
    private List<Worksheet> splitWorksheetWithLevelNumBiggerThanCurrent(Worksheet worksheet) {
        List<Worksheet> result = new ArrayList<>();
        int currentWorksheetDepth = this.path.getLevelNum();
        for (Path parentPath : worksheet.getPath().getAllNotRootParents()) {
            if (parentPath.getLevelNum() <= currentWorksheetDepth) {
                continue;
            }
            Worksheet parentWorksheet = new Worksheet(null, worksheet.getCreateTime(), worksheet.getUpdateTime(),
                    this.projectId, parentPath, worksheet.getCreatorId(), 0L, null, null, null);
            result.add(parentWorksheet);
        }
        result.add(worksheet);
        return result;
    }

    /**
     * rename path of current worksheet to {@param destinationPath}
     *
     * @param destinationPath
     * @return all changed worksheets ,contain current
     */
    public Set<Worksheet> rename(Path destinationPath) {
        nameTooLongCheck(Collections.singleton(destinationPath));
        WorksheetPathUtil.renameValidCheck(this.path, destinationPath);
        if (this.isRenameDuplicated(destinationPath)) {
            throw new BadRequestException(ErrorCodes.DuplicatedExists,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "name", destinationPath.getName()},
                    "duplicated path name for rename,from:" + this.path + ",destinationPath:" + destinationPath);
        }
        Set<Worksheet> changedWorksheets = new HashSet<>();
        if (CollectionUtils.isNotEmpty(subWorksheets)) {
            for (Worksheet subFile : subWorksheets) {
                if (subFile.path.rename(this.path, destinationPath)) {
                    changedWorksheets.add(subFile);
                    subFile.isChanged = true;
                }
                if (changedWorksheets.size() > CHANGE_FILE_NUM_LIMIT - 1) {
                    throw new OverLimitException(LimitMetric.WORKSHEET_CHANGE_NUM, (double) CHANGE_FILE_NUM_LIMIT,
                            "change num is over limit " + CHANGE_FILE_NUM_LIMIT);
                }
            }
        }
        if (this.path.rename(this.path, destinationPath)) {
            this.isChanged = true;
            changedWorksheets.add(this);
        }
        return changedWorksheets;
    }

    /**
     * Edit the name and content（actually, it's objectId） of the current worksheet，
     *
     * @param destinationPath If the destinationPath for renaming is the same as the current path, do
     *        not rename it
     * @param objectId After editing the worksheet content, frontend will upload the worksheet the
     *        object to OSS with objectId. If it is the same as the current one, it means that the
     *        content has not been changed
     * @param readVersion When the frontend reads the file version, if the value of objectId is not
     *        empty, version verification is required for updating. If version is empty, version
     *        verification is not performed
     * @return all changed worksheets ,contain current
     */
    public Set<Worksheet> edit(Path destinationPath, String objectId, Long readVersion) {
        if (this.isTemp()) {
            throw new NotFoundException(ResourceType.ODC_WORKSHEET, "path", this.path);
        }
        // if the objectId is changed, will to determine whether the readVersion meets the criteria
        if (this.path.isFile() && !StringUtils.equals(this.objectId, objectId)) {
            this.objectId = objectId;
            this.readVersion = readVersion;
            this.isChanged = true;
            if (isVersionConflict()) {
                throw new BadRequestException(ErrorCodes.EditVersionConflict,
                        new Object[] {}, "version conflict,current version:" + this.version
                                + ",read version:" + this.readVersion + ",path:" + this.path);
            }
            this.version++;
        }

        Set<Worksheet> changedSubFiles = new HashSet<>();
        // destination and current path are the same, name needs to be changed
        if (!this.path.equals(destinationPath)) {
            changedSubFiles = rename(destinationPath);
        }

        if (this.isChanged) {
            changedSubFiles.add(this);
        }

        return changedSubFiles;
    }

    /**
     * Verify if the renamed destinationPath duplicates the existing worksheets path.
     * <p>
     * <b>annotation: if the destinationPath is parent of any worksheet path in
     * sameParentAtPrevLevelWorksheets(even if the destination does not exist in
     * sameParentAtPrevLevelWorksheets), is also rename duplicated.The reason for handling this is，the
     * name of the sub worksheets for renaming the directory may duplicate with the name of the sub
     * worksheet for destination </b>
     * </p>
     *
     * @param destinationPath the destination path to rename
     * @return true duplicated ;false not duplicated
     */
    public boolean isRenameDuplicated(Path destinationPath) {
        if (CollectionUtils.isEmpty(sameDirectParentWorksheets)) {
            return false;
        }
        for (Worksheet worksheet : sameDirectParentWorksheets) {
            // same name with destination(whatever the worksheet.path is a file or a folder)
            if (StringUtils.equals(worksheet.path.name, destinationPath.name)
                    // the worksheet.path is a child of the destination
                    || (destinationPath.isDirectory() && worksheet.path.isChildOfAny(destinationPath))) {
                return true;
            }
        }
        return false;
    }



    /**
     * Check if there is a version conflict when updating the current worksheet content
     * 
     * @return true has conflict，false no conflict
     */
    private boolean isVersionConflict() {
        return this.readVersion != null && !this.readVersion.equals(this.version);
    }

    public Worksheet create(Path addPath, String objectId, Long creatorId) {
        return batchCreate(Collections.singletonMap(addPath, objectId), creatorId)
                .stream().findFirst()
                // can definitely obtain a created Worksheet here.
                // Adding exception checking is only for the integrity of the program
                .orElseThrow(() -> new IllegalStateException("unexpected exception"));
    }

    public Set<Worksheet> batchCreate(Map<Path, String> createPathToObjectIdMap, Long creatorId) {
        List<Worksheet> nextLevelWorksheets = this.getSubWorksheetsInDepth(1);
        nameTooLongCheck(createPathToObjectIdMap.keySet());
        sameLevelFileNumLimitCheck(createPathToObjectIdMap, nextLevelWorksheets);
        duplicatedNameCheck(createPathToObjectIdMap, nextLevelWorksheets);
        return createPathToObjectIdMap.entrySet().stream().map(
                entry -> Worksheet.of(this.projectId, entry.getKey(), entry.getValue(), creatorId))
                .collect(Collectors.toSet());
    }

    /**
     * When creating worksheets in batch, check the limit on the number of worksheets in the same level
     * directory
     * 
     * @param createPathToObjectIdMap The map of the path to be added and its corresponding objectId
     * @param nextLevelWorksheets List of the next level worksheets of the current path
     */
    private void sameLevelFileNumLimitCheck(Map<Path, String> createPathToObjectIdMap,
            List<Worksheet> nextLevelWorksheets) {

        if (nextLevelWorksheets.size() +
                createPathToObjectIdMap.size() > SAME_LEVEL_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_SAME_LEVEL,
                    (double) SAME_LEVEL_NUM_LIMIT,
                    "create path num exceed limit, create path num: " + createPathToObjectIdMap.size()
                            + ", same level exist file num: " + nextLevelWorksheets.size());
        }
    }

    /**
     * Check whether the worksheet paths created in bulk are duplicated with existing worksheets
     */
    private void duplicatedNameCheck(Map<Path, String> createPathToObjectIdMap,
            List<Worksheet> nextLevelWorksheets) {
        if (CollectionUtils.isEmpty(nextLevelWorksheets)) {
            return;
        }
        Map<String, Path> willToCreatePathNameToPathMap =
                createPathToObjectIdMap.keySet().stream().collect(
                        Collectors.toMap(Path::getName, Function.identity(),
                                (p1, p2) -> {
                                    throw new BadRequestException(ErrorCodes.DuplicatedExists,
                                            new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "name",
                                                    p1.getName()},
                                            "duplicated path name ,path:" + p1);
                                }));
        for (Worksheet worksheet : nextLevelWorksheets) {
            Path willToCreatePath = willToCreatePathNameToPathMap.get(worksheet.getPath().getName());
            // if the id of the worksheet is empty, it means that the worksheet was extracted from its sub
            // worksheet and not actually created. Therefore, even if there is a directory with the same name in
            // the path that needs to be created, it can still be created and is not considered a duplicate name
            if (worksheet.getId() == null && (willToCreatePath == null || willToCreatePath.isDirectory())) {
                continue;
            }
            if (willToCreatePathNameToPathMap.containsKey(worksheet.getPath().getName())) {
                throw new BadRequestException(ErrorCodes.DuplicatedExists,
                        new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "name",
                                worksheet.getPath().getName()},
                        "duplicated path name ,path:" + worksheet.getPath().getName());
            }
        }
    }

    private void nameTooLongCheck(Set<Path> createPaths) {
        createPaths.forEach(createPath -> {
            if (createPath.isExceedNameLengthLimit()) {
                throw new BadRequestException(ErrorCodes.NameTooLong, new Object[] {NAME_LENGTH_LIMIT},
                        "name length is over limit " + NAME_LENGTH_LIMIT + ", path:" + createPath);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Worksheet that = (Worksheet) o;
        return Objects.equals(projectId, that.projectId) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, path);
    }

    @Override
    public String toString() {
        return "Worksheet{" +
                "projectId=" + projectId +
                ", id=" + id +
                ", path=" + path +
                ", version=" + version +
                ", objectId='" + objectId + '\'' +
                '}';
    }
}
