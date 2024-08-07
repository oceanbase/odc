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

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.NAME_LENGTH_LIMIT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.service.worksheet.exceptions.NameTooLongException;
import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;

import lombok.Getter;

/**
 * the path of worksheet
 *
 * @author keyang
 * @date 2024/08/01
 * @since 4.3.2
 */
@Getter
public class Path {

    List<String> parentPathItems;
    /**
     * Path level, starting from 1, with a folder count by pathItems.size()-1 and a file count by
     * pathItems.size()
     */
    Integer levelNum;
    String name;
    WorksheetType type;
    WorksheetLocation location;

    public Path(String path) {
        List<String> items = WorksheetPathUtil.splitPathToItems(path);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        Optional<WorksheetLocation> locationOptional = WorksheetPathUtil.getPathLocation(items);
        if (!locationOptional.isPresent()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        Optional<WorksheetType> pathTypeOptional = WorksheetPathUtil.getPathType(items);
        if (!pathTypeOptional.isPresent()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        Optional<String> standardPathOptional = WorksheetPathUtil.convertItemsToPath(items);
        if (!standardPathOptional.isPresent()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        Optional<String> pathNameOptional = WorksheetPathUtil.getPathName(items);
        if (!pathNameOptional.isPresent()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        this.name = pathNameOptional.get();
        this.type = pathTypeOptional.get();
        this.location = locationOptional.get();
        this.levelNum = this.isFile() ? items.size() : items.size() - 1;
        this.parentPathItems = items.subList(0, levelNum - 1);
    }

    public String getStandardPath() {
        List<String> pathItems = new ArrayList<>(this.parentPathItems);
        pathItems.add(name);
        if (this.isDirectory()) {
            pathItems = WorksheetPathUtil.addSeparatorToItemsEnd(pathItems);
        }
        return WorksheetPathUtil.convertItemsToPath(pathItems)
                // Under normal circumstances, this exception is not returned.
                // Adding exception handling here is only to ensure program integrity
                // (it is necessary to handle situations where Optional is empty)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Path Object : " + this));
    }

    /**
     * Retrieve the parent path, if current is /Worksheets/,/Repos/git/, etc., return empty
     *
     * @return
     */
    public Optional<Path> getParentPath() {
        if (isRoot()) {
            return Optional.empty();
        }
        Optional<String> parentStandardPathOptional = WorksheetPathUtil.convertItemsToPath(
                WorksheetPathUtil.addSeparatorToItemsEnd(this.parentPathItems));
        return parentStandardPathOptional.map(Path::new);

    }

    /**
     * get the prefix path at index
     * 
     * @param index
     * @return
     */
    public Optional<Path> getPathAt(int index) {
        if (index < 0 || index > this.levelNum - 1) {
            throw new IndexOutOfBoundsException(
                    "index is out of bounds,index: " + index + ",levelNum: " + this.levelNum);
        }
        if (index == this.levelNum - 1) {
            return Optional.of(this);
        }
        if (isRoot()) {
            return Optional.empty();
        }
        boolean canGetPath = true;
        switch (location) {
            case WORKSHEETS:
                canGetPath = index >= 0;
                break;
            case REPOS:
                canGetPath = index >= 1;
                break;
        }
        if (!canGetPath) {
            return Optional.empty();
        }

        Optional<String> parentStandardPathOptional = WorksheetPathUtil.convertItemsToPath(
                WorksheetPathUtil.addSeparatorToItemsEnd(this.parentPathItems.subList(0, index + 1)));
        return parentStandardPathOptional.map(Path::new);
    }

    /**
     * strip the prefix path
     * 
     * @param prefixPath
     * @return
     */
    public Optional<String> stripPrefix(Optional<Path> prefixPath) {
        if (!prefixPath.isPresent()) {
            return Optional.of(this.getStandardPath());
        }
        int prefixPathLevelNum = prefixPath.get().getLevelNum();
        if (this.levelNum <= prefixPathLevelNum) {
            return Optional.empty();
        }
        List<String> items = new ArrayList<>();
        for (int i = prefixPathLevelNum; i < this.parentPathItems.size(); i++) {
            items.add(this.parentPathItems.get(i));
        }
        items.add(this.name);
        if (this.isDirectory()) {
            items = WorksheetPathUtil.addSeparatorToItemsEnd(items);
        }
        return WorksheetPathUtil.convertItemsToPath(items);
    }

    public boolean isRoot() {
        switch (location) {
            case WORKSHEETS:
                return levelNum <= 1;
            case REPOS:
                return levelNum <= 2;
        }
        return false;
    }

    /**
     * Rename. Only when the current path matches {@param from} will it be renamed as
     * {@param destination} and return true. If it does not match, no renaming will be performed and
     * return false The {@param from} and {@param destination} here need to satisfy
     * {@link WorksheetPathUtil#isRenameValid}, and the reason why this validation is not added here is
     * that it will not duplicate verify when renaming multiple times.
     * 
     * @param from the path need to rename
     * @param destination path after renamed
     * @return is renamed
     */
    public boolean rename(Path from, Path destination) {
        if (destination.isExceedNameLengthLimit()) {
            throw new NameTooLongException("name length is over limit " +
                    NAME_LENGTH_LIMIT + ", path:" + destination);
        }
        if (!this.isRenameMatch(from)) {
            return false;
        }
        // This is renaming the {@param from} itself
        if (from.levelNum.equals(this.levelNum)) {
            this.name = destination.name;
            return true;
        }
        // This is renaming the sub items of {@param from}
        this.parentPathItems.set(from.levelNum - 1, destination.name);
        return true;
    }

    /**
     * Matching rule: The current {@link Path} is either the same as {@param from} or a subset of
     * {@param from}
     * 
     * @param from
     * @return
     */
    public boolean isRenameMatch(Path from) {
        // current {@link Path} is same as {@param from}
        if (this.equals(from)) {
            return true;
        }
        // current {@link Path} is subset of {@param from}.
        // At this point, {@param from} needs to be of a non file type (with subsets)
        return from.isDirectory() && this.levelNum > from.levelNum
                && CollectionUtils.isEqualCollection(this.parentPathItems.subList(0, from.levelNum - 1),
                        from.parentPathItems.subList(0, from.levelNum - 1))
                && this.parentPathItems.get(from.levelNum - 1).equals(from.name);
    }

    /**
     * is {@link Path#name} contains {@param name}
     *
     * @param name
     * @return
     */
    public boolean isNameContains(String name) {
        return this.name.contains(name);
    }

    /**
     * Determine if the current path can be renamed
     * 
     * @return
     */
    public boolean canRename() {
        // Only subsets of /Worksheets/ and /Repos/RepoName/ can be renamed
        return this.location == WorksheetLocation.WORKSHEETS && this.levelNum > 1
                || this.location == WorksheetLocation.REPOS && this.levelNum > 2;
    }

    /**
     * Path sorted comparator, sorted by type+name
     *
     * @return
     */
    public static Comparator<Path> getPathSameLevelComparator() {
        return (o1, o2) -> {
            if (o1.getType() == o2.getType()) {
                return o1.getName().compareTo(o2.getName());
            }
            return -Integer.compare(o1.getType().getOrder(), o2.getType().getOrder());
        };
    }

    /**
     * sorted by levelNum
     * 
     * @return
     */
    public static Comparator<Path> getLevelNulComparator() {
        return Comparator.comparingInt(o -> o.levelNum);
    }

    public boolean isFile() {
        return this.type == WorksheetType.FILE;
    }

    public boolean isDirectory() {
        return this.type != WorksheetType.FILE;
    }

    public boolean isExceedNameLengthLimit() {
        return this.getName().length() > NAME_LENGTH_LIMIT;
    }

    public void canGetDownloadUrlCheck() {
        if (isDirectory()) {
            throw new IllegalArgumentException("can not get download url for directory,path" + this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Path path = (Path) o;
        return CollectionUtils.isEqualCollection(parentPathItems, path.parentPathItems)
                && Objects.equals(name, path.name)
                && type == path.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentPathItems, name, type);
    }

    @Override
    public String toString() {
        return getStandardPath();
    }
}
