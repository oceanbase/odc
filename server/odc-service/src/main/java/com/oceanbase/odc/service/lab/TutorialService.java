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
package com.oceanbase.odc.service.lab;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.lab.TutorialEntity;
import com.oceanbase.odc.metadb.lab.TutorialRepository;
import com.oceanbase.odc.service.lab.model.Tutorial;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2022/7/18
 */

@Slf4j
@Service
@SkipAuthorize
public class TutorialService {
    @Autowired
    TutorialRepository tutorialRepository;

    private static final Pattern HTML_A_TAG_PATTERN = Pattern.compile("<a.*</a>|\\[立即体验\\]\\(.+?\\)");

    /**
     * get tutorial list
     */
    public List<Tutorial> list() {
        List<TutorialEntity> tutorialEntities = tutorialRepository.findAll();
        return tutorialEntities.stream()
                .map(tutorialEntity -> new Tutorial(
                        tutorialEntity.getId(),
                        tutorialEntity.getName(),
                        tutorialEntity.getOverview(),
                        tutorialEntity.getFilename(),
                        tutorialEntity.getLanguage()))
                .collect(Collectors.toList());
    }

    /**
     * get tutorial by ID
     *
     * @param id tutorial ID
     */
    public Tutorial findById(@NotNull Long id) {
        TutorialEntity tutorialEntity = tutorialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TUTORIAL, "ID", id));
        Tutorial tutorial = new Tutorial(tutorialEntity);
        tutorial.setContent(maskLinkedButton(tutorial.getContent()));
        return tutorial;
    }

    /**
     * get tutorial by filename
     *
     * @param filename tutorial filename
     */
    public Tutorial findByFilenameAndLanguage(@NotNull String filename, @NotNull String language) {
        TutorialEntity tutorialEntity = tutorialRepository.findByFilenameAndLanguage(filename, language)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TUTORIAL, "filename", filename));
        Tutorial tutorial = new Tutorial(tutorialEntity);
        tutorial.setContent(maskLinkedButton(tutorial.getContent()));
        return tutorial;
    }


    /**
     * mask linked button
     */
    private String maskLinkedButton(String content) {
        return HTML_A_TAG_PATTERN.matcher(content).replaceAll("");
    }
}
