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
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.druid.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.worksheet.exceptions.ChangeTooMuchException;
import com.oceanbase.odc.service.worksheet.exceptions.EditVersionConflictException;
import com.oceanbase.odc.service.worksheet.exceptions.ExceedSameLevelNumLimitException;
import com.oceanbase.odc.service.worksheet.exceptions.NameDuplicatedException;
import com.oceanbase.odc.service.worksheet.exceptions.NameTooLongException;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;

import lombok.Data;
import lombok.Setter;

/**
 * worksheet
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
     * Same level worksheets in the same parent directory as the current worksheet, excluding the
     * current worksheet
     */
    @Setter
    private Set<Worksheet> sameLevelWorksheets;
    /**
     * all sub worksheets of current worksheet
     */
    @Setter
    private Set<Worksheet> subWorksheets;

    public static Worksheet of(Long projectId, Path path, String objectId, Long creatorId) {
        return new Worksheet(null, null, null, projectId, path, creatorId,
                null, objectId, null, null);
    }

    public Worksheet(Long id, Date createTime, Date updateTime, Long projectId, Path path, Long creatorId, Long version,
            String objectId, Set<Worksheet> sameLevelWorksheets, Set<Worksheet> subWorksheets) {
        this.id = id;
        this.createTime = createTime;
        this.updateTime = updateTime;
        PreConditions.notNull(projectId, "projectId");
        this.projectId = projectId;
        PreConditions.notNull(path, "path");
        this.path = path;
        this.creatorId = creatorId;
        this.version = version == null ? 0L : version;
        if (path.isFile()) {
            PreConditions.notBlank(objectId, "objectId");
        }
        this.objectId = objectId;
        this.sameLevelWorksheets = sameLevelWorksheets == null ? new HashSet<>() : sameLevelWorksheets;
        this.subWorksheets = subWorksheets == null ? new HashSet<>() : subWorksheets;

    }

    public List<Worksheet> getSubWorksheetsInDepth(Integer depth) {
        PreConditions.notNull(depth, "depth");
        PreConditions.assertTrue(depth >= 0, "depth");
        if (CollectionUtils.isEmpty(subWorksheets)) {
            return new ArrayList<>();
        }
        return subWorksheets.stream()
                .filter(file -> depth == 0 || file.getPath().getLevelNum() <= this.path.levelNum + depth)
                .sorted((o1, o2) -> Path.getPathSameLevelComparator().compare(o1.getPath(), o2.getPath()))
                .collect(Collectors.toList());
    }

    /**
     * rename path of current worksheet to {@param destinationPath}
     *
     * @param destinationPath
     * @return all changed worksheets ,contain current
     */
    public Set<Worksheet> rename(Path destinationPath) {
        if (!WorksheetPathUtil.isRenameValid(this.path, destinationPath)) {
            throw new IllegalArgumentException(
                    "invalid path for rename,from:" + this.path + ",destinationPath:" + destinationPath);
        }
        if (this.isRenameDuplicated(destinationPath)) {
            throw new NameDuplicatedException(
                    "duplicated path for rename,from:" + this.path + ",destinationPath:" + destinationPath);
        }
        if (this.path.rename(this.path, destinationPath)) {
            this.isChanged = true;
        }
        Set<Worksheet> changedSubFiles = new HashSet<>();
        changedSubFiles.add(this);
        if (CollectionUtils.isEmpty(subWorksheets)) {
            return changedSubFiles;
        }
        for (Worksheet subFile : subWorksheets) {
            if (subFile.path.rename(this.path, destinationPath)) {
                changedSubFiles.add(subFile);
                subFile.isChanged = true;
            }
            if (changedSubFiles.size() > CHANGE_FILE_NUM_LIMIT) {
                throw new ChangeTooMuchException("change num is over limit " + CHANGE_FILE_NUM_LIMIT);
            }
        }
        return changedSubFiles;
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
        // 若objectId变更，需要修改内容，且判断readVersion是否符合条件
        if (this.path.isFile() && !StringUtils.equals(this.objectId, objectId)) {
            this.objectId = objectId;
            this.readVersion = readVersion;
            this.isChanged = true;
            if (isVersionConflict()) {
                throw new EditVersionConflictException("version conflict,current version:" + this.version
                        + ",read version:" + this.readVersion + ",path:" + this.path);
            }
        }

        Set<Worksheet> changedSubFiles = new HashSet<>();
        // destination和当前path相同，需要修改名称
        if (!this.path.equals(destinationPath)) {
            changedSubFiles = rename(destinationPath);
        }

        if (this.isChanged) {
            changedSubFiles.add(this);
        }

        return changedSubFiles;
    }

    /**
     * Verify if the renamed destinationPath duplicates the existing worksheets path
     *
     * @param destinationPath
     * @return true 重复 ;false 不重复
     */
    public boolean isRenameDuplicated(Path destinationPath) {
        if (CollectionUtils.isEmpty(sameLevelWorksheets)) {
            return false;
        }
        for (Worksheet subFile : sameLevelWorksheets) {
            if (StringUtils.equals(subFile.path.getName(), destinationPath.getName())) {
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

    public Worksheet create(Path addPath, String objectId) {
        return batchCreate(Collections.singletonMap(addPath, objectId))
                .stream().findFirst()
                // can definitely obtain a created Worksheet here.
                // Adding exception checking is only for the integrity of the program
                .orElseThrow(() -> new IllegalStateException("unexpected exception"));
    }

    public Set<Worksheet> batchCreate(Map<Path, String> createPathToObjectIdMap) {
        List<Worksheet> nextLevelWorksheets = this.getSubWorksheetsInDepth(1);
        nameTooLongCheck(createPathToObjectIdMap.keySet());
        sameLevelFileNumLimitCheck(createPathToObjectIdMap, nextLevelWorksheets);
        duplicatedNameCheck(createPathToObjectIdMap, nextLevelWorksheets);
        return createPathToObjectIdMap.entrySet().stream().map(
                entry -> Worksheet.of(projectId, entry.getKey(), entry.getValue(), creatorId))
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
                createPathToObjectIdMap.size() > LEVEL_FILE_NUM_LIMIT) {
            throw new ExceedSameLevelNumLimitException(
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
        Set<String> willToCreatePathNameSet =
                createPathToObjectIdMap.keySet().stream().map(Path::getName).collect(Collectors.toSet());
        for (Worksheet subFile : nextLevelWorksheets) {
            if (willToCreatePathNameSet.contains(subFile.getPath().getName())) {
                throw new NameDuplicatedException(
                        "create path duplicated with with an existing same level file,"
                                + "exist path: " + subFile.getPath());
            }
        }
    }

    private void nameTooLongCheck(Set<Path> createPaths) {
        createPaths.forEach(createPath -> {
            if (createPath.isExceedNameLengthLimit()) {
                throw new NameTooLongException("name length is over limit " +
                        NAME_LENGTH_LIMIT + ", path:" + createPath);
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
        return "ProjectFile{" +
                "projectId=" + projectId +
                ", id=" + id +
                ", path=" + path +
                ", version=" + version +
                ", objectId='" + objectId + '\'' +
                '}';
    }
}
